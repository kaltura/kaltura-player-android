package com.kaltura.tvplayer.offline.exo;

public class PrefetchConfig {

    int maxItemCountInCache = 20;
    int assetPrefetchSize = 2; // in MB
    boolean removeCacheOnDestroy = true;

    public PrefetchConfig setMaxItemCountInCache(int maxItemCountInCache) {
        this.maxItemCountInCache = maxItemCountInCache;
        return this;
    }

    public PrefetchConfig setAssetPrefetchSize(int assetPrefetchSize) {
        this.assetPrefetchSize = assetPrefetchSize;
        return this;
    }

    public PrefetchConfig setRemoveCacheOnDestroy(boolean removeCacheOnDestroy) {
        this.removeCacheOnDestroy = removeCacheOnDestroy;
        return this;
    }

    public int getMaxItemCountInCache() {
        return maxItemCountInCache;
    }

    public int getAssetPrefetchSize() {
        return assetPrefetchSize;
    }

    public boolean isRemoveCacheOnDestroy() {
        return removeCacheOnDestroy;
    }
}
