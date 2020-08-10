package com.kaltura.tvplayer.offline.exo;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.ExoPlayerLibraryInfo;
import com.kaltura.android.exoplayer2.Format;
import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.database.ExoDatabaseProvider;
import com.kaltura.android.exoplayer2.drm.DrmInitData;
import com.kaltura.android.exoplayer2.drm.DrmSessionManager;
import com.kaltura.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.offline.DefaultDownloadIndex;
import com.kaltura.android.exoplayer2.offline.DefaultDownloaderFactory;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadCursor;
import com.kaltura.android.exoplayer2.offline.DownloadHelper;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloadRequest;
import com.kaltura.android.exoplayer2.offline.DownloadService;
import com.kaltura.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.kaltura.android.exoplayer2.scheduler.Requirements;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.android.exoplayer2.source.TrackGroup;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.source.dash.DashUtil;
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest;
import com.kaltura.android.exoplayer2.source.hls.HlsManifest;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.TrackSelection;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.kaltura.android.exoplayer2.upstream.cache.SimpleCache;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.player.PKHttpClientManager;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.playkit.utils.NativeCookieJarBridge;
import com.kaltura.tvplayer.offline.AbstractOfflineManager;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.drm.DrmCallback;
import com.kaltura.tvplayer.prefetch.PrefetchConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;


// NOTE: this and related classes are not currently in use. OfflineManager.getInstance() always
// returns an instance of DTGOfflineManager. ExoOfflineManager will be used in a future version.

public class ExoOfflineManager extends AbstractOfflineManager {

    private static final PKLog log = PKLog.get("ExoOfflineManager");
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private static final int  REGISTER_ASSET_AFTER_5_SEC = 5000;
    private static final int  REGISTER_ASSET_NOW = 0;
    private static Gson gson = new Gson();

    private static ExoOfflineManager instance;

    //private final String userAgent = Util.getUserAgent(appContext, "ExoDownload");
    private final String userAgent = Utils.getUserAgent(appContext) + " ExoDownloadPlayerLib/" + ExoPlayerLibraryInfo.VERSION;

