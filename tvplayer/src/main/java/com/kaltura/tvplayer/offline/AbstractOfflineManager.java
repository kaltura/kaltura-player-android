package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;

import java.io.IOException;

abstract class AbstractOfflineManager extends OfflineManager {
    final Context appContext;
    private String kalturaServerUrl = KalturaPlayer.DEFAULT_OVP_SERVER_URL;
    private Integer kalturaPartnerId;
    private DownloadProgressListener downloadProgressListener;
    AssetStateListener assetStateListener;
    private String ks;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final AssetStateListener noopListener = new AssetStateListener() {
        @Override public void onStateChanged(String assetId, AssetInfo assetInfo) {}
        @Override public void onAssetRemoved(String assetId) {}
        @Override public void onAssetDownloadFailed(String assetId, AssetDownloadException error) {}
        @Override public void onAssetDownloadComplete(String assetId) {}
        @Override public void onAssetDownloadPending(String assetId) {}
        @Override public void onAssetDownloadPaused(String assetId) {}
        @Override public void onRegistered(String assetId, DrmStatus drmStatus) {}
        @Override public void onRegisterError(String assetId, Exception error) {}
    };

    AbstractOfflineManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public final void prepareAsset(MediaOptions mediaOptions, SelectionPrefs prefs,
                                   PrepareCallback prepareCallback) throws IllegalStateException {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(kalturaServerUrl, kalturaPartnerId, ks, null);

        mediaEntryProvider.load(response -> {
            if (response.isSuccess()) {
                mainHandler.post(() -> {
                    final PKMediaEntry mediaEntry = response.getResponse();
                    prepareAsset(mediaEntry, prefs, prepareCallback);
                });
            } else {
                prepareCallback.onPrepareError(new IOException(response.getError().getMessage()));
            }
        });
    }

    @Override
    public void renewDrmAsset(String assetId, MediaOptions mediaOptions) {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(kalturaServerUrl, kalturaPartnerId, ks, null);

        final AssetStateListener listener = getListener();
        mediaEntryProvider.load(response -> {
            if (response.isSuccess()) {
                final PKMediaEntry mediaEntry = response.getResponse();
                renewDrmAsset(assetId, mediaEntry);
                listener.onRegistered(assetId, null);// TODO: 2019-08-11 status
            } else {
                listener.onRegisterError(assetId, new IOException(response.getError().getMessage()));
            }
        });
    }

    abstract void renewDrmAsset(String assetId, PKMediaEntry mediaEntry);

    @Override
    public final void sendAssetToPlayer(String assetId, KalturaPlayer player) throws IOException {
        final PKMediaEntry entry = getLocalPlaybackEntry(assetId);
        player.setMedia(entry);
    }

    @Override
    public void setKalturaServerUrl(String url) {
        this.kalturaServerUrl = url;
    }

    @Override
    public void setKalturaPartnerId(int partnerId) {
        this.kalturaPartnerId = partnerId;
    }

    @Override
    public void setAssetStateListener(AssetStateListener listener) {
        this.assetStateListener = listener;
    }

    @Override
    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
    }

    @Override
    public void setKs(String ks) {
        this.ks = ks;
    }


    AssetStateListener getListener() {
        return assetStateListener != null ? assetStateListener : noopListener;
    }
}
