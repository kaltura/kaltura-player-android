package com.kaltura.tvplayer.offline;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.offline.exo.PrefetchConfig;

import java.util.List;

public interface Prefetch {

    boolean isPrefetched(String assetId);

    void prefetchByMediaOptionsList(List<MediaOptions> mediaOptions, PrefetchConfig prefetchConfig); // prepare + start + register
    void prefetchByMediaEntryList(List<PKMediaEntry> mediaEntryList, PrefetchConfig prefetchConfig); // prepare + start + register

    List<OfflineManager.AssetInfo> getAllAssets();
    OfflineManager.AssetInfo getAssetInfoByAssetId(String assetId);
    void removeAsset(String assetId);  //  for both prefetch state and downloading state
    void removeAllAssets();  // for both prefetch state and downloading state

    void cancelAsset(String assetId); // for downloading state only
    void cancelAllAssets(); // for downloading state only
}
