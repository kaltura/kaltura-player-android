package com.kaltura.tvplayer.offline.exo;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.database.ExoDatabaseProvider;
import com.kaltura.android.exoplayer2.drm.*;
import com.kaltura.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.offline.*;
import com.kaltura.android.exoplayer2.scheduler.Requirements;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.android.exoplayer2.source.dash.DashUtil;
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest;
import com.kaltura.android.exoplayer2.source.hls.HlsManifest;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.TrackSelection;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.FileDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.cache.*;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.tvplayer.TODO;
import com.kaltura.tvplayer.offline.AbstractOfflineManager;
import okhttp3.OkHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;


public class ExoOfflineManager extends AbstractOfflineManager {

    private static final PKLog log = PKLog.get("ExoOfflineManager");
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";

    private static ExoOfflineManager instance;

    private final String userAgent = Util.getUserAgent(appContext, "ExoDownload");

    private final OkHttpDataSourceFactory httpDataSourceFactory = new OkHttpDataSourceFactory(new OkHttpClient(), userAgent);

    private Handler bgHandler = createBgHandler();

    private final DatabaseProvider databaseProvider;
    private final File downloadDirectory;
    final Cache downloadCache;
    final DownloadManager downloadManager;

    private final LocalAssetsManagerExo localAssetsManager;

    @SuppressWarnings("FieldCanBeLocal")
    private final DownloadManager.Listener exoListener = new DownloadManager.Listener() {
        @Override
        public void onInitialized(DownloadManager downloadManager) {

        }

        @Override
        public void onDownloadChanged(DownloadManager downloadManager, Download download) {
            final String assetId = download.request.id;
            final AssetStateListener listener = getListener();

            switch (download.state) {
                case Download.STATE_COMPLETED:
                    log.d("STATE_COMPLETED: " + assetId);
                    maybeRegisterDrmAsset(assetId, 0);
                    listener.onAssetDownloadComplete(assetId);
                    break;
                case Download.STATE_DOWNLOADING:
                    log.d("STATE_DOWNLOADING: " + assetId);
                    maybeRegisterDrmAsset(assetId, 10000);
                    break;
                case Download.STATE_FAILED:
                    log.d("STATE_FAILED: " + assetId);
                    listener.onAssetDownloadFailed(assetId, new AssetDownloadException("Failed for unknown reason"));
                    break;
                case Download.STATE_QUEUED:
                    log.d("STATE_QUEUED: " + assetId);
                    listener.onAssetDownloadPending(assetId);
                    break;
                case Download.STATE_REMOVING:
                    log.d("STATE_REMOVING: " + assetId);
                    listener.onAssetRemoved(assetId);
                    break;
                case Download.STATE_RESTARTING:
                    log.d("STATE_RESTARTING: " + assetId);
                    throw new TODO();
                case Download.STATE_STOPPED:
                    log.d("STATE_STOPPED: " + assetId);
                    if (StopReason.fromExoReason(download.stopReason) == StopReason.pause) {
                        listener.onAssetDownloadPaused(assetId);
                    }
            }
        }

        @Override
        public void onDownloadRemoved(DownloadManager downloadManager, Download download) {
            getListener().onAssetRemoved(download.request.id);
        }

        @Override
        public void onIdle(DownloadManager downloadManager) {

        }

        @Override
        public void onRequirementsStateChanged(DownloadManager downloadManager, Requirements requirements, int notMetRequirements) {

        }
    };

    @NonNull
    private Handler createBgHandler() {
        final HandlerThread bgHandlerThread = new HandlerThread("bgHandlerThread");
        bgHandlerThread.start();
        return new Handler(bgHandlerThread.getLooper());
    }

    private final Map<String, Pair<PKMediaSource, PKDrmParams>> pendingDrmRegistration = new HashMap<>();

