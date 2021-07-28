package com.kaltura.tvplayer.offline;

import androidx.annotation.NonNull;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.offline.exo.PrefetchConfig;

import java.util.List;

public interface Prefetch {

    /**
     * Prefetch config for {@link com.kaltura.tvplayer.offline.exo.PrefetchManager}
     * @param prefetchConfig config
     */
    void setPrefetchConfig(PrefetchConfig prefetchConfig);

    /**
     * Checks if the asset is already prefetched
     * @param assetId String
     * @return is the asset already prefetched or not
     */
    boolean isPrefetched(@NonNull String assetId);

    /**
     * Get all the assets which are already prefetched
     * @return List of assets
     */
    List<OfflineManager.AssetInfo> getAllAssets();

    /**
     * Get the asset info as per assetId
     * @param assetId AssetId
     * @return AssetInfo
     */
    OfflineManager.AssetInfo getAssetInfoByAssetId(@NonNull String assetId);

    /**
     * Remove the specific asset
     * @param assetId AssetId
     */
    void removeAsset(@NonNull String assetId);  //  for both prefetch state and downloading state

    /**
     * Remove all the assets
     */
    void removeAllAssets();  // for both prefetch state and downloading state

    /**
     * Cancel a specific asset (only if it is being downloaded)
     * @param assetId AssetId
     */
    void cancelAsset(@NonNull String assetId); // for downloading state only

    /**
     * Cancel all the assets (only those which are being downloaded)
     */
    void cancelAllAssets(); // for downloading state only

    /**
     * Prefetch an asset. Select the best source from the entry, load the source metadata, select tracks
     * based on the prefetchConfig, call the listener.
     *
     * @param mediaEntry PKMediaEntry
     * @param selectionPrefs OfflineManager.SelectionPrefs
     * @param prefetchCallback OfflineManager.PrepareCallback
     */
    void prefetchAsset(@NonNull PKMediaEntry mediaEntry,
                       @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                       @NonNull OfflineManager.PrepareCallback prefetchCallback);

    /**
     * Prefetch an asset. Connect to Kaltura Backend to load entry metadata, select the best source from
     * the entry, load the source metadata, select tracks based on the prefetchConfig, call the listener. If the asset requires
     * KS, make sure to set {@link MediaOptions}.
     * Before calling this method, the partner id and the server URL must be set by {@link OfflineManager#setKalturaParams(KalturaPlayer.Type, int)}
     * and {@link OfflineManager#setKalturaServerUrl(String)}, respectively.
     *
     * @param mediaOptions MediaOptions
     * @param selectionPrefs OfflineManager.SelectionPrefs
     * @param prefetchCallback OfflineManager.PrepareCallback
     * @throws IllegalStateException if partner id and/or server URL were not set.
     */
    void prefetchAsset(@NonNull MediaOptions mediaOptions,
                       @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                       @NonNull OfflineManager.PrepareCallback prefetchCallback)
            throws IllegalStateException;

    /**
     * Prefetch the list of assets. Connect to Kaltura Backend to load entry metadata, select the best source from
     * the entry, load the source metadata, select tracks based on the prefetchConfig, call the listener. If the asset requires
     * KS, make sure to set {@link MediaOptions}.
     * Before calling this method, the partner id and the server URL must be set by {@link OfflineManager#setKalturaParams(KalturaPlayer.Type, int)}
     * and {@link OfflineManager#setKalturaServerUrl(String)}, respectively.
     *
     * @param mediaOptions MediaOptions
     * @param selectionPrefs OfflineManager.SelectionPrefs
     * @param prefetchCallback OfflineManager.PrepareCallback
     * @throws IllegalStateException if partner id and/or server URL were not set.
     */
    void prefetchByMediaOptionsList(@NonNull List<MediaOptions> mediaOptions,
                                    @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                                    @NonNull OfflineManager.PrepareCallback prefetchCallback); // prepare + start + register

    /**
     * Prefetch the list of assets. Select the best source from the entry, load the source metadata, select tracks
     * based on the prefetchConfig, call the listener.
     *
     * @param mediaEntryList List of PKMediaEntry
     * @param selectionPrefs OfflineManager.SelectionPrefs
     * @param prefetchCallback OfflineManager.PrepareCallback
     */
    void prefetchByMediaEntryList(@NonNull List<PKMediaEntry> mediaEntryList,
                                  @NonNull OfflineManager.SelectionPrefs selectionPrefs,
                                  @NonNull OfflineManager.PrepareCallback prefetchCallback); // prepare + start + register


}
