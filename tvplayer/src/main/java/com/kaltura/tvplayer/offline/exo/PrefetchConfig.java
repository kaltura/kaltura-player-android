package com.kaltura.tvplayer.offline.exo;

import com.kaltura.playkit.utils.Consts;


public class PrefetchConfig {

    int maxItemCountInCache = 50;
    int maxCacheSize = 100; // in MB
    int assetPrefetchSize = 2; // in MB
    long assetPrefetchDuration = 20 * Consts.MILLISECONDS_MULTIPLIER; //20 seconds
    boolean removeCacheOnDestroy = true;
    boolean emptyCacheIfFull = true; // remove first one by time stamp?

    public PrefetchConfig setMaxItemCountInCache(int maxItemCountInCache) {
        this.maxItemCountInCache = maxItemCountInCache;
        return this;
    }

    public PrefetchConfig setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        return this;
    }

    public PrefetchConfig setAssetPrefetchDuration(long assetPrefetchDuration) {
        this.assetPrefetchDuration = assetPrefetchDuration;
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

    public PrefetchConfig setEmptyCacheIfFull(boolean emptyCacheIfFull) {
        this.emptyCacheIfFull = emptyCacheIfFull;
        return this;
    }

    public int getMaxItemCountInCache() {
        return maxItemCountInCache;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public int getAssetPrefetchSize() {
        return assetPrefetchSize;
    }

    public long getAssetPrefetchDuration() {
        return assetPrefetchDuration;
    }

    public boolean isRemoveCacheOnDestroy() {
        return removeCacheOnDestroy;
    }

    public boolean isEmptyCacheIfFull() {
        return emptyCacheIfFull;
    }
}
