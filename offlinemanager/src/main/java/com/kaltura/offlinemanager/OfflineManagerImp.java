package com.kaltura.offlinemanager;

import android.content.Context;

import com.kaltura.dtg.ContentManager;
import com.kaltura.dtg.DownloadState;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.MediaOptions;

import java.util.List;

// PlayKit entryId === DTG itemId

class OfflineManagerImp extends OfflineManager {
    private AssetInfoUpdateListener assetInfoUpdateListener;
    private DownloadProgressListener downloadProgressListener;

    private LocalAssetsManager localAssetsManager;
    private ContentManager contentManager;
    private DrmLicenseUpdateListener drmLicenseUpdateListener;

    @Override
    public void startService(Context appContext, OnServiceStart listener) {
        // TODO
    }

    @Override
    public void stopService() {
        // TODO
    }

    @Override
    public void setAssetInfoUpdateListener(AssetInfoUpdateListener listener) {
        this.assetInfoUpdateListener = listener;
    }

    @Override
    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
    }

    @Override
    public void setDrmLicenseUpdateListener(DrmLicenseUpdateListener listener) {
        drmLicenseUpdateListener = listener;
    }

    @Override
    public void pauseDownloads() {
        // TODO: 29/01/2018 DTG stop
    }

    @Override
    public void resumeDownloads() {
        // TODO: 29/01/2018 DTG resume
    }

    @Override
    public AssetInfo getAssetInfo(String assetId) {
        return null;
    }

    @Override
    public List<AssetInfo> getAssetsByState(DownloadState state) {
        // TODO: 15/02/2018 DTG get downloads
        return null;
    }

    @Override
    public PKMediaEntry getOriginalMediaEntry(String assetId) {
        // TODO: 28/01/2018 DTG lookup, get entry json from app data
        return null;
    }

    @Override
    public PKMediaEntry getLocalPlaybackEntry(String assetId) {
        // TODO: 28/01/2018 DTG local file and LocalAssetManager local source
        return null;
    }

    @Override
    public boolean addAsset(PKMediaEntry mediaEntry) {
        // TODO: 28/01/2018 Add DTG item and save mediaEntry as json
        return false;
    }

    @Override
    public boolean addAsset(int partnerId, String ks, String serverUrl, MediaOptions mediaOptions) {
        // TODO: 28/01/2018 Use media provider, addAsset()
        return false;
    }

    @Override
    public boolean loadAssetDownloadInfo(String assetId, MediaPrefs selection, TrackSelectionListener trackSelectionListener, AssetInfoUpdateListener assetInfoUpdateListener) {
        // TODO: 29/01/2018 Use DTG's loadMetadata. Apply MediaPrefs in onTracksAvailable. Call listener.
        // The default behavior for video selection is to choose the best track for the device:
        // 1. Dimensions: should match screen size
        // 2. DRM requirements
        // 3. MediaPrefs parameters
        return false;
    }

    @Override
    public boolean startAsset(String assetId) {
        // TODO: 29/01/2018 DTG start item. IF ANOTHER ITEM IS IN PROGRESS, DON'T START yet.
        return false;
    }

    @Override
    public boolean pauseAsset(String assetId) {
        // TODO: 29/01/2018 DTG pause item.
        return false;
    }

    @Override
    public boolean removeAsset(String assetId) {
        // TODO: 29/01/2018 DTG remove item. LAM unregister.
        return false;
    }

    @Override
    public AssetDrmInfo getDrmStatus(String assetId) {
        // TODO: 29/01/2018 LAM check status.
        return null;
    }

    @Override
    public boolean renewAssetDrmLicense(String assetId) {
        // TODO: 29/01/2018 LAM renew.
        return false;
    }
}
