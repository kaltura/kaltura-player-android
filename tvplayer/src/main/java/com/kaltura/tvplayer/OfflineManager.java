package com.kaltura.tvplayer;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.upstream.cache.Cache;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.tvplayer.config.TVPlayerParams;
import com.kaltura.tvplayer.offline.Prefetch;
import com.kaltura.tvplayer.offline.OfflineManagerSettings;
import com.kaltura.tvplayer.offline.dtg.DTGOfflineManager;
import com.kaltura.tvplayer.offline.exo.ExoOfflineManager;
import com.kaltura.tvplayer.offline.exo.PrefetchConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@SuppressWarnings({"WeakerAccess", "unused", "JavaDoc"})
public abstract class OfflineManager {

    protected String kalturaServerUrl = KalturaPlayer.DEFAULT_OVP_SERVER_URL;
    protected Integer kalturaPartnerId;
    protected String referrer;

    public enum OfflineProvider {
        DTG,
        EXO
    }

    public static @NonNull OfflineManager getInstance(Context context, OfflineProvider offlineProvider) {
        switch(offlineProvider) {
            case EXO:
                return ExoOfflineManager.getInstance(context);
            case DTG:
            default:
                return  DTGOfflineManager.getInstance(context);
        }
    }

    public void setKalturaParams(KalturaPlayer.Type type, int partnerId) {
        this.kalturaPartnerId = partnerId;
        final TVPlayerParams params = PlayerConfigManager.retrieve(type, partnerId);
        if (params != null) {
            this.kalturaServerUrl = params.serviceUrl;
        }
    }

    public void setKalturaServerUrl(String url) {
        this.kalturaServerUrl = url;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    /**
     * Set the global download state listener, to be notified about state changes.
     *
     * @param listener setAssetStateListener
     */
    public abstract void setAssetStateListener(@Nullable AssetStateListener listener);

    /**
     * Set the global download progress listener
     *
     * @param listener setDownloadProgressListener
     */
    public abstract void setDownloadProgressListener(@Nullable DownloadProgressListener listener);

    public abstract void start(@Nullable ManagerStartCallback callback) throws IOException;

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
     * Cancel currently downloading assets.
     */
    public abstract void cancelDownloads();

    /**
     * Prepare an asset for download. Select the best source from the entry, load the source metadata, select tracks
     * based on the prefs, call the listener.
     *
     * @param mediaEntry
     * @param selectionPrefs
     * @param prepareCallback
     */
    public abstract void prepareAsset(@NonNull PKMediaEntry mediaEntry,
                                      @NonNull SelectionPrefs selectionPrefs,
                                      @NonNull PrepareCallback prepareCallback);

    /**
     * Prepare an asset for download. Connect to Kaltura Backend to load entry metadata, select the best source from
     * the entry, load the source metadata, select tracks based on the prefs, call the listener. If the asset requires
     * KS, make sure to set {@link MediaOptions}.
     * Before calling this method, the partner id and the server URL must be set by {@link #setKalturaParams(KalturaPlayer.Type, int)}
     * and {@link #setKalturaServerUrl(String)}, respectively.
     *
     * @param mediaOptions
     * @param selectionPrefs
     * @param prepareCallback
     * @throws IllegalStateException if partner id and/or server URL were not set.
     */
    public abstract void prepareAsset(@NonNull MediaOptions mediaOptions,
                                      @NonNull SelectionPrefs selectionPrefs,
                                      @NonNull PrepareCallback prepareCallback)
            throws IllegalStateException;

    /**
     * Add a prepared asset to the db and start downloading it.
     * @param assetInfo AssetInfo
     */
    public abstract void startAssetDownload(@NonNull AssetInfo assetInfo) throws IllegalArgumentException;

    /**
     * Pause downloading an asset. Resume by calling {@link #resumeAssetDownload(String)}.
     *
     * @param assetId String
     */
    public abstract void pauseAssetDownload(@NonNull String assetId);

    /**
     * Resume a download that was paused by {@link #pauseAssetDownload(String)}.
     *
     * @param assetId String
     */
    public abstract void resumeAssetDownload(@NonNull String assetId);

    /**
     * Remove asset with all its data.
     *
     * @param assetId String
     * @return false if asset is not found, true otherwise.
     */
    public abstract boolean removeAsset(@NonNull String assetId);


    /**
     * Renew an asset's license.
     *
     * @param assetId String
     * @param drmParams PKDrmParams
     */
    public abstract void renewDrmAssetLicense(@NonNull String assetId,
                                              @NonNull PKDrmParams drmParams);

    public abstract void renewDrmAssetLicense(@NonNull String assetId,
                                              @NonNull MediaOptions mediaOptions,
                                              @NonNull MediaEntryCallback mediaEntryCallback);


    /**
     * Find asset by id.
     *
     * @param assetId String
     * @return asset info or null if not found.
     */
    public abstract @Nullable AssetInfo getAssetInfo(@NonNull String assetId);

    public abstract @Nullable File getDownloadDirectory();

    public abstract @Nullable Cache getDownloadCache();

    public abstract @Nullable DatabaseProvider getDatabaseProvider();

    /**
     * Get list of {@link AssetInfo} objects for all assets.
     *
     * @return AssetInfo list
     */
    public abstract @NonNull List<AssetInfo> getAllAssets();

    /**
     * Get list of {@link AssetInfo} objects for all assets in the given state.
     *
     * @param state AssetDownloadState
     * @return AssetInfo list
     */
    public abstract @NonNull List<AssetInfo> getAssetsInState(@NonNull AssetDownloadState state);

    /**
     * Get an offline-playable PKMediaEntry object.
     *
     * @param assetId String
     * @return PKMediaEntry
     * @throws IOException io exception
     */
    public abstract @NonNull PKMediaEntry getLocalPlaybackEntry(@NonNull String assetId) throws IOException;

    /**
     * Check the license status of an asset.
     *
     * @param assetId String
     * @return DRM license status - {@link DrmStatus}.
     */
    public abstract @NonNull DrmStatus getDrmStatus(@NonNull String assetId);

    public abstract void setKs(@Nullable String ks);

    public abstract void setPreferredMediaFormat(@Nullable PKMediaFormat preferredMediaFormat);

    public abstract void setForceWidevineL3Playback(boolean forceWidevineL3Playback);

    /**
     * Settings, may be set for downloading the assets.
     * This setter only has effect if called before {@link #start(ManagerStartCallback)} )}.
     * @param offlineManagerSettings OfflineManagerSettings
     */
    public abstract void setOfflineManagerSettings(@NonNull OfflineManagerSettings offlineManagerSettings);

    public abstract Prefetch getPrefetchManager();

    public abstract void setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter);

