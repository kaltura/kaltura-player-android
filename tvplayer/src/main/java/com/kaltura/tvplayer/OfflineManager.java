package com.kaltura.tvplayer;

import android.content.Context;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.tvplayer.offline.ExoOfflineManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"WeakerAccess", "unused", "JavaDoc"})
public abstract class OfflineManager {

    public static OfflineManager getInstance(Context context) {
        return ExoOfflineManager.getInstance(context);
    }


    /**
     * Sets the server URL used with {@link #prepareAsset(MediaOptions, SelectionPrefs, PrepareCallback)}.
     *
     * @param url
     */
    public abstract void setKalturaServerUrl(String url);

    /**
     * Sets the partner id used with {@link #prepareAsset(MediaOptions, SelectionPrefs, PrepareCallback)}.
     *
     * @param partnerId
     */
    public abstract void setKalturaPartnerId(int partnerId);

    /**
     * Set the global download state listener, to be notified about state changes.
     *
     * @param listener
     */
    public abstract void setAssetStateListener(AssetStateListener listener);

    /**
     * Set the global download progress listener
     *
     * @param listener
     */
    public abstract void setDownloadProgressListener(DownloadProgressListener listener);


    /**
     * Temporarily pause all downloads. Doesn't change assets' download state. Revert with {@link #resumeDownloads()}.
     */
    public abstract void pauseDownloads();

    /**
     * Resume downloading assets.
     */
    public abstract void resumeDownloads();


    /**
     * Prepare an asset for download. Select the best source from the entry, load the source metadata, select tracks
     * based on the prefs, call the listener.
     *
     * @param mediaEntry
     * @param prefs
     * @param prepareCallback
     */
    public abstract void prepareAsset(PKMediaEntry mediaEntry,
                                      SelectionPrefs prefs, PrepareCallback prepareCallback);

    /**
     * Prepare an asset for download. Connect to Kaltura Backend to load entry metadata, select the best source from
     * the entry, load the source metadata, select tracks based on the prefs, call the listener. If the asset requires
     * KS, make sure to set {@link MediaOptions#ks}.
     * Before calling this method, the partner id and the server URL must be set by {@link #setKalturaPartnerId(int)}
     * and {@link #setKalturaServerUrl(String)}, respectively.
     *
     * @param mediaOptions
     * @param prefs
     * @param prepareCallback
     * @throws IllegalStateException if partner id and/or server URL were not set.
     */
    public abstract void prepareAsset(MediaOptions mediaOptions,
                                      SelectionPrefs prefs, PrepareCallback prepareCallback)
            throws IllegalStateException;

    /**
     * Add a prepared asset to the db and start downloading it.
     */
    public abstract void startAssetDownload(AssetInfo assetInfo);

    /**
     * Pause downloading an asset. Resume by calling {@link #resumeAssetDownload(String)}.
     *
     * @param assetId
     */
    public abstract void pauseAssetDownload(String assetId);

    /**
     * Resume a download that was paused by {@link #pauseAssetDownload(String)}.
     *
     * @param assetId
     */
    public abstract void resumeAssetDownload(String assetId);

    /**
     * Remove asset with all its data.
     *
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean removeAsset(String assetId);


    /**
     * Find asset by id.
     *
     * @param assetId
     * @return asset info or null if not found.
     */
    public abstract AssetInfo getAssetInfo(String assetId);

    /**
     * Get list of {@link AssetInfo} objects for all assets in the given state.
     *
     * @param state
     * @return
     */
    public abstract List<AssetInfo> getAssetsInState(AssetDownloadState state);

    /**
     * Get an offline-playable PKMediaEntry object.
     *
     * @param assetId
     * @return
     */
    public abstract PKMediaEntry getLocalPlaybackEntry(String assetId) throws IOException;

    /**
     * Send a downloaded asset to the player.
     *
     * @param assetId
     * @param player
     */
    public abstract void sendAssetToPlayer(String assetId, KalturaPlayer player) throws IOException;


    /**
     * Check the license status of an asset.
     *
     * @param assetId
     * @return DRM license status - {@link DrmInfo}.
     */
    public abstract DrmInfo getDrmStatus(String assetId);


    /**
     * Renew an asset's license.
     *
     * @param assetId
     * @param drmParams
     * @param listener
     * @return false if asset is not found, true otherwise.
     */
    public abstract void renewDrmAsset(String assetId, PKDrmParams drmParams, DrmListener listener);

    public abstract void setKs(String ks);

    public abstract void setPreferredMediaFormat(PKMediaFormat preferredMediaFormat);


