package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class AbstractOfflineManager extends OfflineManager {
    private static final PKLog log = PKLog.get("AbstractOfflineManager");

    protected final Context appContext;
    protected final Map<String, Pair<PKMediaSource, Object>> pendingDrmRegistration = new ConcurrentHashMap<>();
    protected final LocalAssetsManagerExo lam;
    protected PKMediaFormat preferredMediaFormat;
    protected boolean forceWidevineL3Playback;
    protected DownloadProgressListener downloadProgressListener;
    protected OfflineManagerSettings offlineManagerSettings;
    private AssetStateListener assetStateListener;
    private String ks;

    private Handler eventHandler;

    protected void postEvent(Runnable event) {
        if (isEventHandlerAlive()) {
            eventHandler.post(event);
        }
    }

    protected void postEventDelayed(Runnable event, int delayMillis) {
        if (isEventHandlerAlive()) {
            eventHandler.postDelayed(event, delayMillis);
        }
    }
    
    protected void removeEventHandler() {
        if (isEventHandlerAlive()) {
            eventHandler.removeCallbacksAndMessages(null);
            eventHandler = null;
        }
    }

    private static final AssetStateListener noopListener = new AssetStateListener() {
        @Override public void onStateChanged(@NonNull String assetId, @NonNull DownloadType downloadType, @NonNull AssetInfo assetInfo) {}
        @Override public void onAssetRemoved(@NonNull String assetId, @NonNull DownloadType downloadType) {}
        @Override public void onAssetRemoveError(@NonNull String assetId, @NonNull DownloadType downloadType, @NonNull Exception error) {}
        @Override public void onAssetDownloadFailed(@NonNull String assetId, @NonNull DownloadType downloadType, @NonNull Exception error) {}
        @Override public void onAssetDownloadComplete(@NonNull String assetId, @NonNull DownloadType downloadType) {}
        @Override public void onAssetPrefetchComplete(@NonNull String assetId, @NonNull DownloadType downloadType) {}
        @Override public void onAssetDownloadPending(@NonNull String assetId, @NonNull DownloadType downloadType) {}
        @Override public void onAssetDownloadPaused(@NonNull String assetId, @NonNull DownloadType downloadType) {}
        @Override public void onRegistered(@NonNull String assetId, @NonNull DrmStatus drmStatus) {}
        @Override public void onRegisterError(@NonNull String assetId, @NonNull DownloadType downloadType, @NonNull Exception error) {}
        @Override public void onUnRegisterError(@NonNull String assetId, @NonNull OfflineManager.DownloadType downloadType, @NonNull Exception error) { }
    };

    public AbstractOfflineManager(Context context) {
        this.appContext = context.getApplicationContext();
        setupEventHandler();
        lam = new LocalAssetsManagerExo(context);
    }

    @Override
    public final void prepareAsset(@NonNull MediaOptions mediaOptions, @NonNull SelectionPrefs selectionPrefs,
                                   @NonNull PrepareCallback prepareCallback) throws IllegalStateException {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider = mediaOptions.buildMediaProvider(kalturaServerUrl, kalturaPartnerId);

        mediaEntryProvider.load(response -> postEvent(() -> {
            if (response.isSuccess()) {
                final PKMediaEntry mediaEntry = response.getResponse();
                prepareCallback.onMediaEntryLoaded(mediaEntry.getId(), DownloadType.FULL, mediaEntry);
                prepareAsset(mediaEntry, selectionPrefs, prepareCallback);
            } else {
                prepareCallback.onMediaEntryLoadError(DownloadType.FULL, new IOException(response.getError().getMessage()));
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
            DownloadType downloadType = DownloadType.UNKNOWN;
            OfflineManager.AssetInfo assetInfo = getAssetInfo(assetId);
            if (assetInfo != null) {
                downloadType = assetInfo.getDownloadType();
            }

            if (response.isSuccess()) {
                final PKMediaEntry mediaEntry = response.getResponse();
                mediaEntryCallback.onMediaEntryLoaded(mediaEntry.getId(), downloadType, mediaEntry);
                renewDrmAssetLicense(assetId, mediaEntry);
            } else {
                mediaEntryCallback.onMediaEntryLoadError(downloadType, new IOException(response.getError().getMessage()));
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

    protected void setupEventHandler() {
        if (eventHandler == null) {
            HandlerThread handlerThread = new HandlerThread("OfflineManagerEvents");
            handlerThread.start();
            eventHandler = new Handler(handlerThread.getLooper());
        }
    }

    private boolean isEventHandlerAlive() {
        return eventHandler != null;
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
    public void setForceWidevineL3Playback(boolean forceWidevineL3Playback) {
        this.forceWidevineL3Playback = forceWidevineL3Playback;
        if (forceWidevineL3Playback) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(MediaSupport::provisionWidevineL3);
            }
        }
    }

    @Override
    public void setOfflineManagerSettings(@NonNull OfflineManagerSettings offlineManagerSettings) {
        this.offlineManagerSettings = offlineManagerSettings;
        if (lam != null && offlineManagerSettings != null && offlineManagerSettings.getLicenseRequestAdapter() != null) {
            lam.setLicenseRequestAdapter(offlineManagerSettings.getLicenseRequestAdapter());
        }
    }

    @Override
    public void setDownloadProgressListener(DownloadProgressListener listener) {
        this.downloadProgressListener = listener;
    }

    private String sharedPrefsKey(String assetId) {
        return "assetSourceId:" + assetId;
    }

    private String sharedPrefsKeyPkDrmParams(String assetId) {
        return "pkDrmParams:" + assetId;
    }

    private String loadAssetSourceId(String assetId) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        return sharedPrefs.getString(sharedPrefsKey(assetId), null);
    }

    protected PKDrmParams loadAssetPkDrmParams(String assetId) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        String pkDrmParams = sharedPrefs.getString(sharedPrefsKeyPkDrmParams(assetId), null);
        if (pkDrmParams != null) {
            return new Gson().fromJson(pkDrmParams, PKDrmParams.class);
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

    protected void saveAssetPkDrmParams(String assetId, PKDrmParams pkDrmParams) {
        final SharedPreferences sharedPrefs = sharedPrefs();
        String drmParams = new Gson().toJson(pkDrmParams);
        sharedPrefs.edit().putString(sharedPrefsKeyPkDrmParams(assetId), drmParams).apply();
    }

    protected void removeAssetSourceId(String assetId) {
        sharedPrefs().edit().remove(sharedPrefsKey(assetId)).apply();
    }

    protected void removeAssetPkDrmParams(String assetId) {
        sharedPrefs().edit().remove(sharedPrefsKeyPkDrmParams(assetId)).apply();
    }

    protected @NonNull DrmStatus getDrmStatus(@NonNull String assetId, @Nullable byte[] drmInitData) {
        if (drmInitData == null) {
            return DrmStatus.clear;
        }
        final LocalAssetsManager.AssetStatus assetStatus = lam.getDrmStatus(assetId, drmInitData, forceWidevineL3Playback);

        if (assetStatus == null || !assetStatus.registered) {
            return DrmStatus.unknown;
        }

        if (!assetStatus.hasContentProtection) {
            return DrmStatus.clear;
        }

        return DrmStatus.withDrm(assetStatus.licenseDuration, assetStatus.totalDuration);
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
                postEvent(() -> getListener().onRegisterError(assetId, DownloadType.FULL, new LocalAssetsManager.RegisterException("drmInitData = null", null)));
                return;
            }
            lam.registerWidevineDashAsset(assetId, drmParams.getLicenseUri(), drmInitData, forceWidevineL3Playback);
            postEvent(() -> getListener().onRegistered(assetId, getDrmStatus(assetId, drmInitData)));
        } catch (LocalAssetsManager.RegisterException | IOException | InterruptedException e) {
            postEvent(() -> getListener().onRegisterError(assetId, DownloadType.FULL, e));
        }
    }
}
