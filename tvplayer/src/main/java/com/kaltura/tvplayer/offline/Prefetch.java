package com.kaltura.tvplayer.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.offline.exo.PrefetchConfig;

import java.util.List;
import java.util.Map;

public interface Prefetch {

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
     * @param prefetchConfig PrefetchConfig
     * @param prefetchCallback Prefetch.PrefetchCallback
     */
    void prefetchAsset(@NonNull PKMediaEntry mediaEntry,
                       @NonNull PrefetchConfig prefetchConfig,
                       @NonNull Prefetch.PrefetchCallback prefetchCallback);

    /**
     * Prefetch an asset. Connect to Kaltura Backend to load entry metadata, select the best source from
     * the entry, load the source metadata, select tracks based on the prefetchConfig, call the listener. If the asset requires
     * KS, make sure to set {@link MediaOptions}.
     * Before calling this method, the partner id and the server URL must be set by {@link OfflineManager#setKalturaParams(KalturaPlayer.Type, int)}
     * and {@link OfflineManager#setKalturaServerUrl(String)}, respectively.
     *
     * @param mediaOptions MediaOptions
     * @param prefetchConfig PrefetchConfig
     * @param prefetchCallback Prefetch.PrefetchCallback
     * @throws IllegalStateException if partner id and/or server URL were not set.
     */
    void prefetchAsset(@NonNull MediaOptions mediaOptions,
                       @NonNull PrefetchConfig prefetchConfig,
                       @NonNull Prefetch.PrefetchCallback prefetchCallback)
            throws IllegalStateException;

    /**
     * Prefetch the list of assets. Connect to Kaltura Backend to load entry metadata, select the best source from
     * the entry, load the source metadata, select tracks based on the prefetchConfig, call the listener. If the asset requires
     * KS, make sure to set {@link MediaOptions}.
     * Before calling this method, the partner id and the server URL must be set by {@link OfflineManager#setKalturaParams(KalturaPlayer.Type, int)}
     * and {@link OfflineManager#setKalturaServerUrl(String)}, respectively.
     *
     * @param mediaOptions MediaOptions
     * @param prefetchConfig PrefetchConfig
     * @param prefetchCallback Prefetch.PrefetchCallback
     * @throws IllegalStateException if partner id and/or server URL were not set.
     */
    void prefetchByMediaOptionsList(@NonNull List<MediaOptions> mediaOptions,
                                    @NonNull PrefetchConfig prefetchConfig,
                                    @NonNull Prefetch.PrefetchCallback prefetchCallback); // prepare + start + register

    /**
     * Prefetch the list of assets. Select the best source from the entry, load the source metadata, select tracks
     * based on the prefetchConfig, call the listener.
     *
     * @param mediaEntryList List of PKMediaEntry
     * @param prefetchConfig PrefetchConfig
     * @param prefetchCallback Prefetch.PrefetchCallback
     */
    void prefetchByMediaEntryList(@NonNull List<PKMediaEntry> mediaEntryList,
                                  @NonNull PrefetchConfig prefetchConfig,
                                  @NonNull Prefetch.PrefetchCallback prefetchCallback); // prepare + start + register


    /**
     * Event callbacks invoked during asset info loading ({@link #prefetchAsset(PKMediaEntry, PrefetchConfig, PrefetchCallback)})
     * or {@link #prefetchAsset(PKMediaEntry, PrefetchConfig, PrefetchCallback)}).
     * The app MUST handle at least {@link #onPrefetched(String, OfflineManager.AssetInfo, Map)} and {@link #onPrefetchError(String, Exception)}.
     * If the app has used {@link #prefetchAsset(PKMediaEntry, PrefetchConfig, PrefetchCallback)}, it MUST also handle
     * {@link #onMediaEntryLoadError(Exception)}.
     */
    interface PrefetchCallback extends OfflineManager.PrepareCallback {
        /**
         * Called when the asset is prefetched
         * @param assetId String
         * @param assetInfo OfflineManager.AssetInfo
         * @param selected Map<OfflineManager.TrackType, List<OfflineManager.Track>>
         */
        void onPrefetched(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected);

        /**
         * Called when asset prefetch has failed for some reason.
         * Must be handled by all applications.
         * @param assetId String
         * @param error Exception
         */
        void onPrefetchError(@NonNull String assetId, @NonNull Exception error);
    }
}