    public enum AssetDownloadState {
        none, downloading, queued, completed, failed, removing, stopped
    }

    public enum TrackType {
        video, audio, text
    }

    public enum CodecType {
        avc, hevc, vp9
    }

    /**
     * Invoked during asset info loading ({@link #prepareAsset(PKMediaEntry, SelectionPrefs, PrepareCallback)}).
     * Allows the app to inspect and change the track selection. If app returns non-null, it overrides the automatic selection.
     *
     * @see {@link SelectionPrefs} for higher-level track selection customization.
     */
    public interface PrepareCallback {
        void onPrepared(AssetInfo assetInfo, Map<TrackType, List<Track>> selected);

        void onPrepareError(Exception error);
    }

    /**
     * Invoked while downloading an asset; use with {@link #setDownloadProgressListener(DownloadProgressListener)}.
     */
    public interface DownloadProgressListener {
        void onDownloadProgress(String assetId, long downloadedBytes, long totalEstimatedBytes);
    }

    /**
     * Listener for DRM register events.
     */
    public interface DrmListener {
        void onRegistered(String assetId, DrmInfo drmInfo);
        void onRegisterError(String assetId, Exception error);
    }

    public interface AssetStateListener extends DrmListener {
        void onStateChanged(String assetId, AssetInfo assetInfo);
        void onAssetRemoved(String assetId);
        void onAssetDownloadFailed(String assetId, AssetDownloadException error);
        void onAssetDownloadComplete(String assetId);
        void onAssetDownloadPending(String assetId);
        void onAssetDownloadPaused(String assetId);
    }

    public static class DrmInfo {

        public static final DrmInfo clear = new DrmInfo(Status.clear, 0, 0);
        public static final DrmInfo unknown = new DrmInfo(Status.unknown, 0, 0);

        public static DrmInfo withDrm(long currentRemainingTime, long totalRemainingTime) {
            return new DrmInfo(currentRemainingTime > 0 ? Status.valid : Status.expired, currentRemainingTime, totalRemainingTime);
        }

        public final Status status;
        public final long currentRemainingTime;
        public final long totalRemainingTime;

        public boolean isValid() {
            return status == Status.valid || status == Status.clear;
        }

        public enum Status {
            valid, expired, clear, unknown
        }

        private DrmInfo(Status status, long currentRemainingTime, long totalRemainingTime) {
            this.status = status;
            this.currentRemainingTime = currentRemainingTime;
            this.totalRemainingTime = totalRemainingTime;
        }

        @Override
        public String toString() {
            return "DrmInfo{" +
                    "status=" + status +
                    ", currentRemainingTime=" + currentRemainingTime +
                    ", totalRemainingTime=" + totalRemainingTime +
                    '}';
        }
    }

    public static class Track {
        TrackType type;
        String language;
        CodecType codec;
        long bitrate;
        int width;
        int height;
    }

    /**
     * Pre-download media preferences. Used with {@link #prepareAsset(PKMediaEntry, SelectionPrefs, PrepareCallback)}.
     */
    public static class SelectionPrefs {
        public Long preferredVideoBitrate;
        public Long preferredVideoHeight;
        public Long preferredVideoWidth;

        public List<String> preferredAudioLanguages;
        public List<String> preferredTextLanguages;

        public SelectionPrefs setPreferredVideoBitrate(Long preferredVideoBitrate) {
            this.preferredVideoBitrate = preferredVideoBitrate;
            return this;
        }

        public SelectionPrefs setPreferredVideoHeight(Long preferredVideoHeight) {
            this.preferredVideoHeight = preferredVideoHeight;
            return this;
        }

        public SelectionPrefs setPreferredVideoWidth(Long preferredVideoWidth) {
            this.preferredVideoWidth = preferredVideoWidth;
            return this;
        }

        public SelectionPrefs setPreferredAudioLanguages(List<String> preferredAudioLanguages) {
            this.preferredAudioLanguages = preferredAudioLanguages;
            return this;
        }

        public SelectionPrefs setPreferredTextLanguages(List<String> preferredTextLanguages) {
            this.preferredTextLanguages = preferredTextLanguages;
            return this;
        }
    }

    public static abstract class AssetInfo {

        public abstract void release();

        public abstract String getAssetId();

        public abstract AssetDownloadState getState();

        public abstract long getEstimatedSize();

        public abstract long getBytesDownloaded();
    }

    public static class AssetDownloadException extends Exception {
        public AssetDownloadException(String message) {
            super(message);
        }
    }

}
