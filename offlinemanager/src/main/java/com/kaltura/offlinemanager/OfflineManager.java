package com.kaltura.offlinemanager;

import android.support.annotation.NonNull;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.MediaOptions;

import java.io.IOException;
import java.util.List;


public abstract class OfflineManager {

    public static OfflineManager getInstance() {
        return new OfflineManagerImp();
    }

    /**
     * Set the global download state listener, to be notified about state changes.
     * @param listener
     */
    public abstract void setDownloadStateListener(DownloadStateListener listener);

    /**
     * Set the global download progress listener
     * @param listener
     */
    public abstract void setDownloadProgressListener(DownloadProgressListener listener);

    public abstract void pauseDownloads();

    public abstract void resumeDownloads();




    /**
     * Find entry by id. Returns null if the entry is not found.
     * @param entryId
     * @return
     */
    public abstract EntryMetadata getEntryMetadata(String entryId);

    /**
     * Get an entry's PKMediaEntry object, as stored by {@link #addEntry(PKMediaEntry)} or
     * {@link #addEntry(int, String, String, MediaOptions)}.
     * @param entryId
     * @return
     */
    public abstract PKMediaEntry getMediaEntry(String entryId);

    /**
     * Get an offline-playable PKMediaEntry object.
     * @param entryId
     * @return
     */
    public abstract PKMediaEntry getLocalEntry(String entryId);

    /**
     * Add a new entry with information from mediaEntry. All relevant information from mediaEntry is
     * stored, but only for the selection download source.
     * @param mediaEntry
     * @return
     */
    public abstract EntryMetadata addEntry(PKMediaEntry mediaEntry);

    /**
     * Add a new entry by connecting to the backend with the provided details.
     * @param partnerId
     * @param ks
     * @param serverUrl
     * @param mediaOptions
     * @return a reference to the new entry or null if the entry already exists.
     */
    public abstract EntryMetadata addEntry(int partnerId, String ks, String serverUrl, MediaOptions mediaOptions);

    /**
     * Load entry's metadata (tracks, size, etc).
     * @param entryId
     * @param listener
     * @return false if entry is not found.
     */
    public abstract boolean loadMetadata(String entryId, MediaSelection selection, MetadataListener listener);


    /**
     * Start (or resume) downloading an entry.
     * @param entryId
     * @return false if entry is not found.
     */
    public abstract boolean startDownload(String entryId);

    /**
     * Pause downloading an entry.
     * @param entryId
     * @return false if entry is not found.
     */
    abstract boolean pauseDownload(String entryId);

    /**
     * Remove entry with all its data.
     * @param entryId
     * @return false if entry is not found.
     */
    abstract boolean removeEntry(String entryId);

    /**
     * Check the license status of an entry.
     * @param entryId
     * @return false if entry is not found.
     */
    abstract boolean checkDrmLicense(String entryId);

    /**
     * Renew an entry's license.
     * @param entryId
     * @return false if entry is not found.
     */
    abstract boolean renewDrmLicense(String entryId);

    public interface MetadataListener {
        void onTracksAvailable(String entryId, TrackSelector trackSelector);
        void onMetadataReady(String entryId, EntryMetadata entryMetadata);
    }

    public interface DownloadProgressListener {
        void onDownloadProgress(String entryId, long downloadedBytes, long totalEstimatedBytes);
    }

    public interface DownloadStateListener {
        void onDownloadStateChange(String entryId, EntryMetadata metadata, EntryDownloadState downloadState);
    }

    public interface DRMListener {
        void onLicenceInstalled(String entryId);
        void onLicenseRemoved(String entryId);
        void onLicenseRenewed(String entryId);
    }

    public interface TrackSelector {
        List<Track> getAvailableTracks(@NonNull TrackType type);
        List<Track> getDownloadedTracks(@NonNull TrackType type);
        void setSelectedTracks(@NonNull TrackType type, @NonNull List<Track> tracks);
        void apply() throws IOException;
    }

    public interface Track {
        TrackType getType();
        String getLanguage();
        long getBitrate();
        CodecType getCodec();
        int getWidth();
        int getHeight();
    }

    public interface EntryMetadata {
        String id();
        String url();
        Long estimatedSize();
        Long downloadedSize();
        EntryDownloadState state();
    }

    public enum EntryDownloadState {
        added, metadataLoaded, started, paused, completed, failed
    }

    public enum TrackType {
        video, audio, text
    }

    public enum CodecType {
        avc, hevc, vp9
    }

    public class MediaSelection {
        public Long minVideoBitrate;    // Download the minimal bitrate that is at least that large
        public Long minVideoHeight;     // Download the minimal height that is at least that large
        public Long minVideoWidth;      // Download the minimal width that is at least that large
        public CodecType preferredVideoCodec;  // Override codec selection (not recommended)

        public List<String> preferredAudioLanguages;
        public List<String> preferredTextLanguages;

        public MediaSelection setMinVideoBitrate(Long minVideoBitrate) {
            this.minVideoBitrate = minVideoBitrate;
            return this;
        }

        public MediaSelection setMinVideoHeight(Long minVideoHeight) {
            this.minVideoHeight = minVideoHeight;
            return this;
        }

        public MediaSelection setMinVideoWidth(Long minVideoWidth) {
            this.minVideoWidth = minVideoWidth;
            return this;
        }

        public MediaSelection setPreferredVideoCodec(CodecType preferredVideoCodec) {
            this.preferredVideoCodec = preferredVideoCodec;
            return this;
        }

        public MediaSelection setPreferredAudioLanguages(List<String> preferredAudioLanguages) {
            this.preferredAudioLanguages = preferredAudioLanguages;
            return this;
        }

        public MediaSelection setPreferredTextLanguages(List<String> preferredTextLanguages) {
            this.preferredTextLanguages = preferredTextLanguages;
            return this;
        }
    }
}
