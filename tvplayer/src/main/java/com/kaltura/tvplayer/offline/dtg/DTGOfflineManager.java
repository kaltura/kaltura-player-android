package com.kaltura.tvplayer.offline.dtg;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.kaltura.dtg.ContentManager;
import com.kaltura.dtg.DownloadItem;
import com.kaltura.dtg.DownloadItem.TrackSelector;
import com.kaltura.dtg.DownloadState;
import com.kaltura.dtg.exoparser.util.Predicate;
import com.kaltura.playkit.*;
import com.kaltura.playkit.drm.SimpleDashParser;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.tvplayer.offline.AbstractOfflineManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class DTGOfflineManager extends AbstractOfflineManager {
    private static final PKLog log = PKLog.get("DTGOfflineManager");

    private static DTGOfflineManager instance;
    private final ContentManager cm;

    private final DTGListener dtgListener = new DTGListener() {
        @Override
        public void onDownloadComplete(DownloadItem item) {
            final String assetId = item.getItemId();
            postEvent(() -> getListener().onAssetDownloadComplete(assetId));

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

            postEvent(() -> getListener().onStateChanged(assetId, new DTGAssetInfo(item, AssetDownloadState.started)));

            postEventDelayed(() -> registerDrmAsset(assetId, true), 10000);
        }

        @Override
        public void onDownloadPause(DownloadItem item) {
            postEvent(() -> getListener().onAssetDownloadPaused(item.getItemId()));
        }

        @Override
        public void onDownloadFailure(DownloadItem item, Exception error) {
            postEvent(() -> getListener().onAssetDownloadFailed(item.getItemId(), error));
        }
    };

    public static DTGOfflineManager getInstance(Context context) {
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
    public void prepareAsset(PKMediaEntry mediaEntry, SelectionPrefs prefs, PrepareCallback prepareCallback) {
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
                if (!TextUtils.equals(item.getItemId(), assetId)) {
                    return; // wrong item - could be a matter of timing
                }

                if (error != null) {
                    postEvent(() -> prepareCallback.onPrepareError(assetId, error));
                } else {
                    postEvent(() -> prepareCallback.onPrepared(assetId, new DTGAssetInfo(item, AssetDownloadState.prepared), null));
                    pendingDrmRegistration.put(assetId, new Pair<>(source, drmData));
                }
                cm.removeDownloadStateListener(this);
            }

            @Override
            public void onTracksAvailable(DownloadItem item, TrackSelector trackSelector) {
                if (!TextUtils.equals(item.getItemId(), assetId)) {
                    return; // wrong item - could be a matter of timing
                }

                DTGTrackSelectionKt.selectTracks(trackSelector, prefs);
            }
        };

        cm.addDownloadStateListener(listener);

        dtgItem.loadMetadata();
    }

    @Override
    protected byte[] getDrmInitData(String assetId) throws IOException {
        final File localFile = cm.getLocalFile(assetId);
        return getWidevineInitData(localFile);
    }

    private void registerDrmAsset(String assetId, boolean allowFileNotFound) {
        if (assetId == null) {
            return;
        }

        final Pair<PKMediaSource, PKDrmParams> pair = pendingDrmRegistration.get(assetId);
        if (pair == null || pair.first == null || pair.second == null) {
            return; // no DRM or already processed
        }

        final PKDrmParams drmData = pair.second;

        final String licenseUri = drmData.getLicenseUri();

        final File localFile = cm.getLocalFile(assetId);

        if (!localFile.canRead()) {
            if (!allowFileNotFound) {
                postEvent(() -> getListener().onRegisterError(assetId, new FileNotFoundException(localFile.getAbsolutePath())));
            }
            return;
        }

        try {
            final byte[] widevineInitData = getWidevineInitData(localFile);

            lam.registerWidevineDashAsset(assetId, licenseUri, widevineInitData);
            postEvent(() -> getListener().onRegistered(assetId, getDrmStatus(assetId, widevineInitData)));

            pendingDrmRegistration.remove(assetId);

        } catch (IOException | LocalAssetsManager.RegisterException e) {
            postEvent(() -> getListener().onRegisterError(assetId, e));
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
    public void startAssetDownload(AssetInfo assetInfo) {
        if (assetInfo == null) {
            log.e("assetInfo == null");
            return;
        }

        if (!(assetInfo instanceof DTGAssetInfo)) {
            throw new IllegalArgumentException("Not a DTGAssetInfo object");
        }

        final DTGAssetInfo dtgAssetInfo = (DTGAssetInfo) assetInfo;

        dtgAssetInfo.downloadItem.startDownload();
    }

    @Override
    public void pauseAssetDownload(String assetId) {
        cm.findItem(assetId).pauseDownload();
    }

    @Override
    public void resumeAssetDownload(String assetId) {
        cm.findItem(assetId).startDownload();
    }

    @Override
    public boolean removeAsset(String assetId) {
        final File localFile = cm.getLocalFile(assetId);
        if (localFile == null) {
            log.e("removeAsset: asset not found");
            return false;
        }

        final byte[] drmInitData = getWidevineInitDataOrNull(localFile);
        lam.unregisterAsset(assetId, drmInitData);
        cm.removeItem(assetId);
        removeAssetSourceId(assetId);

        return true;
    }

    @Override
    public AssetInfo getAssetInfo(String assetId) {
        final DownloadItem item = cm.findItem(assetId);
        if (item == null) {
            return null;
        }
        return new DTGAssetInfo(item, null);
    }

    @Override
    public List<AssetInfo> getAssetsInState(AssetDownloadState state) {
        DownloadState dtgState;
        switch (state) {
            case started:
                dtgState = DownloadState.IN_PROGRESS;
                break;
            case prepared:
                dtgState = DownloadState.INFO_LOADED;
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

        final ArrayList<AssetInfo> list = new ArrayList<>(downloads.size());

        for (DownloadItem item : downloads) {
            list.add(new DTGAssetInfo(item, state));
        }

        return list;
    }

    @Override
    public PKMediaEntry getLocalPlaybackEntry(String assetId) {
        final String playbackURL = cm.getPlaybackURL(assetId);
        final PKMediaSource localMediaSource = lam.getLocalMediaSource(assetId, playbackURL);
        return new PKMediaEntry().setId(assetId).setSources(Collections.singletonList(localMediaSource));
    }
}