    private final OkHttpDataSourceFactory httpDataSourceFactory = new OkHttpDataSourceFactory(PKHttpClientManager.newClientBuilder()
            .cookieJar(NativeCookieJarBridge.sharedCookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .readTimeout(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).build(), userAgent);

    private Handler bgHandler = createBgHandler();

    private final DatabaseProvider databaseProvider;
    private final File downloadDirectory;
    final Cache downloadCache;
    final DownloadManager downloadManager;

    //private DrmSessionManager drmSessionManager;
    private DeferredDrmSessionManager deferredDrmSessionManager;

    @SuppressWarnings("FieldCanBeLocal")
    private final DownloadManager.Listener exoListener = new DownloadManager.Listener() {
        @Override
        public void onInitialized(DownloadManager downloadManager) {

        }

        @Override
        public void onDownloadChanged(DownloadManager downloadManager, Download download) {
            final String assetId = download.request.id;
            final AssetStateListener listener = getListener();

            final String dataJson = Util.fromUtf8Bytes(download.request.data);
            PrefetchConfig prefetchConfig = extractPrefetchConfig(dataJson);
            DownloadType downloadType = DownloadType.FULL;
            if (prefetchConfig != null) {
                downloadType = DownloadType.PREFETCH;
            }

            switch (download.state) {
                case Download.STATE_COMPLETED:
                    log.d("STATE_COMPLETED: " + assetId);
                    maybeRegisterDrmAsset(assetId, downloadType, REGISTER_ASSET_NOW);
                    listener.onAssetDownloadComplete(assetId, downloadType);
                    break;
                case Download.STATE_DOWNLOADING:
                    log.d("STATE_DOWNLOADING: " + assetId);
                    maybeRegisterDrmAsset(assetId, downloadType, REGISTER_ASSET_AFTER_5_SEC);
                    break;
                case Download.STATE_FAILED:
                    log.d("STATE_FAILED: " + assetId);
                    listener.onAssetDownloadFailed(assetId, downloadType, new AssetDownloadException("Failed for unknown reason"));
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
                    // TODO: 2019-09-04 what does it mean?
                    break;
                case Download.STATE_STOPPED:
                    log.d("STATE_STOPPED: " + assetId);
                    if (StopReason.fromExoReason(download.stopReason) == StopReason.pause) {
                        listener.onAssetDownloadPaused(assetId);
                    } else if (StopReason.fromExoReason(download.stopReason) == StopReason.prefetchDone) {
                        maybeRegisterDrmAsset(assetId, downloadType, REGISTER_ASSET_NOW);
                        listener.onAssetDownloadComplete(assetId, downloadType);
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


        postEvent(new Runnable() {
            @Override
            public void run() {

                final DownloadProgressListener listener = ExoOfflineManager.this.downloadProgressListener;

                if (listener != null) {
                    final List<Download> downloads = downloadManager.getCurrentDownloads();
                    for (Download download : downloads) {
                        if (download.state != Download.STATE_DOWNLOADING) {
                            continue;
                        }

                        final float percentDownloaded = download.getPercentDownloaded();
                        final long bytesDownloaded = download.getBytesDownloaded();
                        final long totalSize = percentDownloaded > 0 ? (long) (100f * bytesDownloaded / percentDownloaded) : -1;
                        final String dataJson = Util.fromUtf8Bytes(download.request.data);

                        PrefetchConfig prefetchConfig = extractPrefetchConfig(dataJson);
                        if (prefetchConfig != null && prefetchConfig.getAssetPrefetchSize() > 0) {
                                log.d("XXX Downloaded: " + bytesDownloaded / 1000000 + " Mb");
                                if (bytesDownloaded / 1000000 >= prefetchConfig.getAssetPrefetchSize()) { // default 2mb
                                    downloadManager.setStopReason(download.request.id, StopReason.prefetchDone.toExoCode()); // prefetchDone
                                }
                        }
                        final String assetId = download.request.id;
                        listener.onDownloadProgress(assetId, bytesDownloaded, totalSize, percentDownloaded);
                    }

                }

                postEventDelayed(this, 250);
            }
        });
    }

    private PrefetchConfig extractPrefetchConfig(String dataJson) {
        String prefetchConfigStr = null;
        JsonObject jsonObject = new JsonParser().parse(dataJson).getAsJsonObject();

        if (jsonObject.has("prefetchConfig")) {
            prefetchConfigStr = jsonObject.get("prefetchConfig").getAsString();
        }

        if (prefetchConfigStr != null) {
            return gson.fromJson(prefetchConfigStr, PrefetchConfig.class);
        }
        return null;
    }

    private void maybeRegisterDrmAsset(String assetId, DownloadType downloadType, int delayMillis) {
        bgHandler.postDelayed(() -> {
            registerDrmAsset(assetId, downloadType);
        }, delayMillis);
    }

//    private CacheDataSourceFactory buildReadOnlyCacheDataSource(DataSource.Factory upstreamFactory, Cache cache) {
//        return new CacheDataSourceFactory(
//                cache,
//                upstreamFactory,
//                new FileDataSourceFactory(),
//                null,
//                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null
//        );
//    }

    @Override
    public void prepareAsset(@NonNull PKMediaEntry mediaEntry, @NonNull SelectionPrefs selectionPrefs, @NonNull PrepareCallback prepareCallback) {

        SourceSelector selector = new SourceSelector(mediaEntry, preferredMediaFormat);
        //drmSessionManager = null;
        deferredDrmSessionManager = null;
        final String assetId = mediaEntry.getId();
        final PKMediaSource source = selector.getSelectedSource();
        final PKDrmParams drmData = selector.getSelectedDrmParams();
        final PKMediaFormat mediaFormat = source.getMediaFormat();
        final String url = source.getUrl();
        final Uri uri = Uri.parse(url);

        final DownloadHelper downloadHelper;

        postEvent(() -> prepareCallback.onSourceSelected(assetId, source, drmData));
        DefaultTrackSelector.Parameters defaultTrackSelectorParameters =  DownloadHelper.getDefaultTrackSelectorParameters(appContext);
        switch (mediaFormat) {
            // DASH: clear or with Widevine
            case dash:
                if (drmData != null && drmData.getScheme() != null) {
                    final DrmCallback drmCallback = new DrmCallback(httpDataSourceFactory, null); // TODO license adapter usage?
                    //drmSessionManager = buildDrmSessionManager(drmData);
                    deferredDrmSessionManager = new DeferredDrmSessionManager(new Handler(Looper.getMainLooper()), drmCallback, error -> {
                        log.e("XXX onPrepareError drm call failed");
                        postEvent(() -> prepareCallback.onPrepareError(assetId, new IllegalArgumentException("XXX drm call failed")));
                    }, true);
                    deferredDrmSessionManager.setMediaSource(source);
                    downloadHelper = DownloadHelper.forDash(uri, httpDataSourceFactory,
                            new DefaultRenderersFactory(appContext), deferredDrmSessionManager, defaultTrackSelectorParameters);
                } else {
                    downloadHelper = DownloadHelper.forDash(uri, httpDataSourceFactory,
                            new DefaultRenderersFactory(appContext), DrmSessionManager.getDummyDrmSessionManager(), defaultTrackSelectorParameters);
                }
                break;

            // HLS: clear/aes only
            case hls:
                downloadHelper = DownloadHelper.forHls(uri, httpDataSourceFactory,
                        new DefaultRenderersFactory(appContext), DrmSessionManager.getDummyDrmSessionManager(), defaultTrackSelectorParameters);
                break;

            // Progressive
            case mp4:
            case mp3:
                downloadHelper = DownloadHelper.forProgressive(appContext, uri);
                break;

            default:
                postEvent(() -> prepareCallback.onPrepareError(assetId, new IllegalArgumentException("Unsupported media format " + mediaFormat)));
                return;
        }

        downloadHelper.prepare(new DownloadHelper.Callback() {
            @Override
            public void onPrepared(DownloadHelper helper) {

                //helper.addAudioLanguagesToSelection("en");
                //helper.addTextLanguagesToSelection(true, "fr");
                //downloadAllVideoTracks(helper, downloadHelper, prefs);
                //downloadAllTracks(helper, downloadHelper, prefs);

                //prefetchConfig
                if (selectionPrefs.textLanguages != null && !selectionPrefs.textLanguages.isEmpty()) {
                    //helper.addTextLanguagesToSelection(true, "fr");
                    helper.addTextLanguagesToSelection(true, selectionPrefs.textLanguages.toArray(new String[0]));
                }
                if (selectionPrefs.audioLanguages != null && !selectionPrefs.audioLanguages.isEmpty()) {
                    //helper.addAudioLanguagesToSelection("en");
                    helper.addAudioLanguagesToSelection(selectionPrefs.audioLanguages.toArray(new String[0]));
                }
                if (selectionPrefs.videoBitrate == null && selectionPrefs.allAudioLanguages && selectionPrefs.allTextLanguages) {
                    downloadAllTracks(helper, downloadHelper, selectionPrefs);
                } else {
                    downloadAllVideoTracks(helper, downloadHelper, selectionPrefs);
                }

                long selectedSize = estimateTotalSize(helper, estimatedHlsAudioBitrate);
                final ExoAssetInfo assetInfo = new ExoAssetInfo(DownloadType.FULL, assetId, AssetDownloadState.none, selectedSize, -1, helper);
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

    @Override
    public void prefetchAsset(@NonNull PKMediaEntry mediaEntry, @NonNull PrefetchConfig prefetchConfig, @NonNull PrefetchCallback prefetchCallback) {

        prepareAsset(mediaEntry, prefetchConfig.getSelectionPrefs(), new PrepareCallback() {
            @Override
            public void onPrepared(@NonNull String assetId, @NonNull AssetInfo assetInfo, @Nullable Map<TrackType, List<Track>> selected) {
                ((ExoAssetInfo)assetInfo).downloadType = DownloadType.PREFETCH;
                ((ExoAssetInfo)assetInfo).prefetchConfig = prefetchConfig;
                startAssetDownload(assetInfo);
            }

            @Override
            public void onPrepareError(@NonNull String assetId, @NonNull Exception error) {
                log.e("onPrefetchError");
                postEvent(() -> prefetchCallback.onPrefetchError(assetId, error));
            }
        });
    }

    private void downloadAllTracks(DownloadHelper helper, DownloadHelper downloadHelper, @NonNull SelectionPrefs prefs) {

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = helper.getMappedTrackInfo(0);
        for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
            downloadHelper.clearTrackSelections(periodIndex);
            for (int rendererIndex = 0; rendererIndex < 3 ; rendererIndex++) { // 0, 1, 2 run only over video audio and text tracks
                List<DefaultTrackSelector.SelectionOverride> selectionOverrides = new ArrayList<>();
                TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);

                for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {
                    //run through the all tracks in current trackGroup.
                    TrackGroup trackGroup = trackGroupArray.get(groupIndex);
                    for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                        selectionOverrides.add(new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex));
                    }

                    downloadHelper.addTrackSelectionForSingleRenderer(
                            periodIndex,
                            rendererIndex,
                            buildExoParameters(prefs),
                            selectionOverrides);
                }
            }
        }
    }

    private void downloadAllVideoTracks(DownloadHelper helper, DownloadHelper downloadHelper, @NonNull SelectionPrefs prefs) {

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = helper.getMappedTrackInfo(0);
        for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
            downloadHelper.clearTrackSelections(periodIndex);
            for (int rendererIndex = 0; rendererIndex < 1 ; rendererIndex++) { // 0 run only over video tracks
                List<DefaultTrackSelector.SelectionOverride> selectionOverrides = new ArrayList<>();
                TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);

                for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {
                    //run through the all tracks in current trackGroup.
                    TrackGroup trackGroup = trackGroupArray.get(groupIndex);
                    for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                        selectionOverrides.add(new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex));
                    }

                    downloadHelper.addTrackSelectionForSingleRenderer(
                            periodIndex,
                            rendererIndex,
                            buildExoParameters(prefs),
                            selectionOverrides);
                }
            }
        }
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

//    @Nullable
//    private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(PKDrmParams drmData) {
//        if (drmData == null) {
//            return null;
//        }
//
//        if (drmData.getScheme() != PKDrmParams.Scheme.WidevineCENC) {
//            throw new IllegalArgumentException("Unsupported DRM Scheme " + drmData.getScheme());
//        }
//
//        final HttpMediaDrmCallback mediaDrmCallback = new HttpMediaDrmCallback(drmData.getLicenseUri(), httpDataSourceFactory);
//        DefaultDrmSessionManager drmSessionManager = null;
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            drmSessionManager = new DefaultDrmSessionManager.Builder()
//                    .setUuidAndExoMediaDrmProvider(MediaSupport.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
//                    .build(mediaDrmCallback);
//        } else {
//            drmSessionManager = new DefaultDrmSessionManager.Builder().build(mediaDrmCallback);
//        }
//        return drmSessionManager;
//     }

    private DefaultTrackSelector.Parameters buildExoParameters(SelectionPrefs prefs) {
        //MappingTrackSelector.MappedTrackInfo mappedTrackInfo = downloadHelper.getMappedTrackInfo(/* periodIndex= */ 0);
        //return new DefaultTrackSelector.ParametersBuilder(appContext).setMaxVideoSizeSd().build();
        return DefaultTrackSelector.Parameters.getDefaults(appContext);   // TODO: 2019-07-31
        //return new DefaultTrackSelector.ParametersBuilder(appContext).build();
        //return DownloadHelper.getDefaultTrackSelectorParameters(appContext);
        //return  DefaultTrackSelector.Parameters.DEFAULT_WITHOUT_CONTEXT.buildUpon().setForceHighestSupportedBitrate(true).build();
    }

    public static ExoOfflineManager getInstance(Context context) {
        if (instance == null) {
            instance = new ExoOfflineManager(context.getApplicationContext());
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
        // Pause all non prefetch downloads.

        List<Download> downloads = downloadManager.getCurrentDownloads();
        for (Download download : downloads) {
            if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_QUEUED) {
                pauseAssetDownload(download.request.id);
            }
        }
//        DownloadService.sendPauseDownloads(
//                appContext,
//                ExoDownloadService.class,
//                /* foreground= */ false);

    }

    @Override
    public void resumeDownloads() {
        // Resume all non prefetch downloads.

        List<Download> downloads = downloadManager.getCurrentDownloads();
        for (Download download : downloads) {
            if (download.state == Download.STATE_STOPPED && download.state != StopReason.prefetchDone.toExoCode() || download.state == Download.STATE_QUEUED) {
                resumeAssetDownload(download.request.id);
            }
        }
//        DownloadService.sendResumeDownloads(
//                appContext,
//                ExoDownloadService.class,
//                /* foreground= */ false);
    }

    @NonNull
    @Override
    public AssetInfo getAssetInfo(@NonNull String assetId) {
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

    @NonNull
    @Override
    public List<AssetInfo> getAssetsInState(@NonNull AssetDownloadState state) {

        @Download.State int[] exoStates;
        switch (state) {
            case started:
                exoStates = new int[]{Download.STATE_DOWNLOADING, Download.STATE_QUEUED, Download.STATE_RESTARTING};
                break;
            case completed:
                exoStates = new int[]{Download.STATE_COMPLETED};
                break;
            case failed:
                exoStates = new int[]{Download.STATE_FAILED};
                break;
            case removing:
                exoStates = new int[]{Download.STATE_REMOVING};
                break;
            case paused:
                exoStates = new int[]{Download.STATE_STOPPED};
                break;
            case none:
            case prepared:
            default:
                return Collections.emptyList();
        }

        final DownloadCursor downloads;
        try {
            downloads = downloadManager.getDownloadIndex().getDownloads(exoStates);
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
    public PKMediaEntry getLocalPlaybackEntry(@NonNull String assetId) throws IOException {

        final Download download = downloadManager.getDownloadIndex().getDownload(assetId);

        if (download == null ||
                (download.state == Download.STATE_STOPPED && download.stopReason != StopReason.prefetchDone.toExoCode()) ||
                (download.state != Download.STATE_COMPLETED && download.state != Download.STATE_STOPPED)) {
            return null;
        }

        final CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(downloadCache, httpDataSourceFactory);
        final MediaSource mediaSource = DownloadHelper.createMediaSource(download.request, cacheDataSourceFactory, deferredDrmSessionManager);

        final PKMediaSource localMediaSource = lam.getLocalMediaSource(assetId, mediaSource);
        if (deferredDrmSessionManager != null) {
            deferredDrmSessionManager.setMediaSource(localMediaSource);
        }
        if (TextUtils.isEmpty(localMediaSource.getUrl())) {
            localMediaSource.setUrl(download.request.uri.toString());
        }
        return new PKMediaEntry().setId(assetId).setSources(Collections.singletonList(localMediaSource));
    }

    @Override
    public void startAssetDownload(@NonNull AssetInfo assetInfo) {

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

        JsonObject data = new JsonObject();
        byte[] bytes;
        data.addProperty("estimatedSizeBytes", exoAssetInfo.getEstimatedSize());
        if (exoAssetInfo.getDownloadType() == DownloadType.PREFETCH && exoAssetInfo.getPrefetchConfig() != null) {
            data.addProperty("prefetchConfig", gson.toJson(exoAssetInfo.getPrefetchConfig(), PrefetchConfig.class));
        }
        bytes = data.toString().getBytes();
//XXX
        final DownloadRequest downloadRequest = helper.getDownloadRequest(assetInfo.getAssetId(), bytes);

        DownloadService.sendAddDownload(appContext, ExoDownloadService.class, downloadRequest, false);
    }

    @Override
    public void resumeAssetDownload(@NonNull String assetId) {

        // Clear the stop reason for a single download.
        DownloadService.sendSetStopReason(
                appContext,
                ExoDownloadService.class,
                assetId,
                Download.STOP_REASON_NONE,
                /* foreground= */ false);
    }

    @Override
    public void pauseAssetDownload(@NonNull String assetId) {
        // Set the stop reason for a single download.
        DownloadService.sendSetStopReason(
                appContext,
                ExoDownloadService.class,
                assetId,
                StopReason.pause.toExoCode(),
                /* foreground= */ false);
    }

    @Override
    public boolean removeAsset(@NonNull String assetId) {
        try {
            final Pair<PKMediaSource, PKDrmParams> pair = pendingDrmRegistration.get(assetId);
            boolean isDRM = true;
            if (pair == null || pair.first == null || pair.second == null) {
                isDRM = false;
            }
            if (isDRM) {
                final byte[] drmInitData = getDrmInitData(assetId);
                if (drmInitData == null) {
                    log.e("removeAsset failed");
                    return false;
                }
                lam.unregisterAsset(assetId, drmInitData);
            }

            DownloadService.sendRemoveDownload(appContext, ExoDownloadService.class, assetId, false);
            removeAssetSourceId(assetId);

        } catch (IOException | InterruptedException e) {
            log.e("removeAsset failed ", e);
            return false;
        }

        return true;
    }

    @Override
    protected byte[] getDrmInitData(String assetId) throws IOException, InterruptedException {
        final Download download = downloadManager.getDownloadIndex().getDownload(assetId);
        if (download == null) {
            return null;
        }

        final Uri uri = download.request.uri;
        final CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(downloadCache, httpDataSourceFactory);
        return getDrmInitData(cacheDataSourceFactory, uri.toString());
    }


    private void registerDrmAsset(String assetId, DownloadType downloadType) {

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
            lam.registerWidevineDashAsset(assetId, licenseUri, drmInitData);
            postEvent(() -> listener.onRegistered(assetId, getDrmStatus(assetId, drmInitData)));

            pendingDrmRegistration.remove(assetId);

        } catch (IOException | InterruptedException e) {
            postEvent(() -> listener.onRegisterError(assetId, downloadType, e));

        } catch (LocalAssetsManager.RegisterException e) {
            postEvent(() -> listener.onRegisterError(assetId, downloadType, e));
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

    private byte[] getDrmInitData(CacheDataSourceFactory dataSourceFactory, String contentUri) throws IOException, InterruptedException {
        if (PKMediaFormat.valueOfUrl(contentUri) != PKMediaFormat.dash ) {
            return null;
        }

        if (!contentUri.contains(".mpd")) {
            return null;
        }
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
