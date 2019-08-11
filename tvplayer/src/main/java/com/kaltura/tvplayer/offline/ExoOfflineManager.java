package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.database.ExoDatabaseProvider;
import com.kaltura.android.exoplayer2.drm.*;
import com.kaltura.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.offline.*;
import com.kaltura.android.exoplayer2.scheduler.Requirements;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.android.exoplayer2.source.dash.DashUtil;
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.FileDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.cache.*;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.OfflineManager.AssetDownloadState;
import com.kaltura.tvplayer.TODO;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.*;


class ExoAssetInfo extends OfflineManager.AssetInfo {

    private final String assetId;
    private final AssetDownloadState state;
    private final float percentDownloaded;
    private final long estimatedSize;
    private final long bytesDownloaded;

    @Nullable final DownloadHelper downloadHelper;  // Only used during preparation

    ExoAssetInfo(String assetId, AssetDownloadState state, long estimatedSize, long bytesDownloaded, @SuppressWarnings("NullableProblems") DownloadHelper downloadHelper) {
        this.assetId = assetId;
        this.state = state;
        this.estimatedSize = estimatedSize;
        this.bytesDownloaded = bytesDownloaded;
        this.downloadHelper = downloadHelper;
        percentDownloaded = 0;
    }

    ExoAssetInfo(Download download) {
        assetId = download.request.id;
        downloadHelper = null;
        percentDownloaded = download.getPercentDownloaded();
        bytesDownloaded = download.getBytesDownloaded();
        if (download.contentLength > 0) {
            estimatedSize = download.contentLength;
        } else {
            estimatedSize = (long) (100 * bytesDownloaded / percentDownloaded);
        }
        state = toAssetState(download.state);
    }

    static int toExoState(AssetDownloadState state) {
        switch (state) {
            case none:
                break;
            case downloading:
                return Download.STATE_DOWNLOADING;
            case queued:
                return Download.STATE_QUEUED;
            case completed:
                return Download.STATE_COMPLETED;
            case failed:
                return Download.STATE_FAILED;
            case removing:
                return Download.STATE_REMOVING;
            case stopped:
                return Download.STATE_STOPPED;
        }
        return -1;
    }

    static AssetDownloadState toAssetState(@Download.State int exoState) {
        switch (exoState) {
            case Download.STATE_COMPLETED:
                return AssetDownloadState.completed;
            case Download.STATE_DOWNLOADING:
                return AssetDownloadState.downloading;
            case Download.STATE_FAILED:
                return AssetDownloadState.failed;
            case Download.STATE_QUEUED:
                return AssetDownloadState.queued;
            case Download.STATE_REMOVING:
                return AssetDownloadState.removing;
            case Download.STATE_RESTARTING:
                return AssetDownloadState.downloading;  // TODO: is this the same?
            case Download.STATE_STOPPED:
                return AssetDownloadState.stopped;
        }
        return null;
    }

    @Override
    public void release() {
        if (downloadHelper != null) {
            downloadHelper.release();
        }
    }

    @Override
    public String getAssetId() {
        return assetId;
    }

    @Override
    public AssetDownloadState getState() {
        return state;
    }

    @Override
    public long getEstimatedSize() {
        return estimatedSize;
    }

    @Override
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }
}

enum StopReason {
    none,       // 0 = Download.STOP_REASON_NONE
    unknown,    // 10
    pause;      // 11

    static StopReason fromExoReason(int reason) {
        switch (reason) {
            case Download.STOP_REASON_NONE:
                return unknown;
            case 11:
                return pause;
            default:
                return unknown;
        }
    }

