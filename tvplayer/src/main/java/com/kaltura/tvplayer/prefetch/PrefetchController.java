package com.kaltura.tvplayer.prefetch;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;

import java.io.IOException;
import java.util.List;

public interface PrefetchController {

    void prefetchByMediaOptions(List<MediaOptions> mediaOptions); // prepare + start + register
    void prefetchByMediaEntry(List<PKMediaEntry> mediaEntryList); // prepare + start + register
    OfflineManager.AssetInfo getAsset(String assetId);
    boolean isValidAsset(String assetId);
    List<OfflineManager.AssetInfo> getAllAssets();
    void removeAsset(String assetId);  //  for both prefetch state and downloading state
    void removeAllAssets();  // for both prefetch state and downloading state


    PKMediaEntry getMediaEntry(String assetId) throws IOException;
}
