package com.kaltura.tvplayer.offline.exo;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.OVPMediaOptions;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.offline.Prefetch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
                                           @NonNull OfflineManager.PrepareCallback prefetchCallback) {
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
                                         @NonNull OfflineManager.PrepareCallback prefetchCallback) {
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
        log.d("getAllAssets");

        List<OfflineManager.AssetInfo> prefetchedItems = new ArrayList<>();
        List<OfflineManager.AssetInfo> assetInfoItems = offlineManager.getAllAssets();
        for (OfflineManager.AssetInfo assetInfoItem: assetInfoItems) {
            if (assetInfoItem.getState() == OfflineManager.AssetDownloadState.prefetched &&
                    (assetInfoItem.getPrefetchConfig() != null || assetInfoItem.getDownloadType() == OfflineManager.DownloadType.PREFETCH)) {
                prefetchedItems.add(assetInfoItem);
            } else if (assetInfoItem.getPrefetchConfig() != null) {
                prefetchedItems.add(assetInfoItem);
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
        log.d("removeAllAssets");
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
    public final void prefetchAsset(@NonNull MediaOptions mediaOptions,
                                    @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                                    @NonNull OfflineManager.PrepareCallback prefetchCallback) throws IllegalStateException {

        if (offlineManager.getKalturaPartnerId() == null || offlineManager.getKalturaServerUrl() == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(offlineManager.getKalturaServerUrl(), offlineManager.getKalturaPartnerId());

        mediaEntryProvider.load(response -> postEvent(() -> {
            if (response.isSuccess()) {
                final PKMediaEntry mediaEntry = response.getResponse();
                prefetchCallback.onMediaEntryLoaded(mediaEntry.getId(), OfflineManager.DownloadType.PREFETCH, mediaEntry);
                prefetchAsset(mediaEntry, selectionPrefs, prefetchCallback);
            } else {
                prefetchCallback.onMediaEntryLoadError(OfflineManager.DownloadType.PREFETCH, new IOException(response.getError().getMessage()));
            }
        }));
    }

    @Override
    public void prefetchAsset(@NonNull PKMediaEntry mediaEntry,
                              @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                              @NonNull OfflineManager.PrepareCallback prefetchCallback) throws IllegalStateException {
        if (selectionPrefs == null) {
            selectionPrefs = new OfflineManager.SelectionPrefs();
        }

        List<OfflineManager.AssetInfo> prefetched = getAllAssets();
        if (prefetched.size() > 0 && prefetched.size() >= prefetchConfig.getMaxItemCountInCache()) {
            removeOldestPrefetchedAsset(prefetched);
        }
        selectionPrefs.downloadType = OfflineManager.DownloadType.PREFETCH;

        offlineManager.prepareAsset(mediaEntry, selectionPrefs, new OfflineManager.PrepareCallback() {
            @Override
            public void onPrepared(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected) {
                log.d("onPrepared prefetch");
                ((ExoAssetInfo)assetInfo).downloadType = OfflineManager.DownloadType.PREFETCH;
                ((ExoAssetInfo)assetInfo).prefetchConfig = prefetchConfig;
                offlineManager.startAssetDownload(assetInfo);
            }

            @Override
            public void onPrepareError(@NonNull String assetId, OfflineManager.DownloadType downloadType,  @NonNull Exception error) {
                log.e("onPrepareError prefetch");
                prefetchCallback.onPrepareError(assetId, downloadType, error);
            }
        });
    }

    private void removeOldestPrefetchedAsset(List<OfflineManager.AssetInfo> prefetched) {
        if (prefetched.size() > 0) {
            Collections.sort(prefetched, new OfflineManager.TimestampSorter());

            final String assetId = prefetched.get(0).getAssetId();
            removeAsset(assetId);
            prefetched.remove(0);
        }
    }

    public PrefetchConfig getPrefetchConfig() {
        return prefetchConfig;
    }
}
