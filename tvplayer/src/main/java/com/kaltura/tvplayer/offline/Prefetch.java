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

    boolean isPrefetched(@NonNull String assetId);

    void prefetchByMediaOptionsList(@NonNull List<MediaOptions> mediaOptions,
                                    @NonNull PrefetchConfig prefetchConfig,
                                    @NonNull Prefetch.PrefetchCallback prefetchCallback); // prepare + start + register
    void prefetchByMediaEntryList(@NonNull List<PKMediaEntry> mediaEntryList,
                                  @NonNull PrefetchConfig prefetchConfig,
                                  @NonNull Prefetch.PrefetchCallback prefetchCallback); // prepare + start + register

    List<OfflineManager.AssetInfo> getAllAssets();
    OfflineManager.AssetInfo getAssetInfoByAssetId(@NonNull String assetId);
    void removeAsset(@NonNull String assetId);  //  for both prefetch state and downloading state
    void removeAllAssets();  // for both prefetch state and downloading state

    void cancelAsset(@NonNull String assetId); // for downloading state only
    void cancelAllAssets(); // for downloading state only

    /**
     * Prefetch an asset. Select the best source from the entry, load the source metadata, select tracks
     * based on the prefetchConfig, call the listener.
     *
     * @param mediaEntry
     * @param prefetchConfig
     * @param prefetchCallback
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
     * @param mediaOptions
     * @param prefetchConfig
     * @param prefetchCallback
     * @throws IllegalStateException if partner id and/or server URL were not set.
     */
    void prefetchAsset(@NonNull MediaOptions mediaOptions,
                       @NonNull PrefetchConfig prefetchConfig,
                       @NonNull Prefetch.PrefetchCallback prefetchCallback)
            throws IllegalStateException;

    /**
     * Event callbacks invoked during asset info loading ({@link #prefetchAsset(PKMediaEntry, PrefetchConfig, PrefetchCallback)})
     * or {@link #prefetchAsset(PKMediaEntry, PrefetchConfig, PrefetchCallback)}).
     * The app MUST handle at least {@link #onPrefetched(String, OfflineManager.AssetInfo, Map)}  and {@link #onPrefetchError(String, Exception)}. If the
     * app has used {@link #prefetchAsset(PKMediaEntry, PrefetchConfig, PrefetchCallback)}, it MUST also handle
     * {@link #onMediaEntryLoadError(Exception)}.
     */
    public interface PrefetchCallback extends OfflineManager.PrepareCallback {
        /**
         * Called when the asset is prefetched
         * @param assetId
         * @param assetInfo
         * @param selected
         */
        void onPrefetched(@NonNull String assetId, @NonNull OfflineManager.AssetInfo assetInfo, @Nullable Map<OfflineManager.TrackType, List<OfflineManager.Track>> selected);

        /**
         * Called when asset prefetch has failed for some reason.
         * Must be handled by all applications.
         * @param assetId
         * @param error
         */
        void onPrefetchError(@NonNull String assetId, @NonNull Exception error);
    }
}
