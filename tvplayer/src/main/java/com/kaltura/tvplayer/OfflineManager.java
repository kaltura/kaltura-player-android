package com.kaltura.tvplayer;

import android.content.Context;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.offline.ExoOfflineManager;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused", "JavaDoc"})
public abstract class OfflineManager {

    public static OfflineManager getInstance(Context context) {
        return ExoOfflineManager.getInstance(context);
    }

    /**
     * Start the download service. Essential for using all other methods.
     * @param listener
     */
    public abstract void startService(ServiceStartListener listener);

    public abstract void stopService();

    /**
     * Set the global download state listener, to be notified about state changes.
     * @param listener
     */
    public abstract void setAssetStateListener(AssetStateListener listener);

    /**
     * Set the global download progress listener
     * @param listener
     */
    public abstract void setDownloadProgressListener(DownloadProgressListener listener);

    /**
     * Temporarily pause all downloads. Doesn't change assets' download state. Revert with {@link #resumeDownloads()}.
     */
    public abstract void pauseDownloads();

    /**
     * Resume downloading assets. Should be called in two places:
     * - After calling {@link #startService(ServiceStartListener)}, to resume the downloads that
     * were in progress in the previous session
     * - After calling {@link #pauseDownloads()}, to resume the paused downloads.
     */
    public abstract void resumeDownloads();

    /**
     * Find asset by id.
     * @param assetId
     * @return asset info or null if not found.
     */
    public abstract AssetInfo getAssetInfo(String assetId);

    /**
     * Get list of {@link AssetInfo} objects for all assets in the given state.
     * @param state
     * @return
     */
    public abstract List<AssetInfo> getAssetsInState(AssetDownloadState state);

    /**
     * Get an offline-playable PKMediaEntry object.
     * @param assetId
     * @return
     */
    public abstract PKMediaEntry getLocalPlaybackEntry(String assetId);

    /**
     * Send a downloaded asset to the player.
     * @param assetId
     * @param player
     */
    public abstract void sendAssetToPlayer(String assetId, KalturaPlayer player);

    /**
     * Prepare an asset for download. Select the best source from the entry, load the source metadata, select tracks
     * based on the prefs, call the listener.
     * @param mediaEntry
     * @param prefs
     * @param prepareListener
     */
    public abstract void prepareAsset(PKMediaEntry mediaEntry, SelectionPrefs prefs,
                                      PrepareListener prepareListener);

    public abstract void prepareAsset(int partnerId, String serverUrl, MediaOptions mediaOptions, SelectionPrefs prefs,
                                      PrepareListener prepareListener);

    /**
     * Add a prepared asset to the db.
     * @return true if the asset was added, false otherwise. Note: returns false if asset already exists.
     */
    public abstract boolean addAsset(AssetInfo assetInfo);

    /**
     * Start (or resume) downloading an asset.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean startAssetDownload(String assetId);

    /**
     * Pause downloading an asset.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean pauseAssetDownload(String assetId);

    /**
     * Remove asset with all its data.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean removeAsset(String assetId);

    /**
     * Check the license status of an asset.
     * @param assetId
     * @return DRM license status - {@link DrmInfo}.
     */
    public abstract DrmInfo getDrmStatus(String assetId);

    /**
     * Register or renew an asset's license. This method requires that the DRM params stored are fresh --
     * if they aren't, use {@link #registerDrmAsset(String, PKDrmParams, DrmRegisterListener)} instead.
     * @param assetId
     * @param listener
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean registerDrmAsset(String assetId, DrmRegisterListener listener);

    /**
     * Register or renew an asset's license.
     * @param assetId
     * @param drmParams
     * @param listener
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean registerDrmAsset(String assetId, PKDrmParams drmParams, DrmRegisterListener listener);

    public interface ServiceStartListener {
        void onServiceStarted();
    }

    /**
     * Invoked during asset info loading ({@link #prepareAsset(PKMediaEntry, SelectionPrefs, PrepareListener)}).
     * Allows the app to inspect and change the track selection. If app returns non-null, it overrides the automatic selection.
     * @see {@link SelectionPrefs} for higher-level track selection customization.
     */
    public interface PrepareListener {
        void onPrepareComplete(AssetInfo assetInfo, Map<TrackType, List<Track>> selected, long estimatedSize);
        void onPrepareFailed(Exception error);
    }

    /**
     * Invoked while downloading an asset; use with {@link #setDownloadProgressListener(DownloadProgressListener)}.
     */
    public interface DownloadProgressListener {
        void onDownloadProgress(String assetId, long downloadedBytes, long totalEstimatedBytes);
    }

    public interface DrmRegisterListener {
        void onRegisterComplete(String assetId, DrmInfo drmInfo);
        void onRegisterFailed(String assetId, Exception error);
    }

    public static class DrmInfo {
        public Status status;
        public int totalRemainingTime;
        public int currentRemainingTime;

        public enum Status {
            valid, unknown, expired, clear
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

    public enum AssetDownloadState {
        added, prepared, started, paused, completed, failed
    }

    public enum TrackType {
        video, audio, text
    }

    public enum CodecType {
        avc, hevc, vp9
    }

    /**
     * Pre-download media preferences. Used with {@link #prepareAsset(PKMediaEntry, SelectionPrefs, PrepareListener)}.
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

    public interface AssetStateListener {
        void onAssetStateChanged(String assetId, AssetInfo assetInfo);
    }

    public static class AssetInfo {
        String id;
        AssetDownloadState state;
        long estimatedSize;
        long downloadedSize;
    }

}
