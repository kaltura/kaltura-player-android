package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.tvplayer.MediaOptions;
import com.kaltura.tvplayer.OfflineManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AbstractOfflineManager extends OfflineManager {
    private static final PKLog log = PKLog.get("AbstractOfflineManager");

    protected final Context appContext;
    protected final Map<String, Pair<PKMediaSource, PKDrmParams>> pendingDrmRegistration = new HashMap<>();
    protected final LocalAssetsManagerExo lam;
    protected PKMediaFormat preferredMediaFormat;
    protected int estimatedHlsAudioBitrate;
    protected boolean forceWidevineL3Playback;
    protected DownloadProgressListener downloadProgressListener;
    private AssetStateListener assetStateListener;
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
        @Override public void onAssetDownloadFailed(@NonNull String assetId, @NonNull Exception error) {}
        @Override public void onAssetDownloadComplete(@NonNull String assetId) {}
        @Override public void onAssetDownloadPending(@NonNull String assetId) {}
        @Override public void onAssetDownloadPaused(@NonNull String assetId) {}
        @Override public void onRegistered(@NonNull String assetId, @NonNull DrmStatus drmStatus) {}
        @Override public void onRegisterError(@NonNull String assetId, @NonNull Exception error) {}
    };

    public AbstractOfflineManager(Context context) {
        this.appContext = context.getApplicationContext();
        HandlerThread handlerThread = new HandlerThread("OfflineManagerEvents");
        handlerThread.start();
        eventHandler = new Handler(handlerThread.getLooper());
        lam = new LocalAssetsManagerExo(context);
    }

    @Override
    public final void prepareAsset(@NonNull MediaOptions mediaOptions, @NonNull SelectionPrefs prefs,
                                   @NonNull PrepareCallback prepareCallback) throws IllegalStateException {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(kalturaServerUrl, kalturaPartnerId);

        mediaEntryProvider.load(response -> postEvent(() -> {
            if (response.isSuccess()) {
                final PKMediaEntry mediaEntry = response.getResponse();
                prepareCallback.onMediaEntryLoaded(mediaEntry.getId(), mediaEntry);
                prepareAsset(mediaEntry, prefs, prepareCallback);
            } else {
                prepareCallback.onMediaEntryLoadError(new IOException(response.getError().getMessage()));
            }
        }));
    }

    @Override
    public void renewDrmAssetLicense(@NonNull String assetId, @NonNull MediaOptions mediaOptions, @NonNull MediaEntryCallback mediaEntryCallback) {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(kalturaServerUrl, kalturaPartnerId);

        mediaEntryProvider.load(response -> postEvent(() -> {
            if (response.isSuccess()) {
                final PKMediaEntry mediaEntry = response.getResponse();
                mediaEntryCallback.onMediaEntryLoaded(mediaEntry.getId(), mediaEntry);

                renewDrmAssetLicense(assetId, mediaEntry);

            } else {
                mediaEntryCallback.onMediaEntryLoadError(new IOException(response.getError().getMessage()));
            }
        }));
    }

    private void renewDrmAssetLicense(String assetId, PKMediaEntry mediaEntry) {
        PKDrmParams drmParams = findDrmParams(assetId, mediaEntry);
        if (drmParams != null) {
            renewDrmAssetLicense(assetId, drmParams);
        }
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

    private PKDrmParams findDrmParams(String assetId, PKMediaEntry mediaEntry) {

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
    public void setForceWidevineL3Playback(boolean forceWidevineL3Playback) {
        this.forceWidevineL3Playback = forceWidevineL3Playback;
        if (forceWidevineL3Playback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> MediaSupport.provisionWidevineL3());
            }
        }
    }

    @Override
    public void setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        if (lam != null) {
            lam.setLicenseRequestAdapter(licenseRequestAdapter);
        }
    }

    @Override
    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
    }

    private String sharedPrefsKey(String assetId) {
        return "assetSourceId:" + assetId;
    }

    private String sharedPrefsKeyWidevineL3(String assetId) {
        return "forceWidevineL3:" + assetId;
    }

    private String sharedPrefsKeyPkDrmParams(String assetId) {
        return "pkDrmParams:" + assetId;
    }

    private String loadAssetSourceId(String assetId) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        return sharedPrefs.getString(sharedPrefsKey(assetId), null);
    }

    private boolean loadAssetForceWidevineL3Status(String assetId) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        return sharedPrefs.getBoolean(sharedPrefsKeyWidevineL3(assetId), false);
    }

    protected PKDrmParams loadAssetPkDrmParams(String assetId) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        String pkDrmParams = sharedPrefs.getString(sharedPrefsKeyPkDrmParams(assetId), null);
        if (pkDrmParams != null) {
            Gson pkDrmParamsGson = new Gson();
            return pkDrmParamsGson.fromJson(pkDrmParams , PKDrmParams.class);
        }
        return null;
    }

    private SharedPreferences sharedPrefs() {
        return appContext.getSharedPreferences("KalturaOfflineManager", Context.MODE_PRIVATE);
    }

    protected void saveAssetSourceId(String assetId, String sourceId) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        sharedPrefs.edit().putString(sharedPrefsKey(assetId), sourceId).apply();
    }

    protected void saveAssetForceWidevineL3Status(String assetId, boolean forceWidevineL3Playback) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        sharedPrefs.edit().putBoolean(sharedPrefsKeyWidevineL3(assetId), forceWidevineL3Playback).apply();
    }

    protected void saveAssetPkDrmParams(String assetId, PKDrmParams pkDrmParams) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        Gson pkDrmParamsGson = new Gson();
        String drmParams = pkDrmParamsGson.toJson(pkDrmParams);
        sharedPrefs.edit().putString(sharedPrefsKeyPkDrmParams(assetId), drmParams).apply();
    }

    protected void removeAssetSourceId(String assetId) {
        sharedPrefs().edit().remove(sharedPrefsKey(assetId)).apply();
    }

    protected void removeAssetForceWidevineL3Status(String assetId) {
        sharedPrefs().edit().remove(sharedPrefsKeyWidevineL3(assetId)).apply();
    }

    protected void removeAssetPkDrmParams(String assetId) {
        sharedPrefs().edit().remove(sharedPrefsKeyPkDrmParams(assetId)).apply();
    }

    protected @NonNull DrmStatus getDrmStatus(@NonNull String assetId, @Nullable byte[] drmInitData) {
        if (drmInitData == null) {
            return DrmStatus.clear;
        }
        final LocalAssetsManager.AssetStatus assetStatus = lam.getDrmStatus(assetId, drmInitData, isForceWidevineL3Playback(assetId));

        if (assetStatus == null || !assetStatus.registered) {
            return DrmStatus.unknown;
        }

        if (!assetStatus.hasContentProtection) {
            return DrmStatus.clear;
        }

        return DrmStatus.withDrm(assetStatus.licenseDuration, assetStatus.totalDuration);
    }

    protected boolean isForceWidevineL3Playback(String assetId) {
        boolean p =   assetId != null && loadAssetForceWidevineL3Status(assetId);
        return p;
    }

    @NonNull
    @Override
    public DrmStatus getDrmStatus(@NonNull String assetId) {
        try {
            final byte[] drmInitData = getDrmInitData(assetId);
            if (drmInitData == null) {
                log.e("getDrmStatus failed drmInitData = null");
                return DrmStatus.unknown;
            }
            return getDrmStatus(assetId, drmInitData);

        } catch (IOException | InterruptedException e) {
            log.e("getDrmStatus failed ", e);
            return DrmStatus.unknown;
        }
    }

    protected abstract byte[] getDrmInitData(String assetId) throws IOException, InterruptedException;

    @Override
    public void renewDrmAssetLicense(@NonNull String assetId, @NonNull PKDrmParams drmParams) {
        try {
            final byte[] drmInitData = getDrmInitData(assetId);
            if (drmInitData == null) {
                postEvent(() -> getListener().onRegisterError(assetId, new LocalAssetsManager.RegisterException("drmInitData = null", null)));
                return;
            }
            lam.registerWidevineDashAsset(assetId, drmParams.getLicenseUri(), drmInitData, isForceWidevineL3Playback(assetId));
            postEvent(() -> getListener().onRegistered(assetId, getDrmStatus(assetId, drmInitData)));
        } catch (LocalAssetsManager.RegisterException | IOException | InterruptedException e) {
            postEvent(() -> getListener().onRegisterError(assetId, e));
        }
    }
}
