package com.kaltura.tvplayer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.tvplayer.offline.exo.ExoOfflineManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
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


    public abstract void start(ManagerStartCallback callback) throws IOException;

    public abstract void stop();

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
     * Renew an asset's license.
     *
     * @param assetId
     * @param drmParams
     * @return false if asset is not found, true otherwise.
     */
    public abstract void renewDrmAsset(String assetId, PKDrmParams drmParams);

    public abstract void renewDrmAsset(String assetId, MediaOptions mediaOptions, MediaEntryCallback mediaEntryCallback);


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
     * @return DRM license status - {@link DrmStatus}.
     */
    public abstract DrmStatus getDrmStatus(String assetId);


    public abstract void setKs(String ks);

    public abstract void setPreferredMediaFormat(PKMediaFormat preferredMediaFormat);

    public abstract void setEstimatedHlsAudioBitrate(int bitrate);

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
     * Event callbacks invoked during asset info loading ({@link #prepareAsset(PKMediaEntry, SelectionPrefs, PrepareCallback)})
     * or {@link #prepareAsset(MediaOptions, SelectionPrefs, PrepareCallback)}).
     * The app MUST handle at least {@link #onPrepared(String, AssetInfo, Map)} and {@link #onPrepareError(String, Exception)}. If the
     * app has used {@link #prepareAsset(MediaOptions, SelectionPrefs, PrepareCallback)}, it MUST also handle
     * {@link #onMediaEntryLoadError(Exception)}.
     */
    public interface PrepareCallback extends MediaEntryCallback {
        /**
         * Called when the asset is ready to be downloaded. The app should either call {@link #startAssetDownload(AssetInfo)}
         * to start the download or call {@link AssetInfo#release()} if it elected NOT to download the prepared asset.
         * Must be handled by all applications.
         * @param assetId
         * @param assetInfo
         * @param selected
         */
        void onPrepared(@NonNull String assetId, @NonNull AssetInfo assetInfo, @Nullable Map<TrackType, List<Track>> selected);

        /**
         * Called when asset preparation has failed for some reason.
         * Must be handled by all applications.
         * @param assetId
         * @param error
         */
        void onPrepareError(@NonNull String assetId, Exception error);

        /**
         * Called when loading a {@link PKMediaEntry} object from the backend has succeeded. It allows the app to
         * inspect and possibly modify the entry before it is actually prepared for download.
         * This method is only called when using {@link #prepareAsset(MediaOptions, SelectionPrefs, PrepareCallback)}
         * and doesn't have to handled by apps that don't use this variant of prepareAsset().
         * @param assetId
         * @param mediaEntry
         */
        @Override
        default void onMediaEntryLoaded(@NonNull String assetId, @NonNull PKMediaEntry mediaEntry) {}

        /**
         * Called when loading a {@link PKMediaEntry} object from the backend has failed.
         * This method is only called when using {@link #prepareAsset(MediaOptions, SelectionPrefs, PrepareCallback)}
         * and doesn't have to handled by apps that don't use this variant of prepareAsset(). Apps that DO use it,
         * MUST handle it because the preparation process halts if it's called.
         * @param error
         */
        @Override
        default void onMediaEntryLoadError(Exception error) {}

        /**
         * Called when prepareAsset() has selected a specific {@link PKMediaSource} from the provided or loaded
         * {@link PKMediaEntry}.
         * If drmParams is not null, it contains the selected DRM parameters for the source.
         * @param assetId
         * @param source
         * @param drmParams
         */
        default void onSourceSelected(@NonNull String assetId, @NonNull PKMediaSource source, @Nullable PKDrmParams drmParams) {}
    }

    /**
     * Invoked while downloading an asset; use with {@link #setDownloadProgressListener(DownloadProgressListener)}.
     */
    public interface DownloadProgressListener {
        void onDownloadProgress(String assetId, long bytesDownloaded, long totalBytesEstimated, float percentDownloaded);
    }

    public interface AssetStateListener {
        void onStateChanged(@NonNull String assetId, @NonNull AssetInfo assetInfo);
        void onAssetRemoved(@NonNull String assetId);
        void onAssetDownloadFailed(@NonNull String assetId, @Nullable Exception error);
        void onAssetDownloadComplete(@NonNull String assetId);
        void onAssetDownloadPending(@NonNull String assetId);
        void onAssetDownloadPaused(@NonNull String assetId);
        void onRegistered(@NonNull String assetId, DrmStatus drmStatus);
        void onRegisterError(@NonNull String assetId, @Nullable Exception error);
    }

    public interface ManagerStartCallback {
        void onStarted();
    }

    public static class DrmStatus {

        public static final DrmStatus clear = new DrmStatus(Status.clear, 0, 0);
        public static final DrmStatus unknown = new DrmStatus(Status.unknown, 0, 0);

        public static DrmStatus withDrm(long currentRemainingTime, long totalRemainingTime) {
            return new DrmStatus(currentRemainingTime > 0 ? Status.valid : Status.expired, currentRemainingTime, totalRemainingTime);
        }

        public final Status status;
        public final long currentRemainingTime;
        public final long totalRemainingTime;

        public boolean isValid() {
            return status == Status.valid || status == Status.clear;
        }

        public boolean isClear() {
            return status == Status.clear;
        }

        public enum Status {
            valid, expired, clear, unknown
        }

        private DrmStatus(Status status, long currentRemainingTime, long totalRemainingTime) {
            this.status = status;
            this.currentRemainingTime = currentRemainingTime;
            this.totalRemainingTime = totalRemainingTime;
        }

        @Override
        public String toString() {

            long value = currentRemainingTime;
            long ss = value % 60;
            value /= 60;
            long mm = value % 60;
            value /= 60;
            long hh = value % 24;
            value /= 24;
            long dd = value;

            final String format = String.format(Locale.ROOT, "%d+%02d:%02d:%02d", dd, hh, mm, ss);

            return "DrmInfo{" +
                    "status=" + status +
                    ", rem. time=" + format +
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

    public interface MediaEntryCallback {
        void onMediaEntryLoaded(@NonNull String assetId, @NonNull PKMediaEntry mediaEntry);

        void onMediaEntryLoadError(Exception error);
    }
}
