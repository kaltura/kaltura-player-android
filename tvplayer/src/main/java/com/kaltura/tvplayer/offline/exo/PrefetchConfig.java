package com.kaltura.tvplayer.offline.exo;

public class PrefetchConfig {

    private int maxItemCountInCache = 20;
    private int assetPrefetchSize = 2; // in MB
    private boolean cleanPrefetchedAssets = true;

    public PrefetchConfig setMaxItemCountInCache(int maxItemCountInCache) {
        this.maxItemCountInCache = maxItemCountInCache;
        return this;
    }

    public PrefetchConfig setAssetPrefetchSize(int assetPrefetchSize) {
        this.assetPrefetchSize = assetPrefetchSize;
        return this;
    }

    public PrefetchConfig setCleanPrefetchedAssets(boolean cleanPrefetchedAssets) {
        this.cleanPrefetchedAssets = cleanPrefetchedAssets;
        return this;
    }

    public int getMaxItemCountInCache() {
        return maxItemCountInCache;
    }

    public int getAssetPrefetchSize() {
        return assetPrefetchSize;
    }

    public boolean isCleanPrefetchedAssets() {
        return cleanPrefetchedAssets;
    }
}
