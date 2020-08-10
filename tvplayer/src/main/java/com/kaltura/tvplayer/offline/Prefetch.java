package com.kaltura.tvplayer.offline;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.offline.exo.PrefetchConfig;

import java.util.List;

public interface Prefetch {

    boolean isPrefetched(String assetId);

    void prefetchByMediaOptions(List<MediaOptions> mediaOptions, PrefetchConfig prefetchConfig); // prepare + start + register
    void prefetchByMediaEntry(List<PKMediaEntry> mediaEntryList, PrefetchConfig prefetchConfig); // prepare + start + register

    List<OfflineManager.AssetInfo> getAllAssets();
    void removeAsset(String assetId);  //  for both prefetch state and downloading state
    void removeAllAssets();  // for both prefetch state and downloading state

    void cancelAsset(String assetId); // for downloading state only
    void cancelAllAssets(); // for downloading state only
}
