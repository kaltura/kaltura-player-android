package com.kaltura.tvplayer.offline.exo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadHelper;
import com.kaltura.tvplayer.OfflineManager;

class ExoAssetInfo extends OfflineManager.AssetInfo {

    private final String assetId;
    private final OfflineManager.AssetDownloadState state;
    private final float percentDownloaded;
    private final long estimatedSize;
    private final long bytesDownloaded;
    OfflineManager.DownloadType downloadType;
    private final Long downloadTime;
    PrefetchConfig prefetchConfig;
    private static final Gson gson = new Gson();

    @Nullable
    final DownloadHelper downloadHelper;  // Only used during preparation

    ExoAssetInfo(OfflineManager.DownloadType downloadType, String assetId, PrefetchConfig prefetchConfig, OfflineManager.AssetDownloadState state, long estimatedSize, long bytesDownloaded, @SuppressWarnings("NullableProblems") DownloadHelper downloadHelper) {
        this.downloadType = downloadType;
        this.assetId = assetId;
        this.state = state;
        this.estimatedSize = estimatedSize;
        this.bytesDownloaded = bytesDownloaded;
        this.downloadHelper = downloadHelper;
        this.percentDownloaded = 0;
        this.prefetchConfig = prefetchConfig;
        this.downloadTime = System.currentTimeMillis();
    }

    ExoAssetInfo(OfflineManager.DownloadType downloadType, String assetId, OfflineManager.AssetDownloadState state, long estimatedSize, long bytesDownloaded, @SuppressWarnings("NullableProblems") DownloadHelper downloadHelper) {
        this.downloadType = downloadType;
        this.assetId = assetId;
        this.state = state;
        this.estimatedSize = estimatedSize;
        this.bytesDownloaded = bytesDownloaded;
        this.downloadHelper = downloadHelper;
        this.percentDownloaded = 0;
        this.prefetchConfig = null;
        this.downloadTime = System.currentTimeMillis();
    }

    ExoAssetInfo(Download download) {
        assetId = download.request.id;
        downloadHelper = null;
        percentDownloaded = download.getPercentDownloaded();
        bytesDownloaded = download.getBytesDownloaded();
        downloadTime = System.currentTimeMillis();

        JsonObject jsonObject = JsonParser.parseString(new String(download.request.data)).getAsJsonObject();
        if (jsonObject != null && jsonObject.has("prefetchConfig")) {
            String prefetchConfigStr = jsonObject.get("prefetchConfig").getAsString();
            if (prefetchConfigStr != null) {
                prefetchConfig = gson.fromJson(prefetchConfigStr, PrefetchConfig.class);
            } else {
                this.prefetchConfig = null;
            }
        } else {
            this.prefetchConfig = null;
        }

        if (prefetchConfig != null) {
            downloadType = OfflineManager.DownloadType.PREFETCH;
        } else {
            downloadType = OfflineManager.DownloadType.FULL;
        }

        if (download.contentLength > 0) {
            estimatedSize = download.contentLength;
        } else {
            long estimatedSizeBytes;
            if (jsonObject != null && jsonObject.has("estimatedSizeBytes")) {
                estimatedSizeBytes = jsonObject.get("estimatedSizeBytes").getAsLong();
            } else {
                estimatedSizeBytes = (long) (100 * bytesDownloaded / percentDownloaded);
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
    public OfflineManager.DownloadType getDownloadType() {
        return downloadType;
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

    @Override
    public float getPercentDownloaded() {
        return percentDownloaded;
    }

    @Override
    public PrefetchConfig getPrefetchConfig() {
        return prefetchConfig;
    }

    @NonNull
    @Override
    public Long getDownloadTime() {
        return downloadTime;
    }
}
