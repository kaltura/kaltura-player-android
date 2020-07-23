package com.kaltura.tvplayer.offline.exo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadHelper;
import com.kaltura.tvplayer.OfflineManager;

import org.json.JSONException;
import org.json.JSONObject;

class ExoAssetInfo extends OfflineManager.AssetInfo {

    private final String assetId;
    private final OfflineManager.AssetDownloadState state;
    private final float percentDownloaded;
    private final long estimatedSize;
    private final long bytesDownloaded;

    @Nullable
    final DownloadHelper downloadHelper;  // Only used during preparation

    ExoAssetInfo(String assetId, OfflineManager.AssetDownloadState state, long estimatedSize, long bytesDownloaded, @SuppressWarnings("NullableProblems") DownloadHelper downloadHelper) {
        this.assetId = assetId;
        this.state = state;
        this.estimatedSize = estimatedSize;
        this.bytesDownloaded = bytesDownloaded;
        this.downloadHelper = downloadHelper;
        percentDownloaded = 0;
    }

    ExoAssetInfo(Download download) {
        assetId = download.request.id;
        downloadHelper = null;
        percentDownloaded = download.getPercentDownloaded();
        bytesDownloaded = download.getBytesDownloaded();

        if (download.contentLength > 0) {
            estimatedSize = download.contentLength;

        } else {
            long estimatedSizeBytes;
            byte[] data = download.request.data;
            final JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(new String(data));
                estimatedSizeBytes = jsonObject.getLong("estimatedSizeBytes");
            } catch (JSONException e) {
                estimatedSizeBytes = (long) (100 * bytesDownloaded / percentDownloaded);
                e.printStackTrace();
            }
            this.estimatedSize = estimatedSizeBytes;
        }
        state = toAssetState(download);
    }

    private static OfflineManager.AssetDownloadState toAssetState(Download download) {
        switch (download.state) {
            case Download.STATE_COMPLETED:
                return OfflineManager.AssetDownloadState.completed;
            case Download.STATE_DOWNLOADING:
                return OfflineManager.AssetDownloadState.started;
            case Download.STATE_FAILED:
                return OfflineManager.AssetDownloadState.failed;
            case Download.STATE_QUEUED:
                return OfflineManager.AssetDownloadState.started;
            case Download.STATE_REMOVING:
                return OfflineManager.AssetDownloadState.removing;
            case Download.STATE_RESTARTING:
                return OfflineManager.AssetDownloadState.started;  // TODO: is this the same?
            case Download.STATE_STOPPED:
                if (download.stopReason == StopReason.prefetchDone.toExoCode()) {
                    return OfflineManager.AssetDownloadState.prefetched;
                }
                return OfflineManager.AssetDownloadState.paused;
        }
        return null;
    }

    @Override
    public void release() {
        if (downloadHelper != null) {
            downloadHelper.release();
        }
    }

    @NonNull
    @Override
    public String getAssetId() {
        return assetId;
    }

    @NonNull
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
