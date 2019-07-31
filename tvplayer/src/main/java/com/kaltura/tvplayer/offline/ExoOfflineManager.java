package com.kaltura.tvplayer.offline;

import android.content.Context;
import com.kaltura.playkit.LocalAssetsManager;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;

import java.util.List;

public class ExoOfflineManager extends AbstractOfflineManager {

    private static ExoOfflineManager instance;
    private final Context appContext;
    private final LocalAssetsManager localAssetsManager;
    private DownloadProgressListener downloadProgressListener;

    private ExoOfflineManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.localAssetsManager = new LocalAssetsManager(appContext);
    }

    public static ExoOfflineManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ExoOfflineManager.class) {
                if (instance == null) {
                    instance = new ExoOfflineManager(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void setAssetStateListener(AssetStateListener listener) {

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
    public AssetInfo getAssetInfo(String assetId) {
        return null;
    }

    @Override
    public List<AssetInfo> getAssetsInState(AssetDownloadState state) {
        // TODO: 15/02/2018 DTG get downloads
        return null;
    }

    @Override
    public PKMediaEntry getLocalPlaybackEntry(String assetId) {
        // TODO: 28/01/2018 DTG local file and LocalAssetManager local source
        return null;
    }

    @Override
    public void prepareAsset(PKMediaEntry mediaEntry, SelectionPrefs prefs, PrepareListener prepareListener) {

    }

    @Override
    public boolean addAsset(AssetInfo assetInfo) {
        return false;
    }

    @Override
    public boolean startAssetDownload(String assetId) {
        // TODO: 29/01/2018 DTG start item. IF ANOTHER ITEM IS IN PROGRESS, DON'T START yet.
        return false;
    }

    @Override
    public boolean pauseAssetDownload(String assetId) {
        // TODO: 29/01/2018 DTG pause item.
        return false;
    }

    @Override
    public boolean removeAsset(String assetId) {
        // TODO: 29/01/2018 DTG remove item. LAM unregister.
        return false;
    }

    @Override
    public DrmInfo getDrmStatus(String assetId) {
        // TODO: 29/01/2018 LAM check status.
        return null;
    }

    @Override
    public boolean registerDrmAsset(String assetId, DrmRegisterListener listener) {
        return false;
    }

    @Override
    public boolean registerDrmAsset(String assetId, PKDrmParams drmParams, DrmRegisterListener listener) {
        return false;
    }
}
