package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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
import com.kaltura.playkit.drm.WidevineModularAdapter;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.TODO;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class ExoAssetInfo extends OfflineManager.AssetInfo {

    final DownloadHelper downloadHelper;
    private final String assetId;
    private final OfflineManager.AssetDownloadState state;
    private final long estimatedSize;
    private final long downloadedSize;

    ExoAssetInfo(String assetId, OfflineManager.AssetDownloadState state, long estimatedSize, long downloadedSize, DownloadHelper downloadHelper) {
        this.assetId = assetId;
        this.state = state;
        this.estimatedSize = estimatedSize;
        this.downloadedSize = downloadedSize;
        this.downloadHelper = downloadHelper;
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
    public OfflineManager.AssetDownloadState getState() {
        return state;
    }

    @Override
    public long getEstimatedSize() {
        return estimatedSize;
    }

    @Override
    public long getDownloadedSize() {
        return downloadedSize;
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
    }

    private AssetStateListener getListener() {
        return assetStateListener != null ? assetStateListener : noopListener;
    }

    private void maybeRegisterDrmAsset(String assetId, int delayMillis) {
        bgHandler.postDelayed(() -> {
            log.d("Will now register " + delayMillis);
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

        SourceSelector selector = new SourceSelector(mediaEntry, null);

        final String assetId = mediaEntry.getId();
        final PKMediaSource source = selector.getSelectedSource();
        final PKDrmParams drmData = selector.getSelectedDrmParams();
        final PKMediaFormat mediaFormat = source.getMediaFormat();
        final String url = source.getUrl();

        if (mediaFormat != PKMediaFormat.dash) {
            throw new TODO();
        }

        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = buildDrmSessionManager(drmData);
        final DownloadHelper downloadHelper = DownloadHelper.forDash(Uri.parse(url), httpDataSourceFactory,
                new DefaultRenderersFactory(appContext), drmSessionManager, buildExoParameters(prefs));

        downloadHelper.prepare(new DownloadHelper.Callback() {
            @Override
            public void onPrepared(DownloadHelper helper) {

                final Object manifest = helper.getManifest();

                final ExoAssetInfo assetInfo = new ExoAssetInfo(assetId, AssetDownloadState.none, -1, -1, helper);
                pendingDrmRegistration.put(assetId, new Pair<>(source, drmData));
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
                    instance = new ExoOfflineManager(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void pauseDownloads() {
        throw new TODO();
    }

    @Override
    public void resumeDownloads() {
        throw new TODO();
    }

    @Override
    public AssetInfo getAssetInfo(String assetId) {
        throw new TODO();
    }

    @Override
    public List<AssetInfo> getAssetsInState(AssetDownloadState state) {
        throw new TODO();
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
    public boolean addAsset(AssetInfo assetInfo) {

        if (assetInfo instanceof ExoAssetInfo) {
            return addExoAsset(((ExoAssetInfo) assetInfo));
        }
        return false;
    }

    private boolean addExoAsset(ExoAssetInfo assetInfo) {
        final DownloadHelper helper = assetInfo.downloadHelper;
        final DownloadRequest downloadRequest = helper.getDownloadRequest(assetInfo.getAssetId(), null);

        DownloadService.sendAddDownload(appContext, ExoDownloadService.class, downloadRequest, false);

        return true;
    }

    @Override
    public boolean startAssetDownload(String assetId) {
        throw new TODO();
    }

    @Override
    public boolean pauseAssetDownload(String assetId) {
        throw new TODO();
    }

    @Override
    public boolean removeAsset(String assetId) {
        throw new TODO();
    }

    @Override
    public DrmInfo getDrmStatus(String assetId) {
        throw new TODO();
    }



    private void registerDrmAsset(String assetId) {

        if (bgHandler.getLooper() != Looper.myLooper()) {
            throw new IllegalStateException();
        }

        // TODO: 2019-08-04 Force cached

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
            localAssetsManager.registerDrmAsset(drmInitData, PKMediaFormat.dash.mimeType, assetId, PKMediaFormat.dash, licenseUri);
            listener.onRegistered(assetId, null); // TODO: 2019-08-01 drm info

            pendingDrmRegistration.remove(assetId);

        } catch (IOException | InterruptedException e) {
            listener.onRegisterError(assetId, e);

        } catch (WidevineModularAdapter.RegisterException e) {
            listener.onRegisterError(assetId, e);
        }
    }

    @Override
    public boolean renewDrmAsset(String assetId, PKDrmParams drmParams, DrmListener listener) {
        throw new TODO();
    }

    private byte[] getDrmInitData(CacheDataSourceFactory dataSourceFactory, String contentUri) throws IOException, InterruptedException {
        final CacheDataSource cacheDataSource = dataSourceFactory.createDataSource();
        final DashManifest dashManifest = DashUtil.loadManifest(cacheDataSource, Uri.parse(contentUri));
        final DrmInitData drmInitData = DashUtil.loadDrmInitData(cacheDataSource, dashManifest.getPeriod(0));

        final DrmInitData.SchemeData schemeData = drmInitData.get(C.WIDEVINE_UUID);
        return schemeData != null ? schemeData.data : null;
    }
}
