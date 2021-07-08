package com.kaltura.tvplayer.offline.dtg;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.offline.DownloadHelper;
import com.kaltura.android.exoplayer2.source.TrackGroup;
import com.kaltura.android.exoplayer2.source.TrackGroupArray;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.dtg.ContentManager;
import com.kaltura.dtg.DownloadItem;
import com.kaltura.dtg.DownloadItem.TrackSelector;
import com.kaltura.dtg.DownloadState;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.drm.SimpleDashParser;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.tvplayer.offline.AbstractOfflineManager;
import com.kaltura.tvplayer.offline.Prefetch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.kaltura.dtg.DownloadState.INFO_LOADED;


public class DTGOfflineManager extends AbstractOfflineManager {
    private static final PKLog log = PKLog.get("DTGOfflineManager");

    private static DTGOfflineManager instance;
    private final ContentManager cm;

    private final DTGListener dtgListener = new DTGListener() {
        @Override
        public void onDownloadComplete(DownloadItem item) {
            final String assetId = item.getItemId();
            postEvent(() -> getListener().onAssetDownloadComplete(assetId, DownloadType.FULL));
            registerDrmAsset(assetId, false);
        }

        @Override
        public void onProgressChange(DownloadItem item, long downloadedBytes) {
            if (downloadProgressListener == null) {
                return;
            }

            final long total = item.getEstimatedSizeBytes();
            final float percentDownloaded = total > 0 ? Math.min(downloadedBytes * 100f / total, 100f) : 0;

            postEvent(() -> downloadProgressListener.onDownloadProgress(item.getItemId(), downloadedBytes, total, percentDownloaded));
        }

        @Override
        public void onDownloadStart(DownloadItem item) {
            final String assetId = item.getItemId();

            postEvent(() -> getListener().onStateChanged(assetId, DownloadType.FULL, new DTGAssetInfo(item, AssetDownloadState.started)));

            postEventDelayed(() -> registerDrmAsset(assetId, true), 4000);
        }

        @Override
        public void onDownloadPause(DownloadItem item) {
            postEvent(() -> getListener().onAssetDownloadPaused(item.getItemId(), DownloadType.FULL));
        }

        @Override
        public void onDownloadFailure(DownloadItem item, Exception error) {
            postEvent(() -> getListener().onAssetDownloadFailed(item.getItemId(), DownloadType.FULL, error));
        }
    };

