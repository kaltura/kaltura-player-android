package com.kaltura.tvplayer.offline.exo;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.kaltura.android.exoplayer2.MediaItem;
import com.kaltura.android.exoplayer2.drm.DrmSession;
import com.kaltura.android.exoplayer2.drm.DrmSessionEventListener;
import com.kaltura.android.exoplayer2.drm.OfflineLicenseHelper;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
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
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.TrackSelection;
import com.kaltura.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.android.exoplayer2.upstream.cache.CacheDataSource;
import com.kaltura.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.kaltura.android.exoplayer2.upstream.cache.SimpleCache;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.player.PKHttpClientManager;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.playkit.utils.NativeCookieJarBridge;
import com.kaltura.tvplayer.offline.AbstractOfflineManager;
import com.kaltura.playkit.drm.DeferredDrmSessionManager;
import com.kaltura.playkit.drm.DrmCallback;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


// NOTE: this and related classes are not currently in use. OfflineManager.getInstance() always
// returns an instance of DTGOfflineManager. ExoOfflineManager will be used in a future version.

public class ExoOfflineManager extends AbstractOfflineManager {

    private static final PKLog log = PKLog.get("ExoOfflineManager");
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private static long REQUIRED_DATA_PARTITION_SIZE_BYTES = 200 * 1024 * 1024;

    private static final int THREAD_POOL_SIZE = 8;
    private static final int MAX_PARALLEL_DOWNLOADS = 4;
    private static final int MIN_RETRY_COUNT = 5;
    private static final int REGISTER_ASSET_AFTER_5_SEC = 5000;
    private static final int REGISTER_ASSET_NOW = 0;

    private static Gson gson = new Gson();

    private static ExoOfflineManager instance;
    @Nullable private byte[] keySetId;

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

    private DeferredDrmSessionManager deferredDrmSessionManager;

