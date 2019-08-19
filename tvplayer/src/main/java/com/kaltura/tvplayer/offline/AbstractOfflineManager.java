package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;

import java.io.IOException;

public abstract class AbstractOfflineManager extends OfflineManager {
    protected final Context appContext;
    protected PKMediaFormat preferredMediaFormat;
    protected int estimatedHlsAudioBitrate;
    protected DownloadProgressListener downloadProgressListener;
    private String kalturaServerUrl = KalturaPlayer.DEFAULT_OVP_SERVER_URL;
    private Integer kalturaPartnerId;
    AssetStateListener assetStateListener;
    private String ks;

    private final Handler eventHandler;

    protected void postEvent(Runnable event) {
        eventHandler.post(event);
    }

    protected void postEventDelayed(Runnable event, int delayMillis) {
        eventHandler.postDelayed(event, delayMillis);
    }

    private static final AssetStateListener noopListener = new AssetStateListener() {
        @Override public void onStateChanged(@NonNull String assetId, @NonNull AssetInfo assetInfo) {}
        @Override public void onAssetRemoved(@NonNull String assetId) {}
        @Override public void onAssetDownloadFailed(@NonNull String assetId, Exception error) {}
        @Override public void onAssetDownloadComplete(@NonNull String assetId) {}
        @Override public void onAssetDownloadPending(@NonNull String assetId) {}
        @Override public void onAssetDownloadPaused(@NonNull String assetId) {}
        @Override public void onRegistered(@NonNull String assetId, DrmStatus drmStatus) {}
        @Override public void onRegisterError(@NonNull String assetId, Exception error) {}
    };

    public AbstractOfflineManager(Context context) {
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

    protected String sharedPrefsKey(String assetId) {
        return "assetSourceId:" + assetId;
    }

    private String loadAssetSourceId(String assetId) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        return sharedPrefs.getString(sharedPrefsKey(assetId), null);
    }

    protected SharedPreferences sharedPrefs() {
        return appContext.getSharedPreferences("KalturaOfflineManager", Context.MODE_PRIVATE);
    }

    void renewDrmAsset(String assetId, PKMediaEntry mediaEntry) {
        PKDrmParams drmParams = findDrmParams(assetId, mediaEntry);
        renewDrmAsset(assetId, drmParams);
    }

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
    public void setKs(String ks) {
        this.ks = ks;
    }

    protected AssetStateListener getListener() {
        return assetStateListener != null ? assetStateListener : noopListener;
    }

    protected PKDrmParams findDrmParams(String assetId, PKMediaEntry mediaEntry) {

        final String sourceId = loadAssetSourceId(assetId);

        final SourceSelector selector = new SourceSelector(mediaEntry, PKMediaFormat.dash);
        selector.setPreferredSourceId(sourceId);

        PKMediaSource selectedSource = selector.getSelectedSource();
        PKDrmParams selectedDrmParams = selector.getSelectedDrmParams();

        if (selectedSource == null || selectedSource.getMediaFormat() != PKMediaFormat.dash) {
            return null;
        }

        return selectedDrmParams;
    }

    @Override
    public void setPreferredMediaFormat(PKMediaFormat preferredMediaFormat) {
        this.preferredMediaFormat = preferredMediaFormat;
    }

    @Override
    public void setEstimatedHlsAudioBitrate(int bitrate) {
        estimatedHlsAudioBitrate = bitrate;
    }

    @Override
    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
    }
}
