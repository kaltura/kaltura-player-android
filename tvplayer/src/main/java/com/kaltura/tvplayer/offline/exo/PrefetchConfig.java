package com.kaltura.tvplayer.offline.exo;

public class PrefetchConfig {

    int maxItemCountInCache = 20;
//    int maxCacheSize = 100; // in MB
    int assetPrefetchSize = 2; // in MB
    boolean removeCacheOnDestroy = true;
//    boolean freeCacheIfFull = true; // remove first one by time stamp?

    public PrefetchConfig setMaxItemCountInCache(int maxItemCountInCache) {
        this.maxItemCountInCache = maxItemCountInCache;
        return this;
    }

//    public PrefetchConfig setMaxCacheSize(int maxCacheSize) {
//        this.maxCacheSize = maxCacheSize;
//        return this;
//    }
//
    public PrefetchConfig setAssetPrefetchSize(int assetPrefetchSize) {
        this.assetPrefetchSize = assetPrefetchSize;
        return this;
    }

    public PrefetchConfig setRemoveCacheOnDestroy(boolean removeCacheOnDestroy) {
        this.removeCacheOnDestroy = removeCacheOnDestroy;
        return this;
    }

//    public PrefetchConfig setFreeCacheIfFull(boolean freeCacheIfFull) {
//        this.freeCacheIfFull = freeCacheIfFull;
//        return this;
//    }

    public int getMaxItemCountInCache() {
        return maxItemCountInCache;
    }

//    public int getMaxCacheSize() {
//        return maxCacheSize;
//    }
//
    public int getAssetPrefetchSize() {
        return assetPrefetchSize;
    }

    public boolean isRemoveCacheOnDestroy() {
        return removeCacheOnDestroy;
    }

//    public boolean freeCacheIfFull() {
//        return freeCacheIfFull;
//    }
}
