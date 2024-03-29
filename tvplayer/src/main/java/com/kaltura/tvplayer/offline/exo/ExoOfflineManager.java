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
import com.kaltura.android.exoplayer2.MediaItem;
import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.kaltura.android.exoplayer2.drm.DrmInitData;
import com.kaltura.android.exoplayer2.drm.DrmSessionManager;
import com.kaltura.android.exoplayer2.ext.okhttp.OkHttpDataSource;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadCursor;
import com.kaltura.android.exoplayer2.offline.DownloadHelper;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloadRequest;
import com.kaltura.android.exoplayer2.offline.DownloadService;
import com.kaltura.android.exoplayer2.scheduler.Requirements;
import com.kaltura.android.exoplayer2.source.TrackGroup;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.source.dash.DashUtil;
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest;
import com.kaltura.android.exoplayer2.source.hls.HlsManifest;
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsMultivariantPlaylist;
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsPlaylist;
import com.kaltura.android.exoplayer2.source.hls.playlist.HlsPlaylistParser;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.ExoTrackSelection;
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.kaltura.android.exoplayer2.upstream.ParsingLoadable;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.kaltura.android.exoplayer2.upstream.cache.SimpleCache;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.dtg.DownloadRequestParams;
import com.kaltura.dtg.KalturaDownloadRequestAdapter;
import com.kaltura.dtg.exoparser.util.UriUtil;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.LocalAssetsManagerExo;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.drm.DrmCallback;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.player.PKHttpClientManager;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.playkit.utils.NativeCookieJarBridge;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.offline.AbstractOfflineManager;
import com.kaltura.tvplayer.offline.OfflineManagerSettings;
import com.kaltura.tvplayer.offline.Prefetch;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;

public class ExoOfflineManager extends AbstractOfflineManager {

    private static final PKLog log = PKLog.get("ExoOfflineManager");
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private final String VERSION_STRING = "playkit-dtg-exo/android-" + PlayKitManager.VERSION_STRING;
    private final String PARAM_CLIENT_TAG = "clientTag";

    private static final int THREAD_POOL_SIZE = 8;
    private static final int REGISTER_ASSET_AFTER_5_SEC = 5000;
    private static final int REGISTER_ASSET_NOW = 0;

    private final Gson gson = new Gson();
    private static ExoOfflineManager instance;
    private PrefetchManager prefetchManager;
    final DownloadManager downloadManager;
    private DownloadHelper assetDownloadHelper;
    private final DatabaseProvider databaseProvider;
    private String sessionId;
    private String applicationName;

    private Runnable downloadProgressTracker;
    private final Handler bgHandler = createBgHandler();
    private File downloadDirectory;
    private final Cache downloadCache;

    private final String userAgent = Utils.getUserAgent(appContext) + " ExoDownloadPlayerLib/" + ExoPlayerLibraryInfo.VERSION;

