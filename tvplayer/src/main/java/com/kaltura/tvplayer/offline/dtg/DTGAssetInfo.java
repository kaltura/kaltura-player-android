package com.kaltura.tvplayer.offline.dtg;

import com.kaltura.dtg.DownloadItem;
import com.kaltura.tvplayer.OfflineManager;

class DTGAssetInfo extends OfflineManager.AssetInfo {

    final private String itemId;
    final private OfflineManager.AssetDownloadState state;
    final private long estimatedSize;
    final private long bytesDownloaded;
    DownloadItem downloadItem;

    DTGAssetInfo(String itemId, OfflineManager.AssetDownloadState state, long estimatedSize, long bytesDownloaded) {
        this.itemId = itemId;
        this.state = state;
        this.estimatedSize = estimatedSize;
        this.bytesDownloaded = bytesDownloaded;
    }

    @Override
    public void release() {

    }

    @Override
    public String getAssetId() {
        return itemId;
    }

    @Override
    public OfflineManager.AssetDownloadState getState() {
        return state;
    }

    @Override
    public long getEstimatedSize() {
        return estimatedSize;
    }

    @Override
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }

}