    @SuppressWarnings("FieldCanBeLocal")
    private final DownloadManager.Listener exoListener = new DownloadManager.Listener() {
        @Override
        public void onInitialized(DownloadManager downloadManager) {

        }

        @Override
        public void onDownloadsPausedChanged(DownloadManager downloadManager, boolean downloadsPaused) {
            log.d("onDownloadsPausedChanged: " + downloadsPaused);
        }

        @Override
        public void onDownloadChanged(DownloadManager downloadManager, Download download, @Nullable Exception finalException) {
            final String assetId = download.request.id;
            final AssetStateListener listener = getListener();

            switch (download.state) {
                case Download.STATE_COMPLETED:
                    log.d("STATE_COMPLETED: " + assetId);
                    maybeRegisterDrmAsset(assetId, REGISTER_ASSET_NOW);
                    listener.onAssetDownloadComplete(assetId);
                    break;
                case Download.STATE_DOWNLOADING:
                    log.d("STATE_DOWNLOADING: " + assetId);
                    maybeRegisterDrmAsset(assetId, REGISTER_ASSET_AFTER_5_SEC);
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
                    // TODO: 2019-09-04 what does it mean?
                    break;
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

        @Override
        public void onWaitingForRequirementsChanged(DownloadManager downloadManager, boolean waitingForRequirements) {

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

        downloadManager = new DownloadManager(
                context,
                databaseProvider,
                downloadCache,
                httpDataSourceFactory,
                Executors.newFixedThreadPool(/* nThreads= */ THREAD_POOL_SIZE)
        );

        downloadManager.setMaxParallelDownloads(MAX_PARALLEL_DOWNLOADS);
        downloadManager.setRequirements(new Requirements(Requirements.NETWORK|Requirements.DEVICE_STORAGE_NOT_LOW));
        downloadManager.setMinRetryCount(MIN_RETRY_COUNT);
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

    private List<MediaItem.Subtitle> buildSubtitlesList(List<PKExternalSubtitle> externalSubtitleList) {
        List<MediaItem.Subtitle> subtitleList = new ArrayList<>();

        if (externalSubtitleList != null && externalSubtitleList.size() > 0) {
            for (int subtitlePosition = 0 ; subtitlePosition < externalSubtitleList.size() ; subtitlePosition ++) {
                PKExternalSubtitle pkExternalSubtitle = externalSubtitleList.get(subtitlePosition);
                String subtitleMimeType = pkExternalSubtitle.getMimeType() == null ? "Unknown" : pkExternalSubtitle.getMimeType();
                MediaItem.Subtitle subtitleMediaItem = new MediaItem.Subtitle(Uri.parse(pkExternalSubtitle.getUrl()), subtitleMimeType, pkExternalSubtitle.getLanguage() + "-" + subtitleMimeType, pkExternalSubtitle.getLabel(), pkExternalSubtitle.getSelectionFlags());
                subtitleList.add(subtitleMediaItem);
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
    private Format getFirstFormatWithDrmInitData(DownloadHelper helper) {
        for (int periodIndex = 0; periodIndex < helper.getPeriodCount(); periodIndex++) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = helper.getMappedTrackInfo(periodIndex);
            for (int rendererIndex = 0;
                 rendererIndex < mappedTrackInfo.getRendererCount();
                 rendererIndex++) {
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
        deferredDrmSessionManager = null;
        final String assetId = mediaEntry.getId();
        final PKMediaSource source = selector.getSelectedSource();
        if (source == null) {
            return;
        }

        final PKDrmParams drmData = selector.getSelectedDrmParams();
        final PKMediaFormat mediaFormat = source.getMediaFormat();
        final String url = source.getUrl();
        final Uri uri = Uri.parse(url);

        final DownloadHelper downloadHelper;
        postEvent(() -> prepareCallback.onSourceSelected(assetId, source, drmData));
        DefaultTrackSelector.Parameters defaultTrackSelectorParameters =  DownloadHelper.getDefaultTrackSelectorParameters(appContext);

        MediaItem.Builder builder =
                new MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(mediaFormat.mimeType)
                        .setSubtitles(buildSubtitlesList(mediaEntry.getExternalSubtitleList()))
                        .setClipStartPositionMs(0L)
                        .setClipEndPositionMs(C.TIME_END_OF_SOURCE);

        if (mediaFormat == PKMediaFormat.dash) {
            if (drmData != null) {
                boolean setDrmSessionForClearTypes = false;
                // selecting WidevineCENC as default right now
                PKDrmParams.Scheme scheme = PKDrmParams.Scheme.WidevineCENC;
                String licenseUri = getDrmLicenseUrl(source, scheme);

                if (licenseUri != null) {
                    PKRequestParams licenseRequestParams = new PKRequestParams(Uri.parse(licenseUri), new HashMap<>());

                    //if (getLicenseRequestAdapter() != null) {
                    //    licenseRequestParams = getLicenseRequestAdapter().adapt(licenseRequestParams);
                    //}

                    Map<String, String> licenseRequestParamsHeaders = licenseRequestParams.headers;

                    builder
                            .setDrmUuid((scheme == PKDrmParams.Scheme.WidevineCENC) ? MediaSupport.WIDEVINE_UUID : MediaSupport.PLAYREADY_UUID)
                            .setDrmLicenseUri(licenseUri)
                            .setDrmMultiSession(false)
                            .setDrmForceDefaultLicenseUri(false)
                            .setDrmLicenseRequestHeaders(licenseRequestParamsHeaders);
                }
            }
        }
        MediaItem mediaItem = builder.build();
        if (mediaFormat != PKMediaFormat.dash) {
            downloadHelper = DownloadHelper.forMediaItem(
                    appContext, mediaItem, new DefaultRenderersFactory(appContext), httpDataSourceFactory);
        } else if (drmData != null && drmData.getScheme() != null) {
            final DrmCallback drmCallback = new DrmCallback(httpDataSourceFactory, null); // TODO license adapter usage?
            //drmSessionManager = buildDrmSessionManager(drmData);
            deferredDrmSessionManager = new DeferredDrmSessionManager(new Handler(Looper.getMainLooper()), drmCallback, error -> {
                log.e("XXX onPrepareError drm call failed");
                postEvent(() -> prepareCallback.onPrepareError(assetId, new IllegalArgumentException("XXX drm call failed")));
            }, true);
            deferredDrmSessionManager.setMediaSource(source);
            downloadHelper =  DownloadHelper.forMediaItem(mediaItem, defaultTrackSelectorParameters , new DefaultRenderersFactory(appContext), httpDataSourceFactory, deferredDrmSessionManager);
        } else {
            downloadHelper = DownloadHelper.forMediaItem(mediaItem, defaultTrackSelectorParameters , new DefaultRenderersFactory(appContext), httpDataSourceFactory, DrmSessionManager.getDummyDrmSessionManager());

        }

        downloadHelper.prepare(new DownloadHelper.Callback() {
            @Override
            public void onPrepared(DownloadHelper helper) {
                @Nullable Format format = getFirstFormatWithDrmInitData(helper);

                if (mediaItem.playbackProperties.drmConfiguration != null && mediaItem.playbackProperties.drmConfiguration.licenseUri != null) {
                    WidevineOfflineLicenseFetchTask widevineOfflineLicenseFetchTask = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        widevineOfflineLicenseFetchTask = new WidevineOfflineLicenseFetchTask(
                                format,
                                mediaItem.playbackProperties.drmConfiguration.licenseUri,
                                httpDataSourceFactory,
                                helper);
                    }
                    if (widevineOfflineLicenseFetchTask != null) {
                        widevineOfflineLicenseFetchTask.execute();
                    }
                }


                if (selectionPrefs.videoBitrate == null && selectionPrefs.allAudioLanguages && selectionPrefs.allTextLanguages) {
                    downloadAllTracks(helper, downloadHelper, selectionPrefs);
                } else {
                    downloadAllVideoTracks(helper, downloadHelper, selectionPrefs);
                }

                long selectedSize = estimateTotalSize(helper, estimatedHlsAudioBitrate);
                final ExoAssetInfo assetInfo = new ExoAssetInfo(assetId, AssetDownloadState.none, selectedSize, -1, helper);
                if (mediaFormat == PKMediaFormat.dash && drmData != null) {
                    pendingDrmRegistration.put(assetId, new Pair<>(source, drmData));
                }

                saveAssetSourceId(assetId, source.getId());
                if (isLowDiskSpace(assetInfo.getEstimatedSize())) {
                    postEvent(() -> prepareCallback.onPrepareError(assetId, new UnsupportedOperationException("Warning Low Disk Space")));
                } else {
                    postEvent(() -> prepareCallback.onPrepared(assetId, assetInfo, null));
                }
            }

            @Override
            public void onPrepareError(DownloadHelper helper, IOException error) {
                if (error != null) {
                    log.e("onPrepareError: " + error.getCause());
                }
                if (helper != null) {
                    helper.release();
                }
                postEvent(() -> prepareCallback.onPrepareError(assetId, error));
            }
        });
    }

    private boolean isLowDiskSpace(long requiredBytes) {
        final long downloadsDirFreeSpace = downloadDirectory.getFreeSpace();
        return downloadsDirFreeSpace < requiredBytes;
    }

    private void downloadAllTracks(DownloadHelper helper, DownloadHelper downloadHelper, @NonNull SelectionPrefs selectionPrefs) {

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = helper.getMappedTrackInfo(0);
        for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
            //downloadHelper.clearTrackSelections(periodIndex);
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
                            buildExoParameters(selectionPrefs),
                            selectionOverrides);
                }
            }
        }
    }

    private void downloadAllVideoTracks(DownloadHelper helper, DownloadHelper downloadHelper, @NonNull SelectionPrefs selectionPrefs) {
        downloadAllTracks(Consts.TRACK_TYPE_VIDEO, helper, downloadHelper, selectionPrefs);
    }

    private void downloadAllAudioTracks(DownloadHelper helper, DownloadHelper downloadHelper, @NonNull SelectionPrefs selectionPrefs) {
        downloadAllTracks(Consts.TRACK_TYPE_AUDIO, helper, downloadHelper, selectionPrefs);
    }

    private void downloadAllTextTracks(DownloadHelper helper, DownloadHelper downloadHelper, @NonNull SelectionPrefs selectionPrefs) {
        downloadAllTracks(Consts.TRACK_TYPE_TEXT, helper, downloadHelper, selectionPrefs);
    }

    private void downloadAllTracks(int trackType, DownloadHelper helper, DownloadHelper downloadHelper, @NonNull SelectionPrefs selectionPrefs) {

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = helper.getMappedTrackInfo(0);
        for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
            //downloadHelper.clearTrackSelections(periodIndex);
            int rendererIndex = trackType;
            List<DefaultTrackSelector.SelectionOverride> selectionOverrides = new ArrayList<>();
            TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);

            for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {
                //run through the all tracks in current trackGroup.
                TrackGroup trackGroup = trackGroupArray.get(groupIndex);

                for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                    Format format = trackGroup.getFormat(trackIndex);
                    if (Consts.TRACK_TYPE_VIDEO == trackType && selectionPrefs != null && selectionPrefs.videoBitrate != null && format.bitrate > selectionPrefs.videoBitrate) {
                        if (format.bitrate > selectionPrefs.videoBitrate) {
                            continue;
                        }
                    }
                    selectionOverrides.add(new DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex));
                }

                downloadHelper.addTrackSelectionForSingleRenderer(
                        periodIndex,
                        rendererIndex,
                        buildExoParameters(selectionPrefs),
                        selectionOverrides);
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


    private DefaultTrackSelector.Parameters buildExoParameters(SelectionPrefs selectionPrefs) {
        return DefaultTrackSelector.Parameters.getDefaults(appContext);   // TODO: 2019-07-31
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
    }

    @Override
    public void resumeDownloads() {
        // Resume all non prefetch downloads.

        List<Download> downloads = downloadManager.getCurrentDownloads();
        for (Download download : downloads) {
            if (download.state == Download.STATE_STOPPED && download.state == Download.STATE_QUEUED) {
                resumeAssetDownload(download.request.id);
            }
        }
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

    @NonNull
    @Override
    public PKMediaEntry getLocalPlaybackEntry(@NonNull String assetId) throws IOException {

        final Download download = downloadManager.getDownloadIndex().getDownload(assetId);

        if (download == null ||
                (download.state != Download.STATE_COMPLETED && download.state != Download.STATE_STOPPED)) {
            return new PKMediaEntry();
        }

        MediaItem localMediaItem = download.request.toMediaItem();
        MediaItem.Builder builder = localMediaItem.buildUpon();
        builder
                .setMediaId(download.request.id)
                .setUri(download.request.uri)
                .setCustomCacheKey(download.request.customCacheKey)
                .setMimeType(download.request.mimeType)
                .setStreamKeys(download.request.streamKeys)
                .setDrmKeySetId(keySetId);

        final PKMediaSource localMediaSource = lam.getLocalMediaSource(assetId, builder.build());


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
        bytes = data.toString().getBytes();

        final DownloadRequest downloadRequest = helper.getDownloadRequest(assetInfo.getAssetId(), bytes);
        downloadRequest.copyWithKeySetId(keySetId);

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
        log.e("removeAsset:" + assetId);

        final AssetStateListener listener = getListener();
        try {
            AssetInfo asset = getAssetInfo(assetId);
            if (asset == null) {
                return false;
            }

            final Pair<PKMediaSource, PKDrmParams> pair = pendingDrmRegistration.get(assetId);
            boolean isDRM = true;
            if (pair == null || pair.first == null || pair.second == null) {
                isDRM = false;
            }
            if (isDRM) {
                final byte[] drmInitData = getDrmInitData(assetId);
                if (drmInitData == null) {
                    log.e("removeAsset failed drmInitData == null");
                    return false;
                }
                lam.unregisterAsset(assetId, drmInitData);
            }

            DownloadService.sendRemoveDownload(appContext, ExoDownloadService.class, assetId, false);
            removeAssetSourceId(assetId);

        } catch (IOException | InterruptedException e) {
            log.e("removeAsset failed", e);
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
        final CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory().setCache(downloadCache).setUpstreamDataSourceFactory(httpDataSourceFactory);
        cacheDataSourceFactory.setUpstreamPriority(C.PRIORITY_DOWNLOAD);
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

        final CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory().setCache(downloadCache).setUpstreamDataSourceFactory(httpDataSourceFactory);
        cacheDataSourceFactory.setUpstreamPriority(C.PRIORITY_DOWNLOAD);
        final AssetStateListener listener = getListener();

        final byte[] drmInitData;
        try {
            drmInitData = getDrmInitData(cacheDataSourceFactory, sourceUrl);
            lam.registerWidevineDashAsset(assetId, licenseUri, drmInitData);
            postEvent(() -> listener.onRegistered(assetId, getDrmStatus(assetId, drmInitData)));

            pendingDrmRegistration.remove(assetId);

        } catch (IOException | InterruptedException e) {
            postEvent(() -> listener.onRegisterError(assetId, e));

        } catch (LocalAssetsManager.RegisterException e) {
            postEvent(() -> listener.onRegisterError(assetId, e));
        }
    }

    static class RenewParams {
        String licenseUri;
        byte[] drmInitData;
        String sourceId;

        public RenewParams(String licenseUri, byte[] drmInitData, String sourceId) {
            this.licenseUri = licenseUri;
            this.drmInitData = drmInitData;
            this.sourceId = sourceId;
        }
    }

    private byte[] getDrmInitData(CacheDataSource.Factory dataSourceFactory, String contentUri) throws IOException, InterruptedException {
        if (PKMediaFormat.valueOfUrl(contentUri) != PKMediaFormat.dash ) {
            return null;
        }

        if (!contentUri.contains(".mpd")) {
            return null;
        }
        final CacheDataSource cacheDataSource = dataSourceFactory.createDataSource();
        final DashManifest dashManifest = DashUtil.loadManifest(cacheDataSource, Uri.parse(contentUri));
        Format formatWithDrmInitData = DashUtil.loadFormatWithDrmInitData(cacheDataSource, dashManifest.getPeriod(0));
        final DrmInitData drmInitData = formatWithDrmInitData.drmInitData;
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

    @RequiresApi(18)
    private final class WidevineOfflineLicenseFetchTask extends AsyncTask<Void, Void, Void> {

        private final Format format;
        private final Uri licenseUri;
        private final HttpDataSource.Factory httpDataSourceFactory;
        private final DownloadHelper downloadHelper;

        //@Nullable private byte[] keySetId;
        @Nullable private DrmSession.DrmSessionException drmSessionException;

        public WidevineOfflineLicenseFetchTask(
                Format format,
                Uri licenseUri,
                HttpDataSource.Factory httpDataSourceFactory,
                DownloadHelper downloadHelper) {
            this.format = format;
            this.licenseUri = licenseUri;
            this.httpDataSourceFactory = httpDataSourceFactory;
            this.downloadHelper = downloadHelper;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            OfflineLicenseHelper offlineLicenseHelper =
                    OfflineLicenseHelper.newWidevineInstance(
                            licenseUri.toString(),
                            httpDataSourceFactory,
                            new DrmSessionEventListener.EventDispatcher());
            try {
                keySetId = offlineLicenseHelper.downloadLicense(format);
            } catch (DrmSession.DrmSessionException e) {
                drmSessionException = e;
            } finally {
                offlineLicenseHelper.release();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (drmSessionException != null) {
                //dialogHelper.onOfflineLicenseFetchedError(drmSessionException);
            } else {
                //dialogHelper.onOfflineLicenseFetched(downloadHelper, checkStateNotNull(keySetId));
            }
        }
    }
}