//package com.kaltura.tvplayer.prefetch;
//
//import android.content.Context;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//
//import com.kaltura.playkit.PKDrmParams;
//import com.kaltura.playkit.PKLog;
//import com.kaltura.playkit.PKMediaEntry;
//import com.kaltura.playkit.PKMediaSource;
//
//import com.kaltura.tvplayer.KalturaPlayer;
//import com.kaltura.tvplayer.MediaOptions;
//import com.kaltura.tvplayer.OTTMediaOptions;
//import com.kaltura.tvplayer.OVPMediaOptions;
//import com.kaltura.tvplayer.OfflineManager;
//import com.kaltura.tvplayer.offline.exo.PrefetchConfig;
//import com.kaltura.tvplayer.prefetch.PrefetchController;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//
//public class PrefetchManager implements PrefetchController {
//
//    private static final PKLog log = PKLog.get("PrefetchManager");
//
//    Context context;
//    private KalturaPlayer kalturaPlayer; // used for message bus
//    PrefetchConfig prefetchConfig;
//    OfflineManager offlineManager;
//
//    public PrefetchManager(Context context, KalturaPlayer kalturaPlayer, PrefetchConfig prefetchConfig) {
//
//        this.context = context;
//        this.kalturaPlayer = kalturaPlayer;
//        offlineManager = OfflineManager.getInstance(context);
//
//        //TODO Change to ADD listener?
//        offlineManager.setAssetStateListener(new OfflineManager.AssetStateListener() {
//            @Override
//            public void onStateChanged(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo) {
//                log.d("onStateChanged " + assetId);
//
//            }
//
//            @Override
//            public void onAssetRemoved(@NonNull String assetId) {
//                log.d("onAssetRemoved " + assetId);
//            }
//
//            @Override
//            public void onAssetDownloadFailed(@NonNull String assetId, @NonNull Exception error) {
//                log.d("onAssetDownloadFailed " + assetId);
//                // TODO Fire error
//
//            }
//
//            @Override
//            public void onAssetDownloadComplete(@NonNull String assetId) {
//                log.d("onAssetDownloadComplete " + assetId);
//            }
//
//            @Override
//            public void onAssetDownloadPending(@NonNull String assetId) {
//                log.d("onAssetDownloadPending " + assetId);
//            }
//
//            @Override
//            public void onAssetDownloadPaused(@NonNull String assetId) {
//                log.d("onAssetDownloadPaused " + assetId);
//            }
//
//            @Override
//            public void onAssetPrefetched(@NonNull String assetId) {
//                log.d("onAssetPrefetched " + assetId);
//                // TODO Fire update
//            }
//
//            @Override
//            public void onRegistered(@NonNull String assetId, @NonNull OfflineManager.DrmStatus drmStatus) {
//                log.d("onRegistered " + assetId);
//
//            }
//
//            @Override
//            public void onRegisterError(@NonNull String assetId, @NonNull Exception error) {
//                log.d("onRegisterError " + assetId);
//                // TODO Fire update
//            }
//        });
//
//        this.prefetchConfig = prefetchConfig;
//    }
//
//    @Override
//    public void prefetchByMediaOptions(List<MediaOptions> mediaOptionsList) {
//        log.d("prefetchByMediaOptions");
//
//        if (mediaOptionsList == null) {
//            return;
//        }
//
//        for (MediaOptions mediaOptions : mediaOptionsList) {
//            boolean isOTTMedia = false;
//            // prepare + start
//            if (mediaOptions instanceof OTTMediaOptions) {
//                isOTTMedia = true;
//            } else if (mediaOptions instanceof OVPMediaOptions) {
//                isOTTMedia = false;
//            } else {
//                return;
//            }
//            offlineManager.prepareAsset(isOTTMedia ? (OTTMediaOptions) mediaOptions : (OVPMediaOptions) mediaOptions, prefetchConfig.getSelectionPrefs(), getPrepareCalback());
//        }
//    }
//
//    @Override
//    public void prefetchByMediaEntry(List<PKMediaEntry> mediaEntryList) {
//        log.d("prefetchByMediaEntry");
//
//        if (mediaEntryList == null) {
//            return;
//        }
//
//        for (PKMediaEntry mediaEntry : mediaEntryList) {
//            offlineManager.prepareAsset(mediaEntry, prefetchConfig.getSelectionPrefs(), getPrepareCalback());
//        }
//        // prepare + start
//    }
//
//    @Override
//    public OfflineManager.AssetInfo getAsset(String assetId){
//        return offlineManager.getAssetInfo(assetId);
//    }
//
//    @Override
//    public List<OfflineManager.AssetInfo> getAllAssets() {
//        log.d("getAllItems");
//        return offlineManager.getAssetsInState(OfflineManager.AssetDownloadState.prefetched);
//    }
//
//    @Override
//    public boolean isValidAsset(String assetId) {
//        log.d("isValidItemId");
//        OfflineManager.AssetInfo assetInfo = offlineManager.getAssetInfo(assetId);
//        return assetInfo != null && assetInfo.getState() == OfflineManager.AssetDownloadState.prefetched;
//    }
//
//    @Override
//    public void removeAsset(String assetId) {
//        log.d("remove");
//        offlineManager.removeAsset(assetId);
//        // both for prefetch state and downloading state
//    }
//
//    @Override
//    public void removeAllAssets() {
//        log.d("removeAll");
//        List<OfflineManager.AssetInfo> assetInfoList = getAllAssets();
//        if (assetInfoList != null && !assetInfoList.isEmpty()) {
//            for (OfflineManager.AssetInfo assetInfo : assetInfoList) {
//                removeAsset(assetInfo.getAssetId());
//            }
//        }
//
//        // remove all still downloading assets
//        //TODO remove all still downloading assets
//    }
//
//    @Override
//    public PKMediaEntry getMediaEntry(String assetId) throws IOException {
//        return offlineManager.getLocalPlaybackEntry(assetId);
//    }
//
////    private void prepareMedia(PKMediaEntry mediaEntry) {
////        log.d("prepareMedia");
////
////    }
//
//    private OfflineManager.PrepareCallback getPrepareCalback() {
//        return  new OfflineManager.PrepareCallback() {
//            @Override
//            public void onPrepared(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected) {
//                log.d("onPrepared");
//                offlineManager.startAssetDownload(assetInfo);
//
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
//
//            }
//
//            @Override
//            public void onMediaEntryLoadError(@NonNull Exception error) {
//                log.d("onMediaEntryLoadError");
//
//            }
//
//            @Override
//            public void onSourceSelected(@NonNull String assetId, @NonNull PKMediaSource source, @Nullable PKDrmParams drmParams) {
//                log.d("onSourceSelected");
//
//            }
//        };
//    }
//}