    public static @NonNull DTGOfflineManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DTGOfflineManager.class) {
                if (instance == null) {
                    instance = new DTGOfflineManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private DTGOfflineManager(Context context) {
        super(context);

        cm = ContentManager.getInstance(context);
    }

    @Override
    public void start(ManagerStartCallback callback) throws IOException {

        if (offlineManagerSettings != null) {
            cm.getSettings().maxDownloadRetries = offlineManagerSettings.getMaxDownloadRetries();
            cm.getSettings().httpTimeoutMillis = offlineManagerSettings.getHttpTimeoutMillis();
            cm.getSettings().maxConcurrentDownloads = offlineManagerSettings.getMaxConcurrentDownloads();
            cm.getSettings().applicationName = offlineManagerSettings.getApplicationName();
            cm.getSettings().createNoMediaFileInDownloadsDir = offlineManagerSettings.isCreateNoMediaFileInDownloadsDir();
            cm.getSettings().defaultHlsAudioBitrateEstimation = offlineManagerSettings.getHlsAudioBitrateEstimation();
            cm.getSettings().freeDiskSpaceRequiredBytes = offlineManagerSettings.getFreeDiskSpaceRequiredBytes();
            cm.getSettings().downloadRequestAdapter = offlineManagerSettings.getDownloadRequestAdapter();
            cm.getSettings().chunksUrlAdapter = offlineManagerSettings.getChunksUrlAdapter();
            cm.getSettings().crossProtocolRedirectEnabled = offlineManagerSettings.isCrossProtocolRedirectEnabled();
        }

        cm.addDownloadStateListener(dtgListener);
        cm.start(() -> {
            log.d("Started DTG");
            postEvent(() -> {
                if (callback != null) {
                    callback.onStarted();
                }
            });
        });
    }

    @Override
    public void stop() {
        cm.stop();
        removeEventHandler();
    }

    @Override
    public void pauseDownloads() {
        cm.pauseDownloads();
    }

    @Override
    public void resumeDownloads() {
        cm.resumeDownloads();
    }

    @Override
    public void cancelDownloads() {
        List<DownloadItem> downloadItemsInProgress = cm.getDownloads(DownloadState.IN_PROGRESS);
        if (downloadItemsInProgress == null) {
            return;
        }

        for (DownloadItem downloadItem : downloadItemsInProgress) {
            removeAsset(downloadItem.getItemId());
        }
    }

    @Override
    public void prepareAsset(@NonNull PKMediaEntry mediaEntry, @NonNull SelectionPrefs prefs, @NonNull PrepareCallback prepareCallback) {
        SourceSelector selector = new SourceSelector(mediaEntry, preferredMediaFormat);

        final String assetId = mediaEntry.getId();
        final PKMediaSource source = selector.getSelectedSource();
        final PKDrmParams drmData = selector.getSelectedDrmParams();

        if (source == null) {
            postEvent(() -> prepareCallback.onPrepareError(assetId, new IllegalArgumentException("No playable source found")));
            return;
        }

        final String url = source.getUrl();

        postEvent(() -> prepareCallback.onSourceSelected(assetId, source, drmData));

        DownloadItem dtgItem;
        try {
            dtgItem = cm.createItem(assetId, url);
            if (dtgItem == null) {
                // this means the item already exists -- remove it.
                cm.removeItem(assetId);
                dtgItem = cm.createItem(assetId, url);
            }
        } catch (IOException e) {
            postEvent(() -> prepareCallback.onPrepareError(assetId, e));
            return;
        }

        if (dtgItem == null) {
            postEvent(() -> prepareCallback.onPrepareError(assetId, new Exception("Unknown failure adding asset")));
            return;
        }

        final DTGListener listener = new DTGListener() {
            @Override
            public void onDownloadMetadata(DownloadItem item, Exception error) {
                if (!TextUtils.equals(item.getItemId(), assetId))  {
                    postEvent(() -> prepareCallback.onPrepareError(assetId, error));
                    return; // wrong item - could be a matter of timing
                }

                if (error != null) {
                    postEvent(() -> prepareCallback.onPrepareError(assetId, error));
                } else {
                    postEvent(() -> prepareCallback.onPrepared(assetId, new DTGAssetInfo(item, AssetDownloadState.prepared), null));
                    pendingDrmRegistration.put(assetId, new Pair<>(source, drmData));
                    if (drmData != null) {
                        saveAssetPkDrmParams(assetId, drmData);
                    }
                }
                cm.removeDownloadStateListener(this);
            }

            @Override
            public void onTracksAvailable(DownloadItem item, TrackSelector trackSelector) {
                if (!TextUtils.equals(item.getItemId(), assetId)) {
                    return; // wrong item - could be a matter of timing
                }

                DTGTrackSelectionKt.selectTracks(item.getAssetFormat(), trackSelector, prefs);
            }
        };

        cm.addDownloadStateListener(listener);

        dtgItem.loadMetadata();
    }

    private void downloadAllTracks(DownloadHelper helper, DownloadHelper downloadHelper, @NonNull SelectionPrefs selectionPrefs) {

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
                            buildExoParameters(selectionPrefs),
                            selectionOverrides);
                }
            }
        }
    }

    private DefaultTrackSelector.Parameters buildExoParameters(SelectionPrefs selectionPrefs) {
        //MappingTrackSelector.MappedTrackInfo mappedTrackInfo = downloadHelper.getMappedTrackInfo(/* periodIndex= */ 0);
        //return new DefaultTrackSelector.ParametersBuilder(appContext).setMaxVideoSizeSd().build();
        return DefaultTrackSelector.Parameters.getDefaults(appContext);   // TODO: 2019-07-31
        //return new DefaultTrackSelector.ParametersBuilder(appContext).build();
        //return DownloadHelper.getDefaultTrackSelectorParameters(appContext);
        //return  DefaultTrackSelector.Parameters.DEFAULT_WITHOUT_CONTEXT.buildUpon().setForceHighestSupportedBitrate(true).build();
    }

    @Override
    protected byte[] getDrmInitData(String assetId) throws IOException {
        final File localFile = cm.getLocalFile(assetId);
        if (localFile == null) {
            return null;
        }
        return getWidevineInitData(localFile);
    }

    private void registerDrmAsset(String assetId, boolean allowFileNotFound) {
        if (assetId == null) {
            return;
        }

        final PKDrmParams drmData;

        final Pair<PKMediaSource, PKDrmParams> pair = pendingDrmRegistration.get(assetId);
        if (pair == null || pair.first == null || pair.second == null) {
            PKDrmParams pkDrmParams = loadAssetPkDrmParams(assetId);
            if (pkDrmParams == null) {
                // not a DRM media or already processed
                return;
            } else {
                drmData = pkDrmParams;
            }
        } else {
            drmData = pair.second;
        }

        final String licenseUri = drmData.getLicenseUri();

        final File localFile = cm.getLocalFile(assetId);

        if (localFile == null) {
            log.w("registerDrmAsset: Asset was removed before register: " + assetId);
            return;
        }

        if (!localFile.canRead()) {
            if (allowFileNotFound) {
                log.w("registerDrmAsset: file not found (non-fatal)");
            } else {
                log.e("registerDrmAsset: file not found");
                postEvent(() -> getListener().onRegisterError(assetId, DownloadType.FULL, new FileNotFoundException(localFile.getAbsolutePath())));
            }

            return;
        }

        try {
            final byte[] widevineInitData = getWidevineInitData(localFile);
            lam.registerWidevineDashAsset(assetId, licenseUri, widevineInitData, forceWidevineL3Playback);
            postEvent(() -> getListener().onRegistered(assetId, getDrmStatus(assetId, widevineInitData)));

            pendingDrmRegistration.remove(assetId);
            removeAssetPkDrmParams(assetId);

        } catch (IOException | LocalAssetsManager.RegisterException e) {
            postEvent(() -> getListener().onRegisterError(assetId, DownloadType.FULL, e));
        }
    }

    private byte[] getWidevineInitData(File localFile) throws IOException {
        SimpleDashParser dashParser = new SimpleDashParser().parse(localFile.getAbsolutePath());
        return dashParser.getWidevineInitData();
    }

    private @Nullable byte[] getWidevineInitDataOrNull(File localFile) {
        try {
            return getWidevineInitData(localFile);
        } catch (IOException e) {
            log.w("No Widevine init data -- not a DRM asset");
            return null;
        }
    }

    @Override
    public void startAssetDownload(@NonNull AssetInfo assetInfo) {

        if (!(assetInfo instanceof DTGAssetInfo)) {
            throw new IllegalArgumentException("Not a DTGAssetInfo object");
        }

        final DTGAssetInfo dtgAssetInfo = (DTGAssetInfo) assetInfo;
        dtgAssetInfo.downloadItem.startDownload();
    }

    @Override
    public void pauseAssetDownload(@NonNull String assetId) {
        cm.findItem(assetId).pauseDownload();
    }

    @Override
    public void resumeAssetDownload(@NonNull String assetId) {
        cm.findItem(assetId).startDownload();
    }

    @Override
    public boolean removeAsset(@NonNull String assetId) {
        final File localFile = cm.getLocalFile(assetId);
        if (localFile == null) {
            log.e("removeAsset: asset not found");
            postEvent(() -> getListener().onAssetRemoveError(assetId, DownloadType.FULL, new IllegalArgumentException("AssetId: " + assetId + " not found")));
            return false;
        }

        final byte[] drmInitData = getWidevineInitDataOrNull(localFile);
        lam.unregisterAsset(assetId, drmInitData);
        cm.removeItem(assetId);
        removeAssetSourceId(assetId);
        removeAssetPkDrmParams(assetId);
        return true;
    }

    @Nullable
    @Override
    public AssetInfo getAssetInfo(@NonNull String assetId) {
        final DownloadItem item = cm.findItem(assetId);
        if (item == null) {
            return null;
        }
        return new DTGAssetInfo(item, null);
    }

    @Nullable
    @Override
    public File getDownloadDirectory() {
        return null;
    }

    @Nullable
    @Override
    public Cache getDownloadCache() {
        return null;
    }

    @Nullable
    @Override
    public DatabaseProvider getDatabaseProvider() {
        return null;
    }

    @NonNull
    @Override
    public List<AssetInfo> getAllAssets() {
        final List<DownloadItem> downloads = cm.getDownloads(DownloadState.NEW,
                DownloadState.INFO_LOADED,
                DownloadState.IN_PROGRESS,
                DownloadState.PAUSED,
                DownloadState.COMPLETED,
                DownloadState.FAILED);
        final ArrayList<AssetInfo> assetInfoList = new ArrayList<>(downloads.size());

        AssetDownloadState downloadState;
        for (DownloadItem item : downloads) {
            switch (item.getState()) {
                case IN_PROGRESS:
                    downloadState = AssetDownloadState.started;
                    break;
                case INFO_LOADED:
                    downloadState = AssetDownloadState.prepared;
                    break;
                case COMPLETED:
                    downloadState = AssetDownloadState.completed;
                    break;
                case FAILED:
                    downloadState = AssetDownloadState.failed;
                    break;
                case PAUSED:
                    downloadState = AssetDownloadState.paused;
                    break;
                default:
                    downloadState = AssetDownloadState.none;
            }
            assetInfoList.add(new DTGAssetInfo(item, downloadState));
        }

        return assetInfoList;
    }

    @NonNull
    @Override
    public List<AssetInfo> getAssetsInState(@NonNull AssetDownloadState state) {
        DownloadState dtgState;
        switch (state) {
            case started:
                dtgState = DownloadState.IN_PROGRESS;
                break;
            case prepared:
                dtgState = INFO_LOADED;
                break;
            case completed:
                dtgState = DownloadState.COMPLETED;
                break;
            case failed:
                dtgState = DownloadState.FAILED;
                break;
            case paused:
                dtgState = DownloadState.PAUSED;
                break;

            case none:
            case removing:
            default:
                return Collections.emptyList();
        }

        final List<DownloadItem> downloads = cm.getDownloads(dtgState);

        final ArrayList<AssetInfo> assetInfoList = new ArrayList<>(downloads.size());

        for (DownloadItem item : downloads) {
            assetInfoList.add(new DTGAssetInfo(item, state));
        }

        return assetInfoList;
    }

    @NonNull
    @Override
    public PKMediaEntry getLocalPlaybackEntry(@NonNull String assetId) {
        final String playbackURL = cm.getPlaybackURL(assetId);
        final PKMediaSource localMediaSource = lam.getLocalMediaSource(assetId, playbackURL);
        return new PKMediaEntry().setId(assetId).setSources(Collections.singletonList(localMediaSource));
    }

    @Override
    public Prefetch getPrefetchManager() {
        throw new RuntimeException("Feature not supported. To use prefetch use EXO as OfflineProvider");
    }
}