    int toExoCode() {
        switch (this) {
            case none: return Download.STOP_REASON_NONE;
            case pause: return 11;
            default: return 10;
        }
    }
}

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
                    throw new TODO();
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
    private PKMediaFormat preferredMediaFormat;

    @NonNull
    private Handler createBgHandler() {
        final HandlerThread bgHandlerThread = new HandlerThread("bgHandlerThread");
        bgHandlerThread.start();
        return new Handler(bgHandlerThread.getLooper());
    }

    private final Map<String, Pair<PKMediaSource, PKDrmParams>> pendingDrmRegistration = new HashMap<>();

    private static final AssetStateListener noopListener = new AssetStateListener() {
        @Override public void onStateChanged(String assetId, AssetInfo assetInfo) {}
        @Override public void onAssetRemoved(String assetId) {}
        @Override public void onAssetDownloadFailed(String assetId, AssetDownloadException error) {}
        @Override public void onAssetDownloadComplete(String assetId) {}
        @Override public void onAssetDownloadPending(String assetId) {}
        @Override public void onRegistered(String assetId, DrmInfo drmInfo) {}
        @Override public void onRegisterError(String assetId, Exception error) {}
        @Override public void onAssetDownloadPaused(String assetId) {}
    };

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
    }

    private AssetStateListener getListener() {
        return assetStateListener != null ? assetStateListener : noopListener;
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
                prepareCallback.onPrepareError(new IllegalArgumentException("Unsupported media format"));
                return;
        }

        downloadHelper.prepare(new DownloadHelper.Callback() {
            @Override
            public void onPrepared(DownloadHelper helper) {

                final ExoAssetInfo assetInfo = new ExoAssetInfo(assetId, AssetDownloadState.none, -1, -1, helper);
                if (mediaFormat == PKMediaFormat.dash && drmData != null) {
                    pendingDrmRegistration.put(assetId, new Pair<>(source, drmData));
                }
                prepareCallback.onPrepared(assetInfo, null);
            }

            @Override
            public void onPrepareError(DownloadHelper helper, IOException e) {
                if (e != null) {
                    log.e("onPrepareError", e);
                }
                helper.release();
            }
        });
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

        if (!(assetInfo instanceof ExoAssetInfo)) {
            throw new IllegalArgumentException("Not an ExoAssetInfo object");
        }

        final DownloadHelper helper = ((ExoAssetInfo) assetInfo).downloadHelper;
        if (helper == null) {
            throw new IllegalArgumentException("Asset is not in prepare state");
        }
        final DownloadRequest downloadRequest = helper.getDownloadRequest(assetInfo.getAssetId(), null);

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

        } catch (IOException | InterruptedException e) {
            log.e("removeAsset failed ", e);
            return false;
        }

        return true;
    }

    @Override
    public DrmInfo getDrmStatus(String assetId) {
        try {
            final byte[] drmInitData = getDrmInitData(assetId);
            if (drmInitData == null) {
                return DrmInfo.unknown;
            }
            final LocalAssetsManager.AssetStatus assetStatus = localAssetsManager.getDrmStatus(assetId, drmInitData);

            if (assetStatus == null || !assetStatus.registered) {
                return DrmInfo.unknown;
            }

            if (!assetStatus.hasContentProtection) {
                return DrmInfo.clear;
            }

            return DrmInfo.withDrm(assetStatus.licenseDuration, assetStatus.totalDuration);

        } catch (IOException | InterruptedException e) {
            log.e("getDrmStatus failed ", e);
            return DrmInfo.unknown;
        }
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
            listener.onRegistered(assetId, null); // TODO: 2019-08-01 drm info

            pendingDrmRegistration.remove(assetId);

        } catch (IOException | InterruptedException e) {
            listener.onRegisterError(assetId, e);

        } catch (LocalAssetsManager.RegisterException e) {
            listener.onRegisterError(assetId, e);
        }
    }

    @Override
    public void renewDrmAsset(String assetId, PKDrmParams drmParams, DrmListener listener) {
        throw new TODO();
    }

    private byte[] getDrmInitData(CacheDataSourceFactory dataSourceFactory, String contentUri) throws IOException, InterruptedException {
        final CacheDataSource cacheDataSource = dataSourceFactory.createDataSource();
        final DashManifest dashManifest = DashUtil.loadManifest(cacheDataSource, Uri.parse(contentUri));
        final DrmInitData drmInitData = DashUtil.loadDrmInitData(cacheDataSource, dashManifest.getPeriod(0));

        final DrmInitData.SchemeData schemeData = drmInitData.get(C.WIDEVINE_UUID);
        return schemeData != null ? schemeData.data : null;
    }

    @Override
    public void setPreferredMediaFormat(PKMediaFormat preferredMediaFormat) {
        this.preferredMediaFormat = preferredMediaFormat;
    }
}
