package com.kaltura.tvplayer.offline.exo;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.OVPMediaOptions;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.offline.Prefetch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrefetchManager implements Prefetch {

    private static final PKLog log = PKLog.get("PrefetchManager");

    private PrefetchConfig prefetchConfig;
    private final Handler eventHandler;
    OfflineManager offlineManager;

    protected void postEvent(Runnable event) {
        eventHandler.post(event);
    }

    protected void removeEventHandler() {
        eventHandler.removeCallbacksAndMessages(null);
    }

    public PrefetchManager(OfflineManager offlineManager) {
        this.offlineManager = offlineManager;
        this.prefetchConfig = new PrefetchConfig();
        HandlerThread handlerThread = new HandlerThread("PrefetchManagerEvents");
        handlerThread.start();
        eventHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void prefetchByMediaOptionsList(@NonNull List<MediaOptions> mediaOptionsList,
                                           @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                                           @NonNull Prefetch.PrefetchCallback prefetchCallback) {
        log.d("prefetchByMediaOptions");

        if (mediaOptionsList == null) {
            return;
        }

        for (MediaOptions mediaOptions : mediaOptionsList) {
            boolean isOTTMedia = false;
            // prepare + start
            if (mediaOptions instanceof OTTMediaOptions) {
                isOTTMedia = true;
            } else if (!(mediaOptions instanceof OVPMediaOptions)) {
                return;
            }
            prefetchAsset(isOTTMedia ? (OTTMediaOptions) mediaOptions : (OVPMediaOptions) mediaOptions, selectionPrefs, prefetchCallback);
        }
    }

    @Override
    public void prefetchByMediaEntryList(@NonNull List<PKMediaEntry> mediaEntryList,
                                         @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                                         @NonNull Prefetch.PrefetchCallback prefetchCallback) {
        log.d("prefetchByMediaEntry");

        if (mediaEntryList == null) {
            return;
        }

        for (PKMediaEntry mediaEntry : mediaEntryList) {
            prefetchAsset(mediaEntry, selectionPrefs, prefetchCallback);
        }
    }

    @Override
    public List<OfflineManager.AssetInfo> getAllAssets() {
        log.d("getAllItems");
        List<OfflineManager.AssetInfo> prefetchedItems = new ArrayList<>();

        List<OfflineManager.AssetInfo> allStartedItems =  offlineManager.getAssetsInState(OfflineManager.AssetDownloadState.started);
        for (OfflineManager.AssetInfo startedItem: allStartedItems) {
            if (startedItem.getPrefetchConfig() != null) {
                prefetchedItems.add(startedItem);
            }
        }

        List<OfflineManager.AssetInfo> allCompletedItems =  offlineManager.getAssetsInState(OfflineManager.AssetDownloadState.completed);
        for (OfflineManager.AssetInfo completedItem: allCompletedItems) {
            if (completedItem.getPrefetchConfig() != null) {
                prefetchedItems.add(completedItem);
            }
        }

        List<OfflineManager.AssetInfo> allStoppedItems =  offlineManager.getAssetsInState(OfflineManager.AssetDownloadState.prefetched);
        for (OfflineManager.AssetInfo stoppedItem: allStoppedItems) {
            if (stoppedItem.getPrefetchConfig() != null || stoppedItem.getDownloadType() == OfflineManager.DownloadType.PREFETCH) {
                prefetchedItems.add(stoppedItem);
            }
        }

        return prefetchedItems;
    }

    @Override
    public OfflineManager.AssetInfo getAssetInfoByAssetId(@NonNull String assetId) {
        return offlineManager.getAssetInfo(assetId);
    }

    @Override
    public void setPrefetchConfig(PrefetchConfig prefetchConfig) {
        this.prefetchConfig = prefetchConfig;
    }

    @Override
    public boolean isPrefetched(@NonNull String assetId) {
        log.d("isPrefetched");
        OfflineManager.AssetInfo assetInfo = offlineManager.getAssetInfo(assetId);
        return assetInfo != null && assetInfo.getState() == OfflineManager.AssetDownloadState.prefetched;
    }

    @Override
    public void removeAsset(@NonNull String assetId) {
        log.d("removeAsset");
        offlineManager.removeAsset(assetId);
        // both for prefetch state and downloading state
    }

    @Override
    public void removeAllAssets() {
        log.d("removeAll");
        List<OfflineManager.AssetInfo> assetInfoList = getAllAssets();
        if (assetInfoList != null && !assetInfoList.isEmpty()) {
            for (OfflineManager.AssetInfo assetInfo : assetInfoList) {
                removeAsset(assetInfo.getAssetId());
            }
        }
    }

    @Override
    public void cancelAsset(@NonNull String assetId) {
        OfflineManager.AssetInfo assetInfo = offlineManager.getAssetInfo(assetId);
        if (assetInfo instanceof ExoAssetInfo &&
                assetInfo.getPrefetchConfig() != null &&
                assetInfo.getState() == OfflineManager.AssetDownloadState.started) {
                log.d("cancelAsset id: " + assetId);
                removeAsset(assetId);
        }
    }

    @Override
    public void cancelAllAssets() {
        List<OfflineManager.AssetInfo> downloadingAssetInfoList = offlineManager.getAssetsInState(OfflineManager.AssetDownloadState.started);
        if (downloadingAssetInfoList == null) {
            return;
        }
        log.d("cancelAllAssets");
        for (OfflineManager.AssetInfo assetInfo : downloadingAssetInfoList) {
            if (assetInfo instanceof ExoAssetInfo && assetInfo.getPrefetchConfig() != null) {
                removeAsset(assetInfo.getAssetId());
            }
        }
    }

    @Override
    public final void prefetchAsset(@NonNull MediaOptions mediaOptions, @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                                    @NonNull PrefetchCallback prefetchCallback) throws IllegalStateException {

        if (offlineManager.getKalturaPartnerId() == null || offlineManager.getKalturaServerUrl() == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(offlineManager.getKalturaServerUrl(), offlineManager.getKalturaPartnerId());

        mediaEntryProvider.load(response -> postEvent(() -> {
            if (response.isSuccess()) {
                final PKMediaEntry mediaEntry = response.getResponse();
                prefetchCallback.onMediaEntryLoaded(mediaEntry.getId(), mediaEntry);
                prefetchAsset(mediaEntry, selectionPrefs, prefetchCallback);
            } else {
                prefetchCallback.onMediaEntryLoadError(new IOException(response.getError().getMessage()));
            }
        }));
    }

    @Override
    public void prefetchAsset(@NonNull PKMediaEntry mediaEntry, @NonNull OfflineManager.SelectionPrefs selectionPrefs, @NonNull PrefetchCallback prefetchCallback) {
        if (selectionPrefs == null) {
            selectionPrefs = new OfflineManager.SelectionPrefs();
        }

        selectionPrefs.downloadType = OfflineManager.DownloadType.PREFETCH;
        offlineManager.prepareAsset(mediaEntry, selectionPrefs, new OfflineManager.PrepareCallback() {
            @Override
            public void onPrepared(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected) {
                ((ExoAssetInfo)assetInfo).downloadType = OfflineManager.DownloadType.PREFETCH;
                ((ExoAssetInfo)assetInfo).prefetchConfig = prefetchConfig;
                offlineManager.startAssetDownload(assetInfo);
            }

            @Override
            public void onPrepareError(@NonNull String assetId, @NonNull Exception error) {
                log.e("onPrepareError");
                prefetchCallback.onPrefetchError(assetId, error);
            }

            @Override
            public void onSourceSelected(@NonNull String assetId, @NonNull PKMediaSource source, @Nullable PKDrmParams drmParams) {
                log.d("onSourceSelected");
                prefetchCallback.onSourceSelected(assetId, source, drmParams);
            }
        });
    }

    public PrefetchConfig getPrefetchConfig() {
        return prefetchConfig;
    }

    //    private Prefetch.PrefetchCallback getPrefetchCallback() {
//        return new Prefetch.PrefetchCallback() {
//            @Override
//            public void onPrefetched(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected) {
//                log.d("onPrefetched");
//            }
//
//            @Override
//            public void onPrefetchError(@NonNull String assetId, @NonNull Exception error) {
//                log.d("onPrefetchError");
//            }
//
//            @Override
//            public void onPrepared(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected) {
//                log.d("onPrepared");
//            }
//
//            @Override
//            public void onPrepareError(@NonNull String assetId, @NonNull Exception error) {
//                log.d("onPrepareError");
//            }
//
//            @Override
//            public void onMediaEntryLoaded(@NonNull String assetId, @NonNull PKMediaEntry mediaEntry) {
//                log.d("onMediaEntryLoaded");
//            }
//
//            @Override
//            public void onMediaEntryLoadError(@NonNull Exception error) {
//                log.d("onMediaEntryLoadError");
//            }
//
//            @Override
//            public void onSourceSelected(@NonNull String assetId, @NonNull PKMediaSource source, @Nullable PKDrmParams drmParams) {
//                log.d("onSourceSelected");
//            }
//        };
//    }
}