    private final OkHttpDataSource.Factory httpDataSourceFactory = new OkHttpDataSource.Factory((Call.Factory) PKHttpClientManager.newClientBuilder()
            .cookieJar(NativeCookieJarBridge.sharedCookieJar)
            .followRedirects(true)
            .followSslRedirects(offlineManagerSettings != null ?
                    offlineManagerSettings.isCrossProtocolRedirectEnabled() : OfflineManagerSettings.CROSS_PROTOCOL_ENABLED)
            .connectTimeout(offlineManagerSettings != null ?
                    offlineManagerSettings.getHttpTimeoutMillis() :
                    DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .readTimeout(offlineManagerSettings != null ?
                    offlineManagerSettings.getHttpTimeoutMillis() :
                    DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .build())
            .setUserAgent(userAgent);

    private final DownloadManager.Listener exoDownloadManagerListner = new DownloadManager.Listener() {
        @Override
        public void onInitialized(@NonNull DownloadManager downloadManager) {}

        @Override
        public void onDownloadsPausedChanged(@NonNull DownloadManager downloadManager, boolean downloadsPaused) {
            log.d("onDownloadsPausedChanged: " + downloadsPaused);
        }

        @Override
        public void onDownloadChanged(@NonNull DownloadManager downloadManager, Download download, @Nullable Exception finalException) {
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
                    if (downloadProgressTracker == null) {
                        sendDownloadProgress();
                    }
                    if (downloadType != DownloadType.PREFETCH) {
                        maybeRegisterDrmAsset(assetId, downloadType, REGISTER_ASSET_AFTER_5_SEC);
                    }
                    break;
                case Download.STATE_FAILED:
                    log.d("STATE_FAILED: " + assetId);
                    String message = finalException != null ? finalException.getMessage() : "Failed for unknown reason";
                    listener.onAssetDownloadFailed(assetId, downloadType,
                            new AssetDownloadException("DownloadType: " + downloadType.name() + " Error: " + message));
                    break;
                case Download.STATE_QUEUED:
                    log.d("STATE_QUEUED: " + assetId);
                    listener.onAssetDownloadPending(assetId, downloadType);
                    break;
                case Download.STATE_REMOVING:
                    log.d("STATE_REMOVING: " + assetId);
                    //listener.onAssetRemoveStart(assetId, downloadType);
                    break;
                case Download.STATE_RESTARTING:
                    log.d("STATE_RESTARTING: " + assetId);
                    // TODO: 2019-09-04 what does it mean?
                    break;
                case Download.STATE_STOPPED:
                    log.d("STATE_STOPPED: " + assetId);
                    if (StopReason.fromExoReason(download.stopReason) == StopReason.pause) {
                        listener.onAssetDownloadPaused(assetId, downloadType);
                    } else if (StopReason.fromExoReason(download.stopReason) == StopReason.prefetchDone) {
                        maybeRegisterDrmAsset(assetId, downloadType, REGISTER_ASSET_NOW);
                        listener.onAssetPrefetchComplete(assetId, downloadType);
                    }
            }
        }

        @Override
        public void onDownloadRemoved(@NonNull DownloadManager downloadManager, Download download) {
            final String dataJson = Util.fromUtf8Bytes(download.request.data);
            PrefetchConfig prefetchConfig = extractPrefetchConfig(dataJson);
            DownloadType downloadType = DownloadType.FULL;
            if (prefetchConfig != null) {
                downloadType = DownloadType.PREFETCH;
            }
            getListener().onAssetRemoved(download.request.id, downloadType);
        }

        @Override
        public void onIdle(@NonNull DownloadManager downloadManager) {}

        @Override
        public void onRequirementsStateChanged(@NonNull DownloadManager downloadManager, @NonNull Requirements requirements, int notMetRequirements) {}

        @Override
        public void onWaitingForRequirementsChanged(@NonNull DownloadManager downloadManager, boolean waitingForRequirements) {}
    };

    @NonNull
    private Handler createBgHandler() {
        final HandlerThread bgHandlerThread = new HandlerThread("bgHandlerThread");
        bgHandlerThread.start();
        return new Handler(bgHandlerThread.getLooper());
    }

    private ExoOfflineManager(Context context) {
        super(context);

        downloadDirectory = context.getExternalFilesDir(null);
        if (downloadDirectory == null) {
            downloadDirectory = context.getFilesDir();
        }
        File downloadContentDirectory = new File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY);

        databaseProvider = new StandaloneDatabaseProvider(context);
        downloadCache = new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), databaseProvider);

        downloadManager = new DownloadManager(
                context,
                databaseProvider,
                downloadCache,
                httpDataSourceFactory,
                Executors.newFixedThreadPool(THREAD_POOL_SIZE)
        );

        downloadManager.setRequirements(new Requirements(Requirements.NETWORK|Requirements.DEVICE_STORAGE_NOT_LOW));
        downloadManager.setMaxParallelDownloads(offlineManagerSettings != null ?
                offlineManagerSettings.getMaxConcurrentDownloads() :
                OfflineManagerSettings.MAX_PARALLEL_DOWNLOADS);
        downloadManager.setMinRetryCount(offlineManagerSettings != null ?
                offlineManagerSettings.getMaxDownloadRetries() :
                OfflineManagerSettings.MIN_RETRY_COUNT);
        addExoDownloadManagerListener();
    }

    protected void addExoDownloadManagerListener() {
        if (downloadManager != null) {
            downloadManager.addListener(exoDownloadManagerListner);
        }
    }

    protected void removeExoDownloadManagerListener() {
        if (downloadManager != null) {
            downloadManager.removeListener(exoDownloadManagerListner);
        }
    }

    private Runnable getDownloadTrackerRunnable() {
        if (downloadProgressTracker == null) {
            //log.d("getDownloadTrackerRunnable creating new runnable");
            downloadProgressTracker =  new Runnable() {
                @Override
                public void run() {
                    log.d("sendDownloadProgress executed");
                    final DownloadProgressListener listener = ExoOfflineManager.this.downloadProgressListener;

                    if (listener != null) {
                        final List<Download> downloads = downloadManager.getCurrentDownloads();
                        int nonDownloadingCounter = 0;
                        for (Download download : downloads) {
                            if (download.state != Download.STATE_DOWNLOADING) {
                                nonDownloadingCounter++;
                                continue;
                            }

                            final float percentDownloaded = download.getPercentDownloaded();
                            final long bytesDownloaded = download.getBytesDownloaded();
                            final long totalSize = percentDownloaded > 0 ? (long) (100f * bytesDownloaded / percentDownloaded) : -1;
                            final String dataJson = Util.fromUtf8Bytes(download.request.data);

                            PrefetchConfig prefetchConfig = extractPrefetchConfig(dataJson);
                            if (prefetchConfig != null && prefetchConfig.getAssetPrefetchSize() > 0) {
                                log.d("prefetch downloaded: " + bytesDownloaded / 1000000 + " Mb");
                                if (bytesDownloaded / 1000000 >= prefetchConfig.getAssetPrefetchSize()) { // default 2mb
                                    downloadManager.setStopReason(download.request.id, StopReason.prefetchDone.toExoCode()); // prefetchDone
                                }
                            }
                            listener.onDownloadProgress(download.request.id, bytesDownloaded, totalSize, percentDownloaded);
                        }
                        if (nonDownloadingCounter == downloads.size()) {
                            log.d("exit sendDownloadProgress");
                            downloadProgressTracker = null;
                            return;
                        }
                    }
                    postEventDelayed(this, 250);
                }
            };
        }
        return downloadProgressTracker;
    }

    private void sendDownloadProgress( ) {
        postEvent(getDownloadTrackerRunnable());
    }

    private PrefetchConfig extractPrefetchConfig(String dataJson) {
        String prefetchConfigStr = null;
        JsonObject jsonObject = JsonParser.parseString(dataJson).getAsJsonObject();

        if (jsonObject.has("prefetchConfig")) {
            prefetchConfigStr = jsonObject.get("prefetchConfig").getAsString();
        }

        if (prefetchConfigStr != null) {
            return gson.fromJson(prefetchConfigStr, PrefetchConfig.class);
        }
        return null;
    }

    private void maybeRegisterDrmAsset(String assetId, DownloadType downloadType, int delayMillis) {
        bgHandler.postDelayed(() -> registerDrmAsset(assetId, downloadType), delayMillis);
    }

    private List<MediaItem.SubtitleConfiguration> buildSubtitlesList(List<PKExternalSubtitle> externalSubtitleList) {
        List<MediaItem.SubtitleConfiguration> subtitleList = new ArrayList<>();

        if (externalSubtitleList != null && externalSubtitleList.size() > 0) {
            for (int subtitlePosition = 0 ; subtitlePosition < externalSubtitleList.size() ; subtitlePosition ++) {
                PKExternalSubtitle pkExternalSubtitle = externalSubtitleList.get(subtitlePosition);
                String subtitleMimeType = pkExternalSubtitle.getMimeType() == null ? "Unknown" : pkExternalSubtitle.getMimeType();

                MediaItem.SubtitleConfiguration.Builder builder = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(pkExternalSubtitle.getUrl()));
                builder.setMimeType(subtitleMimeType);
                // "-" is important to be added between lang and mimetype
                // This is how we understand that this is external subtitle in TrackSelectionHelper
                builder.setLanguage(pkExternalSubtitle.getLanguage() + "-" + subtitleMimeType);
                builder.setSelectionFlags(pkExternalSubtitle.getSelectionFlags());
                builder.setRoleFlags(pkExternalSubtitle.getRoleFlag());
                builder.setLabel(pkExternalSubtitle.getLabel());

                subtitleList.add(builder.build());
            }
        }
        return subtitleList;
    }

    private String getDrmLicenseUrl(PKMediaSource mediaSource, PKDrmParams.Scheme scheme) {
        String licenseUrl = null;

        if (mediaSource.hasDrmParams()) {
            List<PKDrmParams> drmData = mediaSource.getDrmData();
            for (PKDrmParams pkDrmParam : drmData) {
                if (scheme == pkDrmParam.getScheme()) {
                    licenseUrl = pkDrmParam.getLicenseUri();
                    break;
                }
            }
        }
        return licenseUrl;
    }

    @Nullable
    private Format getFirstFormatWithDrmInitData(DownloadHelper downloadHelper) {
        for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = downloadHelper.getMappedTrackInfo(periodIndex);

            for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
                TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);

                for (int trackGroupIndex = 0; trackGroupIndex < trackGroups.length; trackGroupIndex++) {
                    TrackGroup trackGroup = trackGroups.get(trackGroupIndex);

                    for (int formatIndex = 0; formatIndex < trackGroup.length; formatIndex++) {
                        Format format = trackGroup.getFormat(formatIndex);
                        if (format.drmInitData != null) {
                            return format;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void prepareAsset(@NonNull PKMediaEntry mediaEntry, @NonNull SelectionPrefs selectionPrefs, @NonNull PrepareCallback prepareCallback) {

        SourceSelector selector = new SourceSelector(mediaEntry, preferredMediaFormat);
        final String assetId = mediaEntry.getId();
        final PKMediaSource source = selector.getSelectedSource();
        if (source == null || TextUtils.isEmpty(source.getUrl()) || (selectionPrefs.downloadType == DownloadType.PREFETCH && source.getMediaFormat() != PKMediaFormat.hls && source.getMediaFormat() != PKMediaFormat.dash)) {
            postEvent(() -> prepareCallback.onPrepareError(assetId, selectionPrefs.downloadType, new UnsupportedOperationException("Invalid source")));
            return;
        }
        final PKDrmParams drmData = selector.getSelectedDrmParams();
        final PKMediaFormat mediaFormat = source.getMediaFormat();
        final String url = source.getUrl();
        Uri uri;

        if (prefetchManager.isPrefetched(assetId) && selectionPrefs.downloadType == DownloadType.FULL) {
            log.d("Removing prefetched media before full download");
            removeAsset(assetId);
        } else if (prefetchManager.isPrefetched(assetId) && selectionPrefs.downloadType == DownloadType.PREFETCH) {
            if (getDrmStatus(assetId).isValid()) {
                log.d("Media already prefetched");
                postEvent(() -> prepareCallback.onPrepared(assetId, prefetchManager.getAssetInfoByAssetId(assetId), null));
                return;
            } else {
                log.d("Media already prefetched but asset's license was expired so removing and prefetching.");
                removeAsset(assetId);
            }
        }

        postEvent(() -> prepareCallback.onSourceSelected(assetId, source, drmData));
        DefaultTrackSelector.Parameters defaultTrackSelectorParameters =  DownloadHelper.getDefaultTrackSelectorParameters(appContext);

        DownloadRequestParams downloadRequestParams;
        if (offlineManagerSettings != null && offlineManagerSettings.getDownloadRequestAdapter() != null) {
            downloadRequestParams = offlineManagerSettings.getDownloadRequestAdapter().adapt(new DownloadRequestParams(Uri.parse(url), null));
            if (downloadRequestParams.headers != null) {
                httpDataSourceFactory.setDefaultRequestProperties(downloadRequestParams.headers);
            }
        } else {
            downloadRequestParams = new KalturaDownloadRequestAdapter(sessionId, applicationName).adapt(new DownloadRequestParams(Uri.parse(url), null));
        }

        uri = downloadRequestParams.url;
        uri = replaceQueryParam(uri);

        MediaItem.ClippingConfiguration clippingConfiguration = new MediaItem.ClippingConfiguration
                .Builder()
                .setStartPositionMs(0L)
                .setEndPositionMs(C.TIME_END_OF_SOURCE)
                .build();

        MediaItem.Builder builder =
                new MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(mediaFormat.mimeType)
                        .setSubtitleConfigurations(buildSubtitlesList(mediaEntry.getExternalSubtitleList()))
                        .setClippingConfiguration(clippingConfiguration);

        if (mediaFormat == PKMediaFormat.dash || mediaFormat == PKMediaFormat.hls) {
            if (drmData != null) {
                boolean setDrmSessionForClearTypes = false;
                // selecting WidevineCENC as default right now
                PKDrmParams.Scheme scheme = PKDrmParams.Scheme.WidevineCENC;
                String licenseUri = getDrmLicenseUrl(source, scheme);

                Map<String, String> headers = new HashMap<>(); //requestParams.headers;

                MediaItem.DrmConfiguration.Builder drmConfigurationBuilder = new MediaItem.DrmConfiguration
                        .Builder((scheme == PKDrmParams.Scheme.WidevineCENC) ? MediaSupport.WIDEVINE_UUID : MediaSupport.PLAYREADY_UUID)
                        .setLicenseUri(licenseUri)
                        .setMultiSession(false)
                        .setForceDefaultLicenseUri(false)
                        .setLicenseRequestHeaders(headers);

                if (setDrmSessionForClearTypes) {
                    List<Integer> tracks = new ArrayList<>();
                    tracks.add(C.TRACK_TYPE_VIDEO);
                    tracks.add(C.TRACK_TYPE_AUDIO);
                    drmConfigurationBuilder.setForcedSessionTrackTypes(tracks);
                }

                builder.setDrmConfiguration(drmConfigurationBuilder.build());
            }
        }

        MediaItem mediaItem = builder.build();
        if (mediaFormat != PKMediaFormat.dash && mediaFormat != PKMediaFormat.hls) {
            assetDownloadHelper = DownloadHelper.forMediaItem(
                    appContext, mediaItem, new DefaultRenderersFactory(appContext), httpDataSourceFactory);
        } else if (drmData != null && drmData.getScheme() != null) {
            final DrmCallback drmCallback = new DrmCallback(httpDataSourceFactory,
                    offlineManagerSettings == null ? null : offlineManagerSettings.getLicenseRequestAdapter());
            DeferredDrmSessionManager deferredDrmSessionManager = new DeferredDrmSessionManager(new Handler(Looper.getMainLooper()), drmCallback, error -> {
                log.e("onPrepareError drm call failed");
                postEvent(() -> prepareCallback.onPrepareError(assetId, selectionPrefs.downloadType, new IllegalArgumentException("drm call failed")));
            }, true, forceWidevineL3Playback);
            deferredDrmSessionManager.setMediaSource(source);
            assetDownloadHelper =  DownloadHelper.forMediaItem(mediaItem, defaultTrackSelectorParameters, new DefaultRenderersFactory(appContext), httpDataSourceFactory, deferredDrmSessionManager);
        } else {
            assetDownloadHelper = DownloadHelper.forMediaItem(mediaItem, defaultTrackSelectorParameters, new DefaultRenderersFactory(appContext), httpDataSourceFactory, DrmSessionManager.DRM_UNSUPPORTED);
        }

        Uri assetUri = uri;

        assetDownloadHelper.prepare(new DownloadHelper.Callback() {
            @Override
            public void onPrepared(@NonNull DownloadHelper downloadHelper) {
                @Nullable Format format = getFirstFormatWithDrmInitData(downloadHelper);

                if ((format == null && downloadHelper.getPeriodCount() == 0) ||
                        (format != null && downloadHelper.getPeriodCount() > 0 && !hasAnyValidRenderer(downloadHelper.getMappedTrackInfo(0)))) {
                    log.d("No Period or no valid renderer found. Downloading entire stream.");
                    long selectedSize = estimateTotalSize(downloadHelper,
                            assetId,
                            selectionPrefs.downloadType,
                            assetUri,
                            OfflineManagerSettings.DEFAULT_HLS_AUDIO_BITRATE_ESTIMATION,
                            prepareCallback);
                    final ExoAssetInfo assetInfo = new ExoAssetInfo(DownloadType.FULL, assetId, AssetDownloadState.none, selectedSize, -1, downloadHelper);
                    sendOnPreparedEvent(downloadHelper, assetInfo, prepareCallback, assetId, selectionPrefs);
                    return;
                }

                ExoPlayerTrackSelection.selectTracks(appContext, downloadHelper,selectionPrefs);

                long selectedSize = estimateTotalSize(downloadHelper,
                        assetId,
                        selectionPrefs.downloadType,
                        assetUri,
                        OfflineManagerSettings.DEFAULT_HLS_AUDIO_BITRATE_ESTIMATION,
                        prepareCallback);


                final ExoAssetInfo assetInfo = new ExoAssetInfo(selectionPrefs.downloadType, assetId, AssetDownloadState.none, selectedSize, -1, downloadHelper);
                if ((mediaFormat == PKMediaFormat.dash || mediaFormat == PKMediaFormat.hls) &&
                        format != null &&
                        format.drmInitData != null &&
                        drmData != null) {
                    DrmRegistrationMetaData drmRegistrationMetaData = new DrmRegistrationMetaData(format, false);
                    pendingDrmRegistration.put(assetId, new Pair<>(source, drmRegistrationMetaData));
                }

                saveAssetSourceId(assetId, source.getId());
                sendOnPreparedEvent(downloadHelper, assetInfo, prepareCallback, assetId, selectionPrefs);
            }

            @Override
            public void onPrepareError(@NonNull DownloadHelper downloadHelper, @NonNull IOException error) {
                log.e("onPrepareError " + error.getMessage());
                downloadHelper.release();
                postEvent(() -> prepareCallback.onPrepareError(assetId, selectionPrefs.downloadType, error));
            }
        });
    }

    private Uri replaceQueryParam(Uri uri) {
        uri = UriUtil.removeQueryParameter(uri, PARAM_CLIENT_TAG);
        uri = uri.buildUpon().appendQueryParameter(PARAM_CLIENT_TAG, VERSION_STRING).build();
        return uri;
    }

    private void sendOnPreparedEvent(@NonNull DownloadHelper downloadHelper, ExoAssetInfo assetInfo, @NonNull PrepareCallback prepareCallback, String assetId, @NonNull SelectionPrefs selectionPrefs) {
        if (isLowDiskSpace(assetInfo.getEstimatedSize())) {
            downloadHelper.release();
            postEvent(() -> prepareCallback.onPrepareError(assetId, selectionPrefs.downloadType, new UnsupportedOperationException("Warning Low Disk Space")));
        } else {
            postEvent(() -> prepareCallback.onPrepared(assetId, assetInfo, null));
        }
    }

    private boolean isLowDiskSpace(long requiredBytes) {
        final long downloadsDirFreeSpace = downloadDirectory.getFreeSpace();
        return downloadsDirFreeSpace < requiredBytes;
    }

    private static boolean hasAnyValidRenderer(MappingTrackSelector.MappedTrackInfo mappedTrackInfo) {
        for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
            if (isValidRenderer(mappedTrackInfo, i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidRenderer(MappingTrackSelector.MappedTrackInfo mappedTrackInfo, int rendererIndex) {
        TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
        if (trackGroupArray.length == 0) {
            return false;
        }
        int trackType = mappedTrackInfo.getRendererType(rendererIndex);
        return isSupportedTrackType(trackType);
    }

    private static boolean isSupportedTrackType(int trackType) {
        switch (trackType) {
            case C.TRACK_TYPE_VIDEO:
            case C.TRACK_TYPE_AUDIO:
            case C.TRACK_TYPE_TEXT:
                return true;
            default:
                return false;
        }
    }

    private static long estimateTotalSize(DownloadHelper downloadHelper, String assetId, DownloadType downloadType, Uri uri, int hlsAudioBitrate, @NonNull PrepareCallback prepareCallback) {
        long selectedSize = 0;

        final Object manifest = downloadHelper.getManifest();

        final long durationMs;
        if (manifest instanceof DashManifest) {
            final DashManifest dashManifest = (DashManifest) manifest;
            durationMs = dashManifest.durationMs;
        } else if (manifest instanceof HlsManifest) {
            final HlsManifest hlsManifest = (HlsManifest) manifest;
            durationMs = hlsManifest.mediaPlaylist.durationUs / 1000;
        } else {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            try {
                Future<Long> length = executorService.submit(new HttpHeadGetLength(uri));
                return length.get();
            } catch (ExecutionException|InterruptedException exception) {
                prepareCallback.onPrepareError(assetId, downloadType, exception);
                return 0;
            } finally {
                executorService.shutdown();
            }
        }

        for (int pi = 0; pi < downloadHelper.getPeriodCount(); pi++) {

            final MappingTrackSelector.MappedTrackInfo mappedTrackInfo = downloadHelper.getMappedTrackInfo(pi);
            final int rendererCount = mappedTrackInfo.getRendererCount();

            for (int i = 0; i < rendererCount; i++) {
                final List<ExoTrackSelection> trackSelections = downloadHelper.getTrackSelections(pi, i);

                for (int j = 0 ; j < trackSelections.size() ; j++) {
                    ExoTrackSelection trackSelection = trackSelections.get(j);
                    if (trackSelection != null) {
                        Format format = trackSelection.getFormat(0);
                        if (format != null) {
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
            }
        }
        return selectedSize;
    }

    static long httpHeadGetLength(Uri uri) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(uri);
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("Accept-Encoding", "");
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                throw new IOException("Response code from HEAD request: " + responseCode);
            }
            String contentLength = connection.getHeaderField("Content-Length");
            if (!TextUtils.isEmpty(contentLength)) {
                return Long.parseLong(contentLength);
            } else {
                return -1;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static HttpURLConnection openConnection(Uri uri) throws IOException {
        if (uri == null) {
            return null;
        }

        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(uri.toString()).openConnection();
        return httpURLConnection;
    }

    public static ExoOfflineManager getInstance(Context context) {
        if (instance == null) {
            instance = new ExoOfflineManager(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void start(ManagerStartCallback callback) throws IOException {
        sessionId = UUID.randomUUID().toString();
        applicationName = (offlineManagerSettings != null && !TextUtils.isEmpty(offlineManagerSettings.getApplicationName())) ?
                offlineManagerSettings.getApplicationName() :
                appContext.getPackageName();

        createNoMediaFile();
        setupEventHandler();
        if (callback != null) {
            callback.onStarted();
        }
        if (downloadProgressTracker == null) {
            sendDownloadProgress();
        }
        addExoDownloadManagerListener();
    }

    private void createNoMediaFile() throws IOException {
        if (offlineManagerSettings == null || offlineManagerSettings.isCreateNoMediaFileInDownloadsDir()) {
            if (downloadDirectory == null) {
                throw new FileNotFoundException("No external files dir, can't continue");
            }
            File noMediaFile = new File(downloadDirectory, ".nomedia");
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile();
            }
        }
    }

    @Override
    public void stop() {
        removeExoDownloadManagerListener();
        removeEventHandler();
        downloadProgressTracker = null;
        if (prefetchManager != null) {
            if (prefetchManager.getPrefetchConfig().isCleanPrefetchedAssets()) {
                prefetchManager.removeAllAssets();
            }
            prefetchManager.removeEventHandler();
        }

        if (assetDownloadHelper != null) {
            assetDownloadHelper.release();
            assetDownloadHelper = null;
        }
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
    }

    @Override
    public void cancelDownloads() {
        List<AssetInfo> assetInfoList = getAssetsInState(AssetDownloadState.started);
        if (assetInfoList == null) {
            return;
        }

        for (AssetInfo assetInfo : assetInfoList) {
            if (assetInfo.getState() == AssetDownloadState.started) {
                removeAsset(assetInfo.getAssetId());
            }
        }
    }

    @Override
    public void setForegroundNotification(ExoOfflineNotificationHelper notification) {
        ExoDownloadService.setForegroundNotification(notification);
    }

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
    public List<AssetInfo> getAllAssets(DownloadType... downloadType) {
        List<OfflineManager.AssetInfo> assetInfoItems = getDownloadsFromDatabase();
        if (downloadType == null) {
            return assetInfoItems;
        }

        List<DownloadType> downloadTypeList = new ArrayList<>(Arrays.asList(downloadType));

        if (downloadTypeList.size() == 1) {
            if (downloadTypeList.contains(DownloadType.PREFETCH)) {
                return getAssetInfos(assetInfoItems, DownloadType.PREFETCH);
            } else if (downloadTypeList.contains(DownloadType.FULL)) {
                return getAssetInfos(assetInfoItems, DownloadType.FULL);
            } else {
                return Collections.emptyList();
            }
        }

        return assetInfoItems;
    }

    @NotNull
    private List<AssetInfo> getAssetInfos(List<AssetInfo> assetInfoItems, DownloadType downloadType) {
        List<AssetInfo> filteredItems = new ArrayList<>();

        for (AssetInfo assetInfoItem: assetInfoItems) {
            if (assetInfoItem.getDownloadType() == downloadType) {
                filteredItems.add(assetInfoItem);
            }
        }
        return filteredItems;
    }

    private List<AssetInfo> getDownloadsFromDatabase() {
        final DownloadCursor downloads;
        try {
            downloads = downloadManager.getDownloadIndex().getDownloads(Download.STATE_DOWNLOADING,
                    Download.STATE_QUEUED,
                    Download.STATE_RESTARTING,
                    Download.STATE_COMPLETED,
                    Download.STATE_REMOVING,
                    Download.STATE_STOPPED,
                    Download.STATE_FAILED);
        }
        catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        List<AssetInfo> assetInfoList = new ArrayList<>(downloads.getCount());
        while (downloads.moveToNext()) {
            final Download download = downloads.getDownload();
            if (download != null) {
                assetInfoList.add(new ExoAssetInfo(download));
            }
        }

        return assetInfoList;
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
            case prefetched:
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

        while (downloads.moveToNext()) {
            final Download download = downloads.getDownload();
            if (download != null) {
                assetInfoList.add(new ExoAssetInfo(download));
            }
        }

        return assetInfoList;
    }

    @NonNull
    @Override
    public PKMediaEntry getLocalPlaybackEntry(@NonNull String assetId) throws IOException {
        final Download download = downloadManager.getDownloadIndex().getDownload(assetId);
        if (download == null ||
                (download.state == Download.STATE_STOPPED && download.stopReason != StopReason.prefetchDone.toExoCode()) ||
                (download.state != Download.STATE_COMPLETED && download.state != Download.STATE_STOPPED)) {
            return new PKMediaEntry();
        }

        MediaItem localMediaItem = download.request.toMediaItem();
        MediaItem.Builder builder = localMediaItem.buildUpon();

        MediaItem.DrmConfiguration drmConfiguration = new MediaItem.DrmConfiguration
                .Builder(MediaSupport.WIDEVINE_UUID)
                .setLicenseRequestHeaders(Collections.emptyMap())
                .build();

        builder.setMediaId(download.request.id)
                .setDrmConfiguration(drmConfiguration);

        final PKMediaSource localMediaSource = lam.getLocalMediaSource(assetId, builder.build());

        if (TextUtils.isEmpty(localMediaSource.getUrl())) {
            localMediaSource.setUrl(download.request.uri.toString());
        }
        return new PKMediaEntry().setId(assetId).setSources(Collections.singletonList(localMediaSource));
    }

    @Override
    public Prefetch getPrefetchManager(@NonNull PrefetchConfig prefetchConfig) {
        if (prefetchManager == null) {
            prefetchManager = new PrefetchManager(this);
            if (prefetchConfig == null) {
                prefetchConfig = new PrefetchConfig();
            }
            if (prefetchConfig.isCleanPrefetchedAssets()) {
                prefetchManager.removeAllAssets();
            }
        }
        prefetchManager.setPrefetchConfig(prefetchConfig);
        return prefetchManager;
    }

    @Override
    public void startAssetDownload(@NonNull AssetInfo assetInfo) throws IllegalArgumentException {
        if (!(assetInfo instanceof ExoAssetInfo)) {
            throw new IllegalArgumentException("Not an ExoAssetInfo object");
        }

        final ExoAssetInfo exoAssetInfo = (ExoAssetInfo) assetInfo;
        final DownloadHelper downloadHelper = exoAssetInfo.downloadHelper;
        if (downloadHelper == null) {
            try {
                downloadManager.getDownloadIndex().getDownload(exoAssetInfo.getAssetId());
                DownloadService.sendSetStopReason(appContext, ExoDownloadService.class, exoAssetInfo.getAssetId(), 0, false);
                log.e("DownloadHelper is null");
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

        final DownloadRequest downloadRequest = downloadHelper.getDownloadRequest(assetInfo.getAssetId(), bytes);
        downloadHelper.release();

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
        log.d("removeAsset:" + assetId);
        final boolean[] removeAssetStatus = {true};

        ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
        try {
            networkExecutor.execute(() -> {
                final AssetStateListener listener = getListener();
                try {
                    AssetInfo asset = getAssetInfo(assetId);
                    if (asset == null) {
                        if (listener != null) {
                            postEvent(() -> listener.onAssetRemoveError(assetId, DownloadType.UNKNOWN, new IllegalArgumentException("AssetId: " + assetId + " not found")));
                        }
                        removeAssetStatus[0] = false;
                        return;
                    }
                    if (isDRMOfflineAsset(assetId)) {
                        final byte[] drmInitData = getDrmInitData(assetId);
                        if (drmInitData == null) {
                            String errorMsg = "Can not unregisterAsset when drmInitData == null AssetId: " + assetId;
                            log.e(errorMsg);
                            if (listener != null) {
                                postEvent(() -> listener.onUnRegisterError(assetId, asset.getDownloadType(), new IllegalArgumentException(errorMsg)));
                            }
                            removeAssetStatus[0] = false;
                        }
                        lam.unregisterAsset(assetId, drmInitData);
                        pendingDrmRegistration.remove(assetId);
                    }
                    DownloadService.sendRemoveDownload(appContext, ExoDownloadService.class, assetId, false);
                    removeAssetSourceId(assetId);

                } catch (Exception e) {
                    log.e("removeAsset failed", e);
                    if (listener != null) {
                        AssetInfo assetInfo = getAssetInfo(assetId);
                        DownloadType downloadType = (assetInfo != null) ? assetInfo.getDownloadType() : DownloadType.FULL;
                        postEvent(() -> listener.onAssetRemoveError(assetId, downloadType, e));
                    }
                    removeAssetStatus[0] = false;
                }
            });
        } finally {
            networkExecutor.shutdown();
        }

        return removeAssetStatus[0];
    }

    private boolean isDRMOfflineAsset(String assetId) throws IOException {
        PKMediaEntry mediaEntry = getLocalPlaybackEntry(assetId);

        return mediaEntry != null &&
                mediaEntry.getSources() != null &&
                mediaEntry.getSources().size() > 0 &&
                mediaEntry.getSources().get(0) instanceof LocalAssetsManagerExo.LocalExoMediaItem &&
                ((LocalAssetsManagerExo.LocalExoMediaItem) mediaEntry.getSources().get(0)).getScheme() != null;
    }

    /**
     * Gets DRM int data either from local map or Remote
     */
    @Override
    protected byte[] getDrmInitData(String assetId) throws IOException, InterruptedException {
        final Pair<PKMediaSource, Object> pair = pendingDrmRegistration.get(assetId);
        if (pair != null && pair.first != null && pair.second != null &&
                (pair.second instanceof DrmRegistrationMetaData && ((DrmRegistrationMetaData) pair.second).isRegistered())) {
            byte[] drmInitData = extractDrmInitDataFromFormat(pair.second, assetId);
            if (drmInitData != null) {
                log.d("Extracting DrmInitData from the local map");
                return drmInitData;
            }
        }

        final Download download = downloadManager.getDownloadIndex().getDownload(assetId);
        if (download == null) {
            return null;
        }

        final Uri uri = download.request.uri;
        final CacheDataSource.Factory cacheDataSourceFactory = getCacheDataSourceFactory();
        cacheDataSourceFactory.setUpstreamPriority(C.PRIORITY_DOWNLOAD);
        return getDrmInitData(cacheDataSourceFactory, uri.toString());
    }

    @Override
    protected PKMediaFormat getAssetFormat(String assetId) {
        PKMediaFormat pkMediaFormat = PKMediaFormat.unknown;
        try {
            final Download download = downloadManager.getDownloadIndex().getDownload(assetId);
            if (download == null) {
                return pkMediaFormat;
            }
            final Uri uri = download.request.uri;
            pkMediaFormat = PKMediaFormat.valueOfUrl(uri.toString());
            return pkMediaFormat != null ? pkMediaFormat : PKMediaFormat.unknown;
        } catch (IOException exception) {
            return pkMediaFormat;
        }
    }

    @Nullable
    @Override
    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    @Nullable
    @Override
    public File getDownloadDirectory() {
        return downloadDirectory;
    }

    @Nullable
    @Override
    public Cache getDownloadCache() {
        return downloadCache;
    }

    private void registerDrmAsset(String assetId, DownloadType downloadType) {
        if (assetId == null) {
            return;
        }

        final Pair<PKMediaSource, Object> pair = pendingDrmRegistration.get(assetId);
        if (pair == null || pair.first == null || pair.second == null ||
                (pair.second instanceof DrmRegistrationMetaData && ((DrmRegistrationMetaData) pair.second).isRegistered())) {
            return; // no DRM or already processed
        }

        final PKMediaSource source = pair.first;
        String licenseUri = extractDrmLicense(source.getDrmData(), PKDrmParams.Scheme.WidevineCENC);

        if (TextUtils.isEmpty(licenseUri)) {
            log.w("LicenseUri is empty. assetId: " + assetId);
            return;
        }

        final byte[] drmInitData = extractDrmInitDataFromFormat(pair.second, assetId);
        if (drmInitData == null) {
            return;
        }

        final CacheDataSource.Factory cacheDataSourceFactory = getCacheDataSourceFactory();
        cacheDataSourceFactory.setUpstreamPriority(C.PRIORITY_DOWNLOAD);
        final AssetStateListener listener = getListener();

        try {
            lam.registerWidevineAsset(assetId, getAssetFormat(assetId), licenseUri, drmInitData, forceWidevineL3Playback);
            postEvent(() -> listener.onRegistered(assetId, getDrmStatus(assetId, drmInitData)));
            ((DrmRegistrationMetaData) pair.second).setRegistered(true);
        } catch (LocalAssetsManager.RegisterException e) {
            postEvent(() -> listener.onRegisterError(assetId, downloadType, e));
        }
    }

    private String extractDrmLicense(List<PKDrmParams> drmData, PKDrmParams.Scheme scheme) {
        if (drmData != null && scheme != null) {
            for (PKDrmParams pkDrmParam : drmData) {
                if (scheme == pkDrmParam.getScheme()) {
                    return pkDrmParam.getLicenseUri();
                }
            }
        }
        return null;
    }

    private byte[] extractDrmInitDataFromFormat(Object drmRegistrationMetaData, String assetId) {
        Format format = ((DrmRegistrationMetaData) drmRegistrationMetaData).getFormat();
        if (format.drmInitData == null) {
            log.w("DrmInitData is null. assetId: " + assetId);
            return null;
        }

        DrmInitData.SchemeData schemeData = findWidevineSchemaData(format.drmInitData);
        if (schemeData == null || schemeData.data == null) {
            log.w("SchemeData is invalid. assetId: " + assetId);
            return null;
        }

        return schemeData.data;
    }

    @NonNull
    private CacheDataSource.Factory getCacheDataSourceFactory() {
        return new CacheDataSource.Factory()
                .setCache(downloadCache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    /**
     * Takes DRM Init data from the remote source
     */
    private byte[] getDrmInitData(CacheDataSource.Factory dataSourceFactory, String contentUri) throws IOException, InterruptedException {
        PKMediaFormat pkMediaFormat = PKMediaFormat.valueOfUrl(contentUri);
        if (pkMediaFormat != null &&
                pkMediaFormat != PKMediaFormat.dash &&
                pkMediaFormat != PKMediaFormat.hls) {
            return null;
        }

        if (!contentUri.contains(".mpd") && !contentUri.contains(".m3u8")) {
            return null;
        }

        final DrmInitData drmInitData = fetchDrmInitData(pkMediaFormat, contentUri, dataSourceFactory);
        if (drmInitData == null) {
            return null;
        }

        final DrmInitData.SchemeData schemeData = findWidevineSchemaData(drmInitData);
        return schemeData != null ? schemeData.data : null;
    }

    /**
     * Method to get the drm Init data from remote. It parses the manifest and gets the relevant representation
     * or segments and from there it takes the DRM initdata
     *
     * @param pkMediaFormat weather it is DASH or HLS
     * @param contentUri URI of the content (For exo case, it is always remote URI not the local URI_
     * @param dataSourceFactory CacheDataSource
     * @return DRMInitData
     * @throws IOException If there is some error
     * @throws InterruptedException if the remote thread is interrupted
     */
    @Nullable
    private DrmInitData fetchDrmInitData(PKMediaFormat pkMediaFormat, String contentUri, CacheDataSource.Factory dataSourceFactory) throws IOException, InterruptedException {
        DrmInitData drmInitData = null;
        final CacheDataSource cacheDataSource = dataSourceFactory.createDataSourceForRemovingDownload();

        if (pkMediaFormat == null) {
            return null;
        }

        switch (pkMediaFormat) {
            case dash: {
                final DashManifest dashManifest = DashUtil.loadManifest(cacheDataSource, Uri.parse(contentUri));
                Format formatWithDrmInitData = DashUtil.loadFormatWithDrmInitData(cacheDataSource, dashManifest.getPeriod(0));
                drmInitData = formatWithDrmInitData == null ? null : formatWithDrmInitData.drmInitData;
                log.d("Loading Dash drmInitData from cache");
                break;
            }
            case hls: {
                HlsPlaylist hlsPlaylist = ParsingLoadable.load(cacheDataSource,
                        new HlsPlaylistParser(),
                        Uri.parse(contentUri),
                        C.DATA_TYPE_MANIFEST);
                if (hlsPlaylist instanceof HlsMultivariantPlaylist) {
                    HlsMultivariantPlaylist hlsMasterPlaylist = (HlsMultivariantPlaylist) hlsPlaylist;

                    if (!hlsMasterPlaylist.variants.isEmpty()) {
                        int index = findDownloadedVariantIndexForHls(cacheDataSource, hlsMasterPlaylist);

                        if (index != -1) {
                            HlsPlaylist playlist = ParsingLoadable.load(cacheDataSource,
                                    new HlsPlaylistParser(),
                                    Uri.parse(hlsMasterPlaylist.variants.get(index).url.toString()),
                                    C.DATA_TYPE_MANIFEST);
                            if (playlist instanceof HlsMediaPlaylist && !((HlsMediaPlaylist) playlist).segments.isEmpty()) {
                                drmInitData = ((HlsMediaPlaylist) playlist).segments.get(0).drmInitData;
                                log.d("Loading HLS drmInitData from cache");
                            }
                        }
                    }
                }
                break;
            }
        }

        return drmInitData;
    }

    /**
     * Loop over the available variant(video tracks) in HLSMasterPlaylist
     * and checks if that variant is available in the Cache
     *
     * @param cacheDataSource datasource
     * @param hlsMasterPlaylist given master playlist
     *
     * @return index of the very first variant video track which is available in the cache
     */
    private int findDownloadedVariantIndexForHls(CacheDataSource cacheDataSource, HlsMultivariantPlaylist hlsMasterPlaylist) {
        Set<String> downloadedKeys = cacheDataSource.getCache().getKeys();

        for (int variantCount = 0; variantCount < hlsMasterPlaylist.variants.size(); variantCount++) {
            if (downloadedKeys.contains(hlsMasterPlaylist.variants.get(variantCount).url.toString())) {
                return variantCount;
            }
        }
        return -1;
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

    private static class HttpHeadGetLength implements Callable<Long> {
        private Uri uri;

        public HttpHeadGetLength(Uri uri) {
            this.uri = uri;
        }

        public Long call() throws IOException {
            return httpHeadGetLength(uri);
        }
    }
}