    public String getKalturaServerUrl() {
        return kalturaServerUrl;
    }

    public Integer getKalturaPartnerId() {
        return kalturaPartnerId;
    }

    public enum AssetDownloadState {
        none, prepared, started, prefetched, completed, failed, removing, paused
    }

    public enum TrackType {
        video, audio, text
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
         * @param assetId String
         * @param assetInfo AssetInfo
         * @param selected trascks
         */
        void onPrepared(@NonNull String assetId, @NonNull AssetInfo assetInfo, @Nullable Map<TrackType, List<Track>> selected);

        /**
         * Called when asset preparation has failed for some reason.
         * Must be handled by all applications.
         * @param assetId String
         * @param error Exception
         */
        void onPrepareError(@NonNull String assetId, @NonNull Exception error);

        /**
         * Called when loading a {@link PKMediaEntry} object from the backend has succeeded. It allows the app to
         * inspect and possibly modify the entry before it is actually prepared for download.
         * This method is only called when using {@link #prepareAsset(MediaOptions, SelectionPrefs, PrepareCallback)}
         * and doesn't have to handled by apps that don't use this variant of prepareAsset().
         * @param assetId String
         * @param mediaEntry PKMediaEntry
         */
        @Override
        default void onMediaEntryLoaded(@NonNull String assetId, @NonNull PKMediaEntry mediaEntry) {}

        /**
         * Called when loading a {@link PKMediaEntry} object from the backend has failed.
         * This method is only called when using {@link #prepareAsset(MediaOptions, SelectionPrefs, PrepareCallback)}
         * and doesn't have to handled by apps that don't use this variant of prepareAsset(). Apps that DO use it,
         * MUST handle it because the preparation process halts if it's called.
         * @param error Exception
         */
        @Override
        default void onMediaEntryLoadError(@NonNull Exception error) {}

