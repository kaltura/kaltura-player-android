package com.kaltura.tvplayer.offline.exo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.OVPMediaOptions;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.offline.Prefetch;

import java.util.List;
import java.util.Map;

public class PrefetchManager implements Prefetch {

    private static final PKLog log = PKLog.get("PrefetchManager");
    OfflineManager offlineManager;

    public PrefetchManager(OfflineManager offlineManager) {
        this.offlineManager = offlineManager;
    }

    @Override
    public void prefetchByMediaOptionsList(List<MediaOptions> mediaOptionsList, PrefetchConfig prefetchConfig) {
        log.d("prefetchByMediaOptions");

        if (mediaOptionsList == null) {
            return;
        }

        for (MediaOptions mediaOptions : mediaOptionsList) {
            boolean isOTTMedia = false;
            // prepare + start
            if (mediaOptions instanceof OTTMediaOptions) {
                isOTTMedia = true;
            } else if (mediaOptions instanceof OVPMediaOptions) {
                isOTTMedia = false;
            } else {
                return;
            }
            offlineManager.prefetchAsset(isOTTMedia ? (OTTMediaOptions) mediaOptions : (OVPMediaOptions) mediaOptions, prefetchConfig, getPrefetchCallback());
        }
    }

    @Override
    public void prefetchByMediaEntryList(List<PKMediaEntry> mediaEntryList, PrefetchConfig prefetchConfig) {
        log.d("prefetchByMediaEntry");

        if (mediaEntryList == null) {
            return;
        }

        for (PKMediaEntry mediaEntry : mediaEntryList) {
            offlineManager.prefetchAsset(mediaEntry, prefetchConfig, getPrefetchCallback());
        }
    }

    @Override
    public List<OfflineManager.AssetInfo> getAllAssets() {
        log.d("getAllItems");
        return offlineManager.getAssetsInState(OfflineManager.AssetDownloadState.prefetched);
    }

    @Override
    public boolean isPrefetched(String assetId) {
        log.d("isPrefetched");
        OfflineManager.AssetInfo assetInfo = offlineManager.getAssetInfo(assetId);
        return assetInfo != null && (assetInfo.getState() == OfflineManager.AssetDownloadState.prefetched || assetInfo.getState() == OfflineManager.AssetDownloadState.completed);
    }

    @Override
    public void removeAsset(String assetId) {
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

        // remove all still downloading assets
        //TODO remove all still downloading assets
    }

    @Override
    public void cancelAsset(String assetId) {
        OfflineManager.AssetInfo assetInfo = offlineManager.getAssetInfo(assetId);
        if (assetInfo instanceof ExoAssetInfo && (((ExoAssetInfo) assetInfo).prefetchConfig != null)) {

            if (assetInfo.getState() == OfflineManager.AssetDownloadState.started && (assetInfo instanceof ExoAssetInfo && (((ExoAssetInfo) assetInfo).prefetchConfig != null))) {
                removeAsset(assetId);
            }
        }
    }

    @Override
    public void cancelAllAssets() {
        List<OfflineManager.AssetInfo> downloadingAssetInfoList = offlineManager.getAssetsInState(OfflineManager.AssetDownloadState.started);
        if (downloadingAssetInfoList == null) {
            return;
        }

        for (OfflineManager.AssetInfo assetInfo : downloadingAssetInfoList) {
            if (assetInfo instanceof ExoAssetInfo && (((ExoAssetInfo) assetInfo).prefetchConfig != null)) {
                removeAsset(assetInfo.getAssetId());
            }
        }
    }

    private OfflineManager.PrefetchCallback getPrefetchCallback() {
        return new OfflineManager.PrefetchCallback() {
            @Override
            public void onPrefetched(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected) {
                log.d("onPrefetched");
            }

            @Override
            public void onPrefetchError(@NonNull String assetId, @NonNull Exception error) {
                log.d("onPrefetchError");

            }

            @Override
            public void onPrepared(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected) {
                log.d("onPrepared");

            }

            @Override
            public void onPrepareError(@NonNull String assetId, @NonNull Exception error) {
                log.d("onPrepareError");

            }

            @Override
            public void onMediaEntryLoaded(@NonNull String assetId, @NonNull PKMediaEntry mediaEntry) {
                log.d("onMediaEntryLoaded");

            }

            @Override
            public void onMediaEntryLoadError(@NonNull Exception error) {
                log.d("onMediaEntryLoadError");

            }

            @Override
            public void onSourceSelected(@NonNull String assetId, @NonNull PKMediaSource source, @Nullable PKDrmParams drmParams) {
                log.d("onSourceSelected");

            }
        };
    }
}
