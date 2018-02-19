package com.kaltura.offlinemanager;

import com.kaltura.dtg.DownloadState;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.MediaOptions;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused", "JavaDoc"})
public abstract class OfflineManager {

    public static OfflineManager getInstance() {
        return new OfflineManagerImp();
    }

    /**
     * Set the global download state listener, to be notified about state changes.
     * @param listener
     */
    public abstract void setAssetInfoUpdateListener(AssetInfoUpdateListener listener);

    /**
     * Set the global download progress listener
     * @param listener
     */
    public abstract void setDownloadProgressListener(DownloadProgressListener listener);

    /**
     * Set the global DRM license update listener.
     * @param listener
     * @see DrmLicenseUpdateListener
     */
    public abstract void setDrmLicenseUpdateListener(DrmLicenseUpdateListener listener);

    /**
     * Temporarily pause all downloads. Doesn't change assets' download state. Revert with {@link #resumeDownloads()}.
     */
    public abstract void pauseDownloads();

    /**
     * Opposite of {@link #pauseDownloads()}.
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
    public abstract List<AssetInfo> getAssetsByState(DownloadState state);

    /**
     * Get an asset's PKMediaEntry object, as stored by {@link #addAsset(PKMediaEntry)} or
     * {@link #addAsset(int, String, String, MediaOptions)}.
     * @param assetId
     * @return
     */
    public abstract PKMediaEntry getOriginalMediaEntry(String assetId);

    /**
     * Get an offline-playable PKMediaEntry object.
     * @param assetId
     * @return
     */
    public abstract PKMediaEntry getLocalPlaybackEntry(String assetId);

    /**
     * Add a new asset with information from mediaEntry. All relevant metadata from mediaEntry is
     * stored, but only for the selection download source.
     * @param mediaEntry
     * @return true if the asset was added, false otherwise. Note: returns false if asset already exists.
     */
    public abstract boolean addAsset(PKMediaEntry mediaEntry);

    /**
     * Add a new asset by connecting to the backend with the provided details.
     * @param partnerId
     * @param ks
     * @param serverUrl
     * @param mediaOptions
     * @return true if the asset was added, false otherwise. Note: returns false if asset already exists.
     */
    public abstract boolean addAsset(int partnerId, String ks, String serverUrl, MediaOptions mediaOptions);

    /**
     * Load asset's metadata (tracks, size, etc).
     * @param assetId
     * @param trackSelectionListener
     * @param assetInfoUpdateListener
     * @return false if asset is not found.
     */
    public abstract boolean loadAssetDownloadInfo(String assetId, MediaPrefs selection,
                                                  TrackSelectionListener trackSelectionListener,
                                                  AssetInfoUpdateListener assetInfoUpdateListener);


    /**
     * Start (or resume) downloading an asset.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean startDownload(String assetId);

    /**
     * Pause downloading an asset.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean pauseDownload(String assetId);

    /**
     * Remove asset with all its data.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean removeEntry(String assetId);

    /**
     * Check the license status of an asset.
     * @param assetId
     * @return DRM license status - {@link AssetDrmInfo}.
     */
    public abstract AssetDrmInfo getDrmStatus(String assetId);

    /**
     * Renew an asset's license. The result is passed asynchronously to the global {@link DrmLicenseUpdateListener}.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean renewAssetDrmLicense(String assetId);

    /**
     * Invoked during asset info loading ({@link #loadAssetDownloadInfo(String, MediaPrefs, TrackSelectionListener, AssetInfoUpdateListener)}).
     * Allows the app to inspect and change the track selection.
     * @see {@link MediaPrefs} for higher-level track selection customization.
     */
    public interface TrackSelectionListener {
        void onTracksAvailable(String assetId, TrackSelector trackSelector);
    }

    /**
     * Invoked while downloading an asset; use with {@link #setDownloadProgressListener(DownloadProgressListener)}.
     */
    public interface DownloadProgressListener {
        void onDownloadProgress(String assetId, long downloadedBytes, long totalEstimatedBytes);
    }

    /**
     * Invoked during asset info loading ({@link #loadAssetDownloadInfo(String, MediaPrefs, TrackSelectionListener, AssetInfoUpdateListener)}).
     */
    public interface AssetInfoUpdateListener {
        void onAssetInfoUpdated(AssetInfo assetInfo);
    }

    /**
     * Global listener for DRM actions. Use with {@link #setDrmLicenseUpdateListener(DrmLicenseUpdateListener)}.
     */
    public interface DrmLicenseUpdateListener {
        void onLicenceInstalled(String assetId, int totalTime, int timeToRenew);
        void onLicenseRenewed(String assetId, int totalTime, int timeToRenew);
        void onLicenseRemoved(String assetId);
    }

    public static class AssetDrmInfo {
        public Status status;
        public int totalRemainingTime;
        public int currentRemainingTime;

        public enum Status {
            valid, unknown, expired, clear
        }
    }

    public interface TrackSelector {
        List<Track> getAvailableTracks(TrackType type);
        List<Track> getSelectedTracks(TrackType type);
        void setSelectedTracks(TrackType type, List<Track> tracks);
    }

    public static class Track {
        TrackType type;
        String language;
        CodecType codec;
        long bitrate;
        int width;
        int height;
    }

    public static class AssetInfo {
        String id;
        AssetDownloadState state;
        long estimatedSize;
        long downloadedSize;
    }

    public enum AssetDownloadState {
        added, metadataLoaded, started, paused, completed, failed
    }

    public enum TrackType {
        video, audio, text
    }

    public enum CodecType {
        avc, hevc, vp9
    }

    /**
     * Pre-download media preferences. Used with {@link #loadAssetDownloadInfo(String, MediaPrefs, TrackSelectionListener, AssetInfoUpdateListener)}.
     */
    public static class MediaPrefs {
        public Long preferredVideoBitrate;
        public Long preferredVideoHeight;
        public Long preferredVideoWidth;

        public List<String> preferredAudioLanguages;
        public List<String> preferredTextLanguages;

        public MediaPrefs setPreferredVideoBitrate(Long preferredVideoBitrate) {
            this.preferredVideoBitrate = preferredVideoBitrate;
            return this;
        }

        public MediaPrefs setPreferredVideoHeight(Long preferredVideoHeight) {
            this.preferredVideoHeight = preferredVideoHeight;
            return this;
        }

        public MediaPrefs setPreferredVideoWidth(Long preferredVideoWidth) {
            this.preferredVideoWidth = preferredVideoWidth;
            return this;
        }

        public MediaPrefs setPreferredAudioLanguages(List<String> preferredAudioLanguages) {
            this.preferredAudioLanguages = preferredAudioLanguages;
            return this;
        }

        public MediaPrefs setPreferredTextLanguages(List<String> preferredTextLanguages) {
            this.preferredTextLanguages = preferredTextLanguages;
            return this;
        }
    }
}
