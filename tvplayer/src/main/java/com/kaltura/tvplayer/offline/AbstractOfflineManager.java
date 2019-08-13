package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import com.kaltura.playkit.*;
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

    private final Handler eventHandler;

    void postEvent(Runnable event) {
        eventHandler.post(event);
    }

    private static final AssetStateListener noopListener = new AssetStateListener() {
        @Override public void onStateChanged(@NonNull String assetId, @NonNull AssetInfo assetInfo) {}
        @Override public void onAssetRemoved(@NonNull String assetId) {}
        @Override public void onAssetDownloadFailed(@NonNull String assetId, AssetDownloadException error) {}
        @Override public void onAssetDownloadComplete(@NonNull String assetId) {}
        @Override public void onAssetDownloadPending(@NonNull String assetId) {}
        @Override public void onAssetDownloadPaused(@NonNull String assetId) {}
        @Override public void onRegistered(@NonNull String assetId, DrmStatus drmStatus) {}
        @Override public void onRegisterError(@NonNull String assetId, Exception error) {}
    };

    AbstractOfflineManager(Context context) {
        this.appContext = context.getApplicationContext();


        HandlerThread handlerThread = new HandlerThread("OfflineManagerEvents");
        handlerThread.start();
        eventHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public final void prepareAsset(MediaOptions mediaOptions, SelectionPrefs prefs,
                                   PrepareCallback prepareCallback) throws IllegalStateException {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(kalturaServerUrl, kalturaPartnerId, ks, null);

        mediaEntryProvider.load(response -> {
            postEvent(() -> {
                if (response.isSuccess()) {
                    final PKMediaEntry mediaEntry = response.getResponse();
                    prepareCallback.onMediaEntryLoaded(mediaEntry.getId(), mediaEntry);
                    prepareAsset(mediaEntry, prefs, prepareCallback);
                } else {
                    prepareCallback.onMediaEntryLoadError(new IOException(response.getError().getMessage()));
                }
            });
        });
    }

    @Override
    public void renewDrmAsset(String assetId, MediaOptions mediaOptions, MediaEntryCallback mediaEntryCallback) {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(kalturaServerUrl, kalturaPartnerId, ks, null);

        mediaEntryProvider.load(response -> {
            postEvent(() -> {
                if (response.isSuccess()) {
                    final PKMediaEntry mediaEntry = response.getResponse();
                    mediaEntryCallback.onMediaEntryLoaded(mediaEntry.getId(), mediaEntry);

                    renewDrmAsset(assetId, mediaEntry);

                } else {
                    mediaEntryCallback.onMediaEntryLoadError(new IOException(response.getError().getMessage()));
                }
            });
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
