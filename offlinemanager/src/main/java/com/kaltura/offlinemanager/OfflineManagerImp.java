package com.kaltura.offlinemanager;

import com.kaltura.dtg.ContentManager;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.MediaOptions;

// PlayKit entryId === DTG itemId

class OfflineManagerImp extends OfflineManager {
    private DownloadStateListener downloadStateListener;
    private DownloadProgressListener downloadProgressListener;

    private LocalAssetsManager localAssetsManager;
    private ContentManager contentManager;

    @Override
    public void setDownloadStateListener(DownloadStateListener listener) {
        this.downloadStateListener = listener;
    }

    @Override
    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
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
    public EntryMetadata getEntryMetadata(String entryId) {
        // TODO: 28/01/2018 DTG lookup
        return null;
    }

    @Override
    public PKMediaEntry getMediaEntry(String entryId) {
        // TODO: 28/01/2018 DTG lookup, get entry json from app data
        return null;
    }

    @Override
    public PKMediaEntry getLocalEntry(String entryId) {
        // TODO: 28/01/2018 DTG local file and LocalAssetManager local source
        return null;
    }

    @Override
    public EntryMetadata addEntry(PKMediaEntry mediaEntry) {
        // TODO: 28/01/2018 Add DTG item and save mediaEntry as json
        return null;
    }

    @Override
    public EntryMetadata addEntry(int partnerId, String ks, String serverUrl, MediaOptions mediaOptions) {
        // TODO: 28/01/2018 Use media provider, addEntry()
        return null;
    }

    @Override
    public boolean loadMetadata(String entryId, MediaSelection selection, MetadataListener listener) {
        // TODO: 29/01/2018 Use DTG's loadMetadata. Apply MediaSelection in onTracksAvailable. Call listener.
        // The default behavior for video selection is to choose the best track for the device:
        // 1. Dimensions: should match screen size
        // 2. DRM requirements
        // 3. MediaSelection parameters
        return false;
    }

    @Override
    public boolean startDownload(String entryId) {
        // TODO: 29/01/2018 DTG start item. IF ANOTHER ITEM IS IN PROGRESS, DON'T START yet.
        return false;
    }

    @Override
    public boolean pauseDownload(String entryId) {
        // TODO: 29/01/2018 DTG pause item.
        return false;
    }

    @Override
    public boolean removeEntry(String entryId) {
        // TODO: 29/01/2018 DTG remove item. LAM unregister.
        return false;
    }

    @Override
    public boolean checkDrmLicense(String entryId) {
        // TODO: 29/01/2018 LAM check status.
        return false;
    }

    @Override
    public boolean renewDrmLicense(String entryId) {
        // TODO: 29/01/2018 LAM renew.
        return false;
    }
}