        /**
         * Called when prepareAsset() has selected a specific {@link PKMediaSource} from the provided or loaded
         * {@link PKMediaEntry}.
         * If drmParams is not null, it contains the selected DRM parameters for the source.
         * @param assetId String
         * @param source PKMediaSource
         * @param drmParams PKDrmParams
         */
        default void onSourceSelected(@NonNull String assetId, @NonNull PKMediaSource source, @Nullable PKDrmParams drmParams) {}
    }

    /**
     * Invoked while downloading an asset; use with {@link #setDownloadProgressListener(DownloadProgressListener)}.
     */
    public interface DownloadProgressListener {
        void onDownloadProgress(@NonNull String assetId, long bytesDownloaded, long totalBytesEstimated, float percentDownloaded);
    }

    public interface AssetStateListener {
        void onStateChanged(@NonNull String assetId, @NonNull DownloadType downloadType, @NonNull AssetInfo assetInfo);
        void onAssetRemoved(@NonNull String assetId, @NonNull DownloadType downloadType);
        void onAssetRemoveError(@NonNull String assetId, @NonNull DownloadType downloadType, @NonNull Exception error);
        void onAssetDownloadFailed(@NonNull String assetId, @NonNull DownloadType downloadType, @NonNull Exception error);
        void onAssetDownloadComplete(@NonNull String assetId, @NonNull DownloadType downloadType);
        void onAssetPrefetchComplete(@NonNull String assetId, @NonNull DownloadType downloadType);
        void onAssetDownloadPending(@NonNull String assetId, @NonNull DownloadType downloadType);
        void onAssetDownloadPaused(@NonNull String assetId, @NonNull DownloadType downloadType);
        void onRegistered(@NonNull String assetId, @NonNull DrmStatus drmStatus);
        void onRegisterError(@NonNull String assetId, @NonNull DownloadType downloadType, @NonNull Exception error);
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

        @NonNull
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
        String language;
        long bitrate;
        int width;
        int height;
        TrackCodec codec;
    }

    public enum TrackCodec {
        // Video Codecs
        /// AVC1 codec, AKA H.264
        AVC1,
        /// HEVC codec, AKA HVC1 or H.265
        HEVC,


        // Audio Codecs
        /// MP4A
        MP4A,
        /// AC3: Dolby Atmos
        AC3,
        /// E-AC3: Dolby Digital Plus (Enhanced AC3)
        EAC3
    }

    public enum DownloadType {
        UNKNOWM,
        PREFETCH,
        FULL
    }

    /**
     * Pre-download media preferences. Used with {@link #prepareAsset(PKMediaEntry, SelectionPrefs, PrepareCallback)}.
     */
    public static class SelectionPrefs {

        public enum DownloadVideoQuality {
            LOW,
            MEDIUM,
            HIGH
        }
        @Nullable public DownloadVideoQuality downloadVideoQuality;
        public static Pair<Integer,Integer> low = new Pair(1, 350000);
        public static Pair<Integer,Integer> medium = new Pair(350001, 750000);
        public static Pair<Integer,Integer> high = new Pair(750001, 16000000);

        @Nullable public Map<TrackCodec, Integer> codecVideoBitrates;
        @Nullable public List<TrackCodec> videoCodecs;
        @Nullable public List<TrackCodec> audioCodecs;
        @Nullable public Integer videoBitrate;


        @Nullable public Integer videoHeight;
        @Nullable public Integer videoWidth;

        @Nullable public List<String> audioLanguages;
        @Nullable public List<String> textLanguages;

        public boolean allAudioLanguages;
        public boolean allTextLanguages;
        public boolean allowInefficientCodecs;
        public OfflineManager.DownloadType downloadType = DownloadType.FULL;
    }

    public static abstract class AssetInfo {

        public abstract void release();

        @NonNull
        public abstract DownloadType getDownloadType();

        @NonNull
        public abstract String getAssetId();

        @NonNull
        public abstract AssetDownloadState getState();

        public abstract long getEstimatedSize();

        public abstract long getBytesDownloaded();

        public abstract float getPercentDownloaded();

        @Nullable
        public abstract PrefetchConfig getPrefetchConfig();
    }

    public static class AssetDownloadException extends Exception {
        public AssetDownloadException(String message) {
            super(message);
        }
    }

    public interface MediaEntryCallback {
        void onMediaEntryLoaded(@NonNull String assetId, @NonNull PKMediaEntry mediaEntry);

        void onMediaEntryLoadError(@NonNull Exception error);
    }
}
