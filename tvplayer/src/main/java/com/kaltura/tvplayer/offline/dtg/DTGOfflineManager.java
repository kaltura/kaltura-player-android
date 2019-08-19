package com.kaltura.tvplayer.offline.dtg;

import android.content.Context;
import com.kaltura.dtg.ContentManager;
import com.kaltura.dtg.DownloadItem;
import com.kaltura.dtg.DownloadState;
import com.kaltura.dtg.DownloadStateListener;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.tvplayer.offline.AbstractOfflineManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class DTGOfflineManager extends AbstractOfflineManager {
    private static final PKLog log = PKLog.get("DTGOfflineManager");

    private static DTGOfflineManager instance;
    private final ContentManager cm;
    private final LocalAssetsManager lam;

    private final DTGListener dtgListener = new DTGListener() {
        @Override
        public void onDownloadComplete(DownloadItem item) {
            postEvent(() -> getListener().onAssetDownloadComplete(item.getItemId()));
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
            postEvent(() -> getListener().onStateChanged(item.getItemId(), buildDtgAssetInfo(item, AssetDownloadState.queued)));
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
        lam = new LocalAssetsManager(context);
    }

    @Override
    public void start(ManagerStartCallback callback) throws IOException {
        cm.addDownloadStateListener(dtgListener);
        cm.start(() -> {
            log.d("Started DTG");
            if (callback != null) {
                callback.onStarted();
            }
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
            }
            dtgItem = cm.createItem(assetId, url);
        } catch (IOException e) {
            postEvent(() -> prepareCallback.onPrepareError(assetId, e));
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);

        DownloadItem[] itemOut = {null};
        Exception[] errorOut = {null};

        final DTGListener listener = new DTGListener() {
            @Override
            public void onDownloadMetadata(DownloadItem item, Exception error) {
                itemOut[0] = item;
                errorOut[0] = error;
                latch.countDown();
            }

            @Override
            public void onTracksAvailable(DownloadItem item, DownloadItem.TrackSelector trackSelector) {
                // TODO: 2019-08-19 select based on prefs
            }
        };
        cm.addDownloadStateListener(listener);

        dtgItem.loadMetadata();

        try {
            latch.await(10, TimeUnit.SECONDS);
            if (errorOut[0] != null) {
                postEvent(() -> prepareCallback.onPrepareError(assetId, errorOut[0]));
            } else {
                postEvent(() -> prepareCallback.onPrepared(assetId, buildDtgAssetInfo(itemOut[0], AssetDownloadState.none), null));
            }
        } catch (InterruptedException e) {
            postEvent(() -> prepareCallback.onPrepareError(assetId, e));
        } finally {
            cm.removeDownloadStateListener(listener);
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
        final String playbackURL = cm.getPlaybackURL(assetId);
        lam.unregisterAsset(playbackURL, assetId, localAssetPath -> getListener().onAssetRemoved(assetId));
        return true;
    }

    @Override
    public void renewDrmAsset(String assetId, PKDrmParams drmParams) {
        final String playbackURL = cm.getPlaybackURL(assetId);
        lam.refreshAsset(new PKMediaSource().setDrmData(Collections.singletonList(drmParams)), playbackURL, assetId, new LocalAssetsManager.AssetRegistrationListener() {
            @Override
            public void onRegistered(String localAssetPath) {
                getListener().onRegistered(assetId, null);// TODO: 2019-08-19 status
            }

            @Override
            public void onFailed(String localAssetPath, Exception error) {
                getListener().onRegisterError(assetId, error);
            }
        });
    }

    @Override
    public AssetInfo getAssetInfo(String assetId) {
        final DownloadItem item = cm.findItem(assetId);
        if (item == null) {
            return null;
        }
        return buildDtgAssetInfo(item, null);
    }

    @Override
    public List<AssetInfo> getAssetsInState(AssetDownloadState state) {
        DownloadState dtgState;
        switch (state) {
            case downloading:
                dtgState = DownloadState.IN_PROGRESS;
                break;
            case queued:
                dtgState = DownloadState.INFO_LOADED;
                break;
            case completed:
                dtgState = DownloadState.COMPLETED;
                break;
            case failed:
                dtgState = DownloadState.FAILED;
                break;
            case stopped:
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
            list.add(buildDtgAssetInfo(item, state));
        }

        return list;
    }

    private DTGAssetInfo buildDtgAssetInfo(DownloadItem item, AssetDownloadState state) {
        final String itemId = item.getItemId();
        final long downloadedSizeBytes = item.getDownloadedSizeBytes();
        final long estimatedSizeBytes = item.getEstimatedSizeBytes();

        if (state == null) {
            switch (item.getState()) {
                case NEW:
                    state = AssetDownloadState.queued;
                    break;
                case INFO_LOADED:
                    state = AssetDownloadState.queued;
                    break;
                case IN_PROGRESS:
                    state = AssetDownloadState.downloading;
                    break;
                case PAUSED:
                    state = AssetDownloadState.stopped;
                    break;
                case COMPLETED:
                    state = AssetDownloadState.completed;
                    break;
                case FAILED:
                    state = AssetDownloadState.failed;
                    break;
            }
        }

        final DTGAssetInfo dtgAssetInfo = new DTGAssetInfo(itemId, state, estimatedSizeBytes, downloadedSizeBytes);

        dtgAssetInfo.downloadItem = item;
        return dtgAssetInfo;
    }

    @Override
    public PKMediaEntry getLocalPlaybackEntry(String assetId) {
        final String playbackURL = cm.getPlaybackURL(assetId);
        final PKMediaSource localMediaSource = lam.getLocalMediaSource(assetId, playbackURL);
        return new PKMediaEntry().setId(assetId).setSources(Collections.singletonList(localMediaSource));
    }

    @Override
    public DrmStatus getDrmStatus(String assetId) {
        final String playbackURL = cm.getPlaybackURL(assetId);

        final CountDownLatch latch = new CountDownLatch(1);

        DrmStatus[] status = {DrmStatus.unknown};

        lam.checkAssetStatus(playbackURL, assetId, (localAssetPath, expiryTimeSeconds, availableTimeSeconds, isRegistered) -> {
            status[0] = DrmStatus.withDrm(expiryTimeSeconds, availableTimeSeconds);
            latch.countDown();
        });

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return status[0];
    }
}

