package com.kaltura.tvplayer

import android.content.Context

import com.kaltura.playkit.PKMediaEntry

abstract class OfflineManager {

    /**
     * Start the download service. Essential for using all other methods.
     * @param appContext
     * @param listener
     */
    abstract fun startService(appContext: Context, listener: OnServiceStart)

    abstract fun stopService()

    /**
     * Set the global download state listener, to be notified about state changes.
     * @param listener
     */
    abstract fun setAssetInfoUpdateListener(listener: AssetInfoUpdateListener)

    /**
     * Set the global download progress listener
     * @param listener
     */
    abstract fun setDownloadProgressListener(listener: DownloadProgressListener)

    /**
     * Set the global DRM license update listener.
     * @param listener
     * @see DrmLicenseUpdateListener
     */
    abstract fun setDrmLicenseUpdateListener(listener: DrmLicenseUpdateListener)

    /**
     * Temporarily pause all downloads. Doesn't change assets' download state. Revert with [.resumeDownloads].
     */
    abstract fun pauseDownloads()

    /**
     * Resume downloading assets. Should be called in two places:
     * - After calling [.startService], to resume the downloads that
     * were in progress in the previous session
     * - After calling [.pauseDownloads], to resume the paused downloads.
     */
    abstract fun resumeDownloads()

    /**
     * Find asset by id.
     * @param assetId
     * @return asset info or null if not found.
     */
    abstract fun getAssetInfo(assetId: String): AssetInfo?

    /**
     * Get list of [AssetInfo] objects for all assets in the given state.
     * @param state
     * @return
     */
    abstract fun getAssetsByState(state: AssetDownloadState): List<AssetInfo>?

    /**
     * Get an asset's PKMediaEntry object, as stored by [.addAsset] or
     * [.addAsset].
     * @param assetId
     * @return
     */
    abstract fun getOriginalMediaEntry(assetId: String): PKMediaEntry?

    /**
     * Get an offline-playable PKMediaEntry object.
     * @param assetId
     * @return
     */
    abstract fun getLocalPlaybackEntry(assetId: String): PKMediaEntry?

    /**
     * Add a new asset with information from mediaEntry. All relevant metadata from mediaEntry is
     * stored, but only for the selection download source.
     * @param mediaEntry
     * @return true if the asset was added, false otherwise. Note: returns false if asset already exists.
     */
    abstract fun addAsset(mediaEntry: PKMediaEntry): Boolean

    /**
     * Add a new asset by connecting to the backend with the provided details.
     * @param partnerId
     * @param ks
     * @param serverUrl
     * @param mediaOptions
     * @return true if the asset was added, false otherwise. Note: returns false if asset already exists.
     */
    abstract fun addAsset(partnerId: Int, ks: String, serverUrl: String, mediaOptions: MediaOptions): Boolean

    /**
     * Load asset's metadata (tracks, size, etc).
     * @param assetId
     * @param trackSelectionListener
     * @param assetInfoUpdateListener
     * @return false if asset is not found.
     */
    abstract fun loadAssetDownloadInfo(
        assetId: String, selection: MediaPrefs,
        trackSelectionListener: TrackSelectionListener,
        assetInfoUpdateListener: AssetInfoUpdateListener
    ): Boolean


    /**
     * Start (or resume) downloading an asset.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    abstract fun startAsset(assetId: String): Boolean

    /**
     * Pause downloading an asset.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    abstract fun pauseAsset(assetId: String): Boolean

    /**
     * Remove asset with all its data.
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    abstract fun removeAsset(assetId: String): Boolean

    /**
     * Check the license status of an asset.
     * @param assetId
     * @return DRM license status - [AssetDrmInfo].
     */
    abstract fun getDrmStatus(assetId: String): AssetDrmInfo?

    /**
     * Renew an asset's license. The result is passed asynchronously to the global [DrmLicenseUpdateListener].
     * @param assetId
     * @return false if asset is not found, true otherwise.
     */
    abstract fun renewAssetDrmLicense(assetId: String): Boolean

    interface OnServiceStart {
        fun onServiceStart()
    }

    /**
     * Invoked during asset info loading ([.loadAssetDownloadInfo]).
     * Allows the app to inspect and change the track selection. If app returns non-null, it overrides the automatic selection.
     * @see {@link MediaPrefs} for higher-level track selection customization.
     */
    interface TrackSelectionListener {
        fun onTracksAvailable(
            assetId: String, available: Map<TrackType, List<Track>>,
            selected: Map<TrackType, List<Track>>
        ): Map<TrackType, List<Track>>
    }

    /**
     * Invoked during asset info loading ([.loadAssetDownloadInfo]).
     */
    interface AssetInfoUpdateListener {
        fun onAssetInfoUpdated(assetId: String, assetInfo: AssetInfo)
    }

    /**
     * Invoked while downloading an asset; use with [.setDownloadProgressListener].
     */
    interface DownloadProgressListener {
        fun onDownloadProgress(assetId: String, downloadedBytes: Long, totalEstimatedBytes: Long)
    }

    /**
     * Global listener for DRM actions. Use with [.setDrmLicenseUpdateListener].
     */
    interface DrmLicenseUpdateListener {
        fun onLicenceInstall(assetId: String, totalTime: Int, timeToRenew: Int)
        fun onLicenseRenew(assetId: String, totalTime: Int, timeToRenew: Int)
        fun onLicenseRemove(assetId: String)
    }

    class AssetDrmInfo {
        var status: Status? = null
        var totalRemainingTime: Int = 0
        var currentRemainingTime: Int = 0

        enum class Status {
            valid, unknown, expired, clear
        }
    }

    class Track {
        internal var type: TrackType? = null
        internal var language: String? = null
        internal var codec: CodecType? = null
        internal var bitrate: Long = 0
        internal var width: Int = 0
        internal var height: Int = 0
    }

    class AssetInfo {
        internal var id: String? = null
        internal var state: AssetDownloadState? = null
        internal var estimatedSize: Long = 0
        internal var downloadedSize: Long = 0
    }

    enum class AssetDownloadState {
        added, metadataLoaded, started, paused, completed, failed
    }

    enum class TrackType {
        video, audio, text
    }

    enum class CodecType {
        avc, hevc, vp9
    }

    /**
     * Pre-download media preferences. Used with [.loadAssetDownloadInfo].
     */
    class MediaPrefs {
        var preferredVideoBitrate: Long? = null
        var preferredVideoHeight: Long? = null
        var preferredVideoWidth: Long? = null

        var preferredAudioLanguages: List<String>? = null
        var preferredTextLanguages: List<String>? = null

        fun setPreferredVideoBitrate(preferredVideoBitrate: Long?): MediaPrefs {
            this.preferredVideoBitrate = preferredVideoBitrate
            return this
        }

        fun setPreferredVideoHeight(preferredVideoHeight: Long?): MediaPrefs {
            this.preferredVideoHeight = preferredVideoHeight
            return this
        }

        fun setPreferredVideoWidth(preferredVideoWidth: Long?): MediaPrefs {
            this.preferredVideoWidth = preferredVideoWidth
            return this
        }

        fun setPreferredAudioLanguages(preferredAudioLanguages: List<String>): MediaPrefs {
            this.preferredAudioLanguages = preferredAudioLanguages
            return this
        }

        fun setPreferredTextLanguages(preferredTextLanguages: List<String>): MediaPrefs {
            this.preferredTextLanguages = preferredTextLanguages
            return this
        }
    }

    companion object {

        val instance: OfflineManager
            get() = ExoOfflineManager()
    }
}
