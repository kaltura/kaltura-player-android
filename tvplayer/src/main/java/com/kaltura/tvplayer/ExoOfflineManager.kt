package com.kaltura.tvplayer

import android.content.Context

import com.kaltura.playkit.LocalAssetsManager
import com.kaltura.playkit.PKMediaEntry
import com.kaltura.tvplayer.MediaOptions
import com.kaltura.tvplayer.OfflineManager

// PlayKit entryId === DTG itemId

internal class ExoOfflineManager : OfflineManager() {
    private var assetInfoUpdateListener: OfflineManager.AssetInfoUpdateListener? = null
    private var downloadProgressListener: OfflineManager.DownloadProgressListener? = null

    private val localAssetsManager: LocalAssetsManager? = null
    private var drmLicenseUpdateListener: OfflineManager.DrmLicenseUpdateListener? = null

    override fun startService(appContext: Context, listener: OfflineManager.OnServiceStart) {
        // TODO
    }

    override fun stopService() {
        // TODO
    }

    override fun setAssetInfoUpdateListener(listener: OfflineManager.AssetInfoUpdateListener) {
        this.assetInfoUpdateListener = listener
    }

    override fun setDownloadProgressListener(listener: OfflineManager.DownloadProgressListener) {
        this.downloadProgressListener = listener
    }

    override fun setDrmLicenseUpdateListener(listener: OfflineManager.DrmLicenseUpdateListener) {
        drmLicenseUpdateListener = listener
    }

    override fun pauseDownloads() {
        // TODO: 29/01/2018 DTG stop
    }

    override fun resumeDownloads() {
        // TODO: 29/01/2018 DTG resume
    }

    override fun getAssetInfo(assetId: String): AssetInfo? {
        return null
    }

    override fun getAssetsByState(state: AssetDownloadState): List<AssetInfo>? {
        // TODO: 15/02/2018 DTG get downloads
        return null
    }

    override fun getOriginalMediaEntry(assetId: String): PKMediaEntry? {
        // TODO: 28/01/2018 DTG lookup, get entry json from app data
        return null
    }

    override fun getLocalPlaybackEntry(assetId: String): PKMediaEntry? {
        // TODO: 28/01/2018 DTG local file and LocalAssetManager local source
        return null
    }

    override fun addAsset(mediaEntry: PKMediaEntry): Boolean {
        // TODO: 28/01/2018 Add DTG item and save mediaEntry as json
        return false
    }

    override fun addAsset(partnerId: Int, ks: String, serverUrl: String, mediaOptions: MediaOptions): Boolean {
        // TODO: 28/01/2018 Use media provider, addAsset()
        return false
    }

    override fun loadAssetDownloadInfo(
        assetId: String,
        selection: OfflineManager.MediaPrefs,
        trackSelectionListener: OfflineManager.TrackSelectionListener,
        assetInfoUpdateListener: OfflineManager.AssetInfoUpdateListener
    ): Boolean {
        // TODO: 29/01/2018 Use DTG's loadMetadata. Apply MediaPrefs in onTracksAvailable. Call listener.
        // The default behavior for video selection is to choose the best track for the device:
        // 1. Dimensions: should match screen size
        // 2. DRM requirements
        // 3. MediaPrefs parameters
        return false
    }

    override fun startAsset(assetId: String): Boolean {
        // TODO: 29/01/2018 DTG start item. IF ANOTHER ITEM IS IN PROGRESS, DON'T START yet.
        return false
    }

    override fun pauseAsset(assetId: String): Boolean {
        // TODO: 29/01/2018 DTG pause item.
        return false
    }

    override fun removeAsset(assetId: String): Boolean {
        // TODO: 29/01/2018 DTG remove item. LAM unregister.
        return false
    }

    override fun getDrmStatus(assetId: String): AssetDrmInfo? {
        // TODO: 29/01/2018 LAM check status.
        return null
    }

    override fun renewAssetDrmLicense(assetId: String): Boolean {
        // TODO: 29/01/2018 LAM renew.
        return false
    }
}
