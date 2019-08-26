package com.kaltura.tvplayer.offline.dtg;

import com.kaltura.dtg.DownloadItem;
import com.kaltura.tvplayer.OfflineManager;
import com.kaltura.tvplayer.OfflineManager.AssetDownloadState;

class DTGAssetInfo extends OfflineManager.AssetInfo {

    final private String itemId;
    final private AssetDownloadState state;
    final private long estimatedSize;
    final private long bytesDownloaded;
    final DownloadItem downloadItem;

    DTGAssetInfo(DownloadItem item, AssetDownloadState state) {
        this.itemId = item.getItemId();
        this.bytesDownloaded = item.getDownloadedSizeBytes();
        this.estimatedSize = item.getEstimatedSizeBytes();

        this.downloadItem = item;

        if (state == null) {
            switch (item.getState()) {
                case NEW:
                    state = AssetDownloadState.none;
                    break;
                case INFO_LOADED:
                    state = AssetDownloadState.prepared;
                    break;
                case IN_PROGRESS:
                    state = AssetDownloadState.started;
                    break;
                case PAUSED:
                    state = AssetDownloadState.paused;
                    break;
                case COMPLETED:
                    state = AssetDownloadState.completed;
                    break;
                case FAILED:
                    state = AssetDownloadState.failed;
                    break;
            }
        }

        this.state = state;
    }

    @Override
    public void release() {
//        ContentManager.getInstance(null).release(this);
    }

    @Override
    public String getAssetId() {
        return itemId;
    }

    @Override
    public AssetDownloadState getState() {
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