    private ExoOfflineManager(Context context) {
        super(context);
        final File externalFilesDir = context.getExternalFilesDir(null);
        downloadDirectory = externalFilesDir != null ? externalFilesDir : context.getFilesDir();

        databaseProvider = new ExoDatabaseProvider(context);

        File downloadContentDirectory = new File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY);
        downloadCache = new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), databaseProvider);

        DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(databaseProvider);

        DownloaderConstructorHelper downloaderConstructorHelper = new DownloaderConstructorHelper(downloadCache, httpDataSourceFactory);

        downloadManager = new DownloadManager(
                context,
                downloadIndex,
                new DefaultDownloaderFactory(downloaderConstructorHelper)
        );

        downloadManager.setMaxParallelDownloads(4);
        final Requirements requirements = new Requirements(Requirements.NETWORK);
        downloadManager.setRequirements(requirements);
        downloadManager.addListener(exoListener);
        localAssetsManager = new LocalAssetsManagerExo(context);


        postEvent(new Runnable() {
            @Override
            public void run() {

                final DownloadProgressListener listener = ExoOfflineManager.this.downloadProgressListener;

                if (listener != null) {
                    final List<Download> downloads = downloadManager.getCurrentDownloads();
                    for (Download download : downloads) {
                        if (download.state != Download.STATE_DOWNLOADING) continue;

                        final float percentDownloaded = download.getPercentDownloaded();
                        final long bytesDownloaded = download.getBytesDownloaded();
                        final long totalSize = percentDownloaded > 0 ? (long) (100f * bytesDownloaded / percentDownloaded) : -1;

                        final String assetId = download.request.id;

                        listener.onDownloadProgress(assetId, bytesDownloaded, totalSize, percentDownloaded);
                    }

                }

                postEventDelayed(this, 250);
            }
        });
    }

    private void maybeRegisterDrmAsset(String assetId, int delayMillis) {
        bgHandler.postDelayed(() -> {
            registerDrmAsset(assetId);
        }, delayMillis);
    }

    private CacheDataSourceFactory buildReadOnlyCacheDataSource(DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSourceFactory(
                cache,
                upstreamFactory,
                new FileDataSourceFactory(),
                null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null
        );
    }


    @Override
    public void prepareAsset(PKMediaEntry mediaEntry, SelectionPrefs prefs, PrepareCallback prepareCallback) {

        SourceSelector selector = new SourceSelector(mediaEntry, preferredMediaFormat);

        final String assetId = mediaEntry.getId();
        final PKMediaSource source = selector.getSelectedSource();
        final PKDrmParams drmData = selector.getSelectedDrmParams();
        final PKMediaFormat mediaFormat = source.getMediaFormat();
        final String url = source.getUrl();
        final Uri uri = Uri.parse(url);

        final DownloadHelper downloadHelper;

        postEvent(() -> prepareCallback.onSourceSelected(assetId, source, drmData));

        switch (mediaFormat) {
            // DASH: clear or with Widevine
            case dash:
                downloadHelper = DownloadHelper.forDash(uri, httpDataSourceFactory,
                        new DefaultRenderersFactory(appContext), buildDrmSessionManager(drmData), buildExoParameters(prefs));
                break;

            // HLS: clear/aes only
            case hls:
                downloadHelper = DownloadHelper.forHls(uri, httpDataSourceFactory,
                        new DefaultRenderersFactory(appContext), null, buildExoParameters(prefs));
                break;

            // Progressive
            case mp4:
            case mp3:
                downloadHelper = DownloadHelper.forProgressive(uri);
                break;

            default:
                postEvent(() -> prepareCallback.onPrepareError(assetId, new IllegalArgumentException("Unsupported media format")));
                return;
        }

        downloadHelper.prepare(new DownloadHelper.Callback() {
            @Override
            public void onPrepared(DownloadHelper helper) {

                long selectedSize = estimateTotalSize(helper, estimatedHlsAudioBitrate);

                final ExoAssetInfo assetInfo = new ExoAssetInfo(assetId, AssetDownloadState.none, selectedSize, -1, helper);
                if (mediaFormat == PKMediaFormat.dash && drmData != null) {
                    pendingDrmRegistration.put(assetId, new Pair<>(source, drmData));
                }

                saveAssetSourceId(assetId, source.getId());
                postEvent(() -> prepareCallback.onPrepared(assetId, assetInfo, null));
            }

            @Override
            public void onPrepareError(DownloadHelper helper, IOException error) {
                if (error != null) {
                    log.e("onPrepareError", error);
                }
                helper.release();
                postEvent(() -> prepareCallback.onPrepareError(assetId, error));
            }
        });
    }

    private static long estimateTotalSize(DownloadHelper helper, int hlsAudioBitrate) {
        long selectedSize = 0;

        final Object manifest = helper.getManifest();

        final long durationMs;
        if (manifest instanceof DashManifest) {
            final DashManifest dashManifest = (DashManifest) manifest;
            durationMs = dashManifest.durationMs;
        } else if (manifest instanceof HlsManifest) {
            final HlsManifest hlsManifest = (HlsManifest) manifest;
            durationMs = hlsManifest.mediaPlaylist.durationUs / 1000;
        } else {
            durationMs = 0; // TODO: 2019-08-15
        }

        for (int pi = 0; pi < helper.getPeriodCount(); pi++) {

            final MappingTrackSelector.MappedTrackInfo mappedTrackInfo = helper.getMappedTrackInfo(pi);
            final int rendererCount = mappedTrackInfo.getRendererCount();

            for (int i = 0; i < rendererCount; i++) {
                final List<TrackSelection> trackSelections = helper.getTrackSelections(pi, i);
                for (TrackSelection selection : trackSelections) {
                    final Format format = selection.getSelectedFormat();

                    int bitrate = format.bitrate;
                    if (bitrate <= 0) {
                        if (format.sampleMimeType != null && format.sampleMimeType.startsWith("audio/")) {
                            bitrate = hlsAudioBitrate;
                        }
                    }
                    selectedSize += (bitrate * durationMs) / 1000 / 8;
                }
            }
        }
        return selectedSize;
    }

    private void saveAssetSourceId(String assetId, String sourceId) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        sharedPrefs.edit().putString(sharedPrefsKey(assetId), sourceId).apply();
    }

    private void removeAssetSourceId(String assetId) {
        sharedPrefs().edit().remove(sharedPrefsKey(assetId)).apply();
    }

    @Nullable
    private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(PKDrmParams drmData) {
        if (drmData == null) {
            return null;
        }

        if (drmData.getScheme() != PKDrmParams.Scheme.WidevineCENC) {
            throw new IllegalArgumentException("Only WidevineCENC");
        }

        String licenseUrl = drmData.getLicenseUri();
        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
        try {
            final HttpMediaDrmCallback mediaDrmCallback = new HttpMediaDrmCallback(licenseUrl, httpDataSourceFactory);
            drmSessionManager = DefaultDrmSessionManager.newWidevineInstance(mediaDrmCallback, null);
        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
        }
        return drmSessionManager;
    }

    private DefaultTrackSelector.Parameters buildExoParameters(SelectionPrefs prefs) {
        return new DefaultTrackSelector.ParametersBuilder().setMaxVideoSizeSd().build();
//        return DefaultTrackSelector.Parameters.DEFAULT;// TODO: 2019-07-31
    }

    public static ExoOfflineManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ExoOfflineManager.class) {
                if (instance == null) {
                    instance = new ExoOfflineManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @Override
    public void start(ManagerStartCallback callback) throws IOException {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pauseDownloads() {
        // Pause all downloads.
        DownloadService.sendPauseDownloads(
                appContext,
                ExoDownloadService.class,
                /* foreground= */ false);

    }

    @Override
    public void resumeDownloads() {
        // Resume all downloads.
        DownloadService.sendResumeDownloads(
                appContext,
                ExoDownloadService.class,
                /* foreground= */ false);
    }

    @Override
    public AssetInfo getAssetInfo(String assetId) {
        final Download download;
        try {
            download = downloadManager.getDownloadIndex().getDownload(assetId);
            if (download != null) {
                return new ExoAssetInfo(download);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<AssetInfo> getAssetsInState(AssetDownloadState state) {

        final int exoState;
        exoState = ExoAssetInfo.toExoState(state);
        final DownloadCursor downloads;
        try {
            downloads = downloadManager.getDownloadIndex().getDownloads(exoState);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        List<AssetInfo> assetInfoList = new ArrayList<>(downloads.getCount());

        for (downloads.moveToFirst(); downloads.moveToNext();) {
            final Download download = downloads.getDownload();
            assetInfoList.add(new ExoAssetInfo(download));
        }

        return assetInfoList;
    }

    @Override
    public PKMediaEntry getLocalPlaybackEntry(String assetId) throws IOException {

        final Download download = downloadManager.getDownloadIndex().getDownload(assetId);

        if (download == null || download.state != Download.STATE_COMPLETED) {
            return null;
        }

        final CacheDataSourceFactory dataSourceFactory = new CacheDataSourceFactory(downloadCache, httpDataSourceFactory);
        final MediaSource mediaSource = DownloadHelper.createMediaSource(download.request, dataSourceFactory);

        final PKMediaSource localMediaSource = localAssetsManager.getLocalMediaSource(assetId, mediaSource);
        return new PKMediaEntry().setId(assetId).setSources(Collections.singletonList(localMediaSource));
    }

    @Override
    public void startAssetDownload(AssetInfo assetInfo) {

        if (assetInfo == null) {
            log.e("assetInfo == null");
            return;
        }

        if (!(assetInfo instanceof ExoAssetInfo)) {
            throw new IllegalArgumentException("Not an ExoAssetInfo object");
        }

        final ExoAssetInfo exoAssetInfo = (ExoAssetInfo) assetInfo;

        final DownloadHelper helper = exoAssetInfo.downloadHelper;
        if (helper == null) {
            try {
                final Download download = downloadManager.getDownloadIndex().getDownload(exoAssetInfo.getAssetId());
                DownloadService.sendSetStopReason(appContext, ExoDownloadService.class, exoAssetInfo.getAssetId(), 0, false);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Asset is not in prepare state");
            }
            return;
        }

        JSONObject data = new JSONObject();
        byte[] bytes = null;
        try {
            data.put("estimatedSizeBytes", exoAssetInfo.getEstimatedSize());
            bytes = data.toString().getBytes();
        } catch (JSONException e) {

        }

        final DownloadRequest downloadRequest = helper.getDownloadRequest(assetInfo.getAssetId(), bytes);

        DownloadService.sendAddDownload(appContext, ExoDownloadService.class, downloadRequest, false);
    }

    @Override
    public void resumeAssetDownload(String assetId) {

        // Clear the stop reason for a single download.
        DownloadService.sendSetStopReason(
                appContext,
                ExoDownloadService.class,
                assetId,
                Download.STOP_REASON_NONE,
                /* foreground= */ false);
    }

    @Override
    public void pauseAssetDownload(String assetId) {
        // Set the stop reason for a single download.
        DownloadService.sendSetStopReason(
                appContext,
                ExoDownloadService.class,
                assetId,
                StopReason.pause.toExoCode(),
                /* foreground= */ false);
    }

    @Override
    public boolean removeAsset(String assetId) {
        final byte[] drmInitData;
        try {
            drmInitData = getDrmInitData(assetId);
            DownloadService.sendRemoveDownload(appContext, ExoDownloadService.class, assetId, false);
            localAssetsManager.unregisterAsset(assetId, drmInitData);
            removeAssetSourceId(assetId);

        } catch (IOException | InterruptedException e) {
            log.e("removeAsset failed ", e);
            return false;
        }

        return true;
    }

    @Override
    public DrmStatus getDrmStatus(String assetId) {
        if (assetId == null) {
            return DrmStatus.unknown;
        }

        try {
            final byte[] drmInitData = getDrmInitData(assetId);
            return getDrmStatus(assetId, drmInitData);

        } catch (IOException | InterruptedException e) {
            log.e("getDrmStatus failed ", e);
            return DrmStatus.unknown;
        }
    }

    private DrmStatus getDrmStatus(String assetId, byte[] drmInitData) {
        if (drmInitData == null) {
            return DrmStatus.clear;
        }
        final LocalAssetsManager.AssetStatus assetStatus = localAssetsManager.getDrmStatus(assetId, drmInitData);

        if (assetStatus == null || !assetStatus.registered) {
            return DrmStatus.unknown;
        }

        if (!assetStatus.hasContentProtection) {
            return DrmStatus.clear;
        }

        return DrmStatus.withDrm(assetStatus.licenseDuration, assetStatus.totalDuration);
    }

    private byte[] getDrmInitData(String assetId) throws IOException, InterruptedException {
        final Download download = downloadManager.getDownloadIndex().getDownload(assetId);
        if (download == null) {
            return null;
        }

        final Uri uri = download.request.uri;
        final CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(downloadCache, httpDataSourceFactory);
        return getDrmInitData(cacheDataSourceFactory, uri.toString());
    }


    private void registerDrmAsset(String assetId) {

        if (assetId == null) {
            return;
        }

        final Pair<PKMediaSource, PKDrmParams> pair = pendingDrmRegistration.get(assetId);
        if (pair == null || pair.first == null || pair.second == null) {
            return; // no DRM or already processed
        }

        final String sourceUrl = pair.first.getUrl();
        final PKDrmParams drmData = pair.second;

        final String licenseUri = drmData.getLicenseUri();

        final CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(downloadCache, httpDataSourceFactory);

        final AssetStateListener listener = getListener();

        final byte[] drmInitData;
        try {
            drmInitData = getDrmInitData(cacheDataSourceFactory, sourceUrl);
            localAssetsManager.registerWidevineDashAsset(assetId, licenseUri, drmInitData);
            postEvent(() -> listener.onRegistered(assetId, getDrmStatus(assetId, drmInitData)));

            pendingDrmRegistration.remove(assetId);

        } catch (IOException | InterruptedException e) {
            postEvent(() -> listener.onRegisterError(assetId, e));

        } catch (LocalAssetsManager.RegisterException e) {
            postEvent(() -> listener.onRegisterError(assetId, e));
        }
    }

    class RenewParams {
        String licenseUri;
        byte[] drmInitData;
        String sourceId;

        public RenewParams(String licenseUri, byte[] drmInitData, String sourceId) {
            this.licenseUri = licenseUri;
            this.drmInitData = drmInitData;
            this.sourceId = sourceId;
        }
    }

    @Override
    public void renewDrmAsset(String assetId, PKDrmParams drmParams) {
        try {
            final byte[] drmInitData = getDrmInitData(assetId);
            localAssetsManager.registerWidevineDashAsset(assetId, drmParams.getLicenseUri(), drmInitData);
            postEvent(() -> getListener().onRegistered(assetId, getDrmStatus(assetId, drmInitData)));
        } catch (LocalAssetsManager.RegisterException | IOException | InterruptedException e) {
            postEvent(() -> getListener().onRegisterError(assetId, e));
        }
    }

    private byte[] getDrmInitData(CacheDataSourceFactory dataSourceFactory, String contentUri) throws IOException, InterruptedException {
        final CacheDataSource cacheDataSource = dataSourceFactory.createDataSource();
        final DashManifest dashManifest = DashUtil.loadManifest(cacheDataSource, Uri.parse(contentUri));
        final DrmInitData drmInitData = DashUtil.loadDrmInitData(cacheDataSource, dashManifest.getPeriod(0));

        final DrmInitData.SchemeData schemeData;
        if (drmInitData == null) {
            return null;
        }

        schemeData = findWidevineSchemaData(drmInitData);
        return schemeData != null ? schemeData.data : null;
    }

    private DrmInitData.SchemeData findWidevineSchemaData(DrmInitData drmInitData) {
        for (int i = 0; i < drmInitData.schemeDataCount; i++) {
            final DrmInitData.SchemeData schemeData = drmInitData.get(i);
            if (schemeData != null && schemeData.matches(C.WIDEVINE_UUID)) {
                return schemeData;
            }
        }
        return null;
    }

}
