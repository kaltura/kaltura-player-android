package com.kaltura.kalturaplayer;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.api.ovp.SimpleOvpSessionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class KalturaPlayer <MOT extends MediaOptions> {

    public static final String DEFAULT_OVP_SERVER_URL = "https://cdnapisec.kaltura.com/";
    private int uiConfId;
    
    private static JsonObject cachedUIConf;
    private static int cachedUIConfId;

    private JsonObject uiConf;
    
    String serverUrl;
    private String ks;
    private int partnerId;
    
    final String referrer;
    private final Context context;
    Player pkPlayer;
    PKMediaFormat preferredFormat;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private View view;
    private PKMediaEntry mediaEntry;
    private boolean prepared;

    protected KalturaPlayer(Context context, PlayerInitOptions initOptions) {

        this.context = context;
        
        // TODO: stuff from UIConf
//        initOptions.uiConf
//        initOptions.uiConfId

        this.preload = initOptions.preload || initOptions.autoPlay; // autoPlay implies preload
        this.autoPlay = initOptions.autoPlay;

        this.preferredFormat = initOptions.preferredFormat;
        this.referrer = buildReferrer(context, initOptions.referrer);
        this.partnerId = initOptions.partnerId;
        this.ks = initOptions.ks;
        
        registerPlugins(context);

        loadPlayer(initOptions.pluginConfigs);
    }

    static String safeServerUrl(String url, String defaultUrl) {
        return url == null ? defaultUrl :
                url.endsWith("/") ? url : url + "/";
    }
    
    private static String buildReferrer(Context context, String referrer) {
        if (referrer != null) {
            // If a referrer is given, it must be a valid URL.
            // Parse and check that scheme and authority are not empty.
            final Uri uri = Uri.parse(referrer);
            if (!TextUtils.isEmpty(uri.getScheme()) && !TextUtils.isEmpty(uri.getAuthority())) {
                return referrer;
            }
            // If referrer is not a valid URL, fall back to the generated default.
        }
        
        return new Uri.Builder().scheme("app").authority(context.getPackageName()).toString();
    }

    private void loadPlayer(PKPluginConfigs pluginConfigs) {
        // Load a player preconfigured to use stats plugins and the playManifest adapter.

        PKPluginConfigs combined = new PKPluginConfigs();

        addKalturaPluginConfigs(combined);

        // Copy application-provided configs.
        if (pluginConfigs != null) {
            for (Map.Entry<String, Object> entry : pluginConfigs) {
                combined.setPluginConfig(entry.getKey(), entry.getValue());
            }
        }

        pkPlayer = PlayKitManager.loadPlayer(context, combined);

        PlayManifestRequestAdapter.install(pkPlayer, referrer);
    }



    public View getView() {

        if (this.view != null) {
            return view;
            
        } else {
            FrameLayout view = new FrameLayout(context);
            view.setBackgroundColor(Color.BLACK);
            view.addView(pkPlayer.getView(), FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

            PlaybackControlsView controlsView = new PlaybackControlsView(context);

            final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.BOTTOM | Gravity.START);
            view.addView(controlsView, layoutParams);

            controlsView.setPlayer(this);

            this.view = view;
        }

        return view;
    }

    private void maybeRemoveUnpreferredFormats(PKMediaEntry entry) {
        if (preferredFormat == null) {
            return;
        }
        
        List<PKMediaSource> preferredSources = new ArrayList<>(1);
        for (PKMediaSource source : entry.getSources()) {
            if (source.getMediaFormat() == preferredFormat) {
                preferredSources.add(source);
            }
        }
        
        if (!preferredSources.isEmpty()) {
            entry.setSources(preferredSources);
        }
        
        // otherwise, leave the original source list.
    }

    public void setMedia(PKMediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        prepared = false;

        if (preload) {
            prepare();
        }
    }

    @NonNull
    protected SimpleOvpSessionProvider newSimpleSessionProvider() {
        return new SimpleOvpSessionProvider(getServerUrl(), getPartnerId(), getKS());
    }

    public void setMedia(PKMediaEntry entry, MediaOptions mediaOptions) {
        setStartPosition(mediaOptions.startPosition);
        setKS(mediaOptions.ks);
        
        setMedia(entry);
    }
    
    public abstract void loadMedia(MOT mediaOptions, OnEntryLoadListener listener);
    
    protected abstract void registerPlugins(Context context);
    protected abstract void addKalturaPluginConfigs(PKPluginConfigs combined);
    protected abstract void updateKS(String ks);


    public void setKS(String ks) {
        this.ks = ks;
        updateKS(ks);
    }

    public void prepare() {
        
        if (prepared) {
            return;
        }
        
        final PKMediaConfig config = new PKMediaConfig()
                .setMediaEntry(mediaEntry)
                .setStartPosition((long) (startPosition * 1000));

        pkPlayer.prepare(config);
        prepared = true;

        if (autoPlay) {
            pkPlayer.play();
        }
    }

    // Player controls
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        pkPlayer.updatePluginConfig(pluginName, pluginConfig);
    }

    public void onApplicationPaused() {
        pkPlayer.onApplicationPaused();
    }

    public void onApplicationResumed() {
        pkPlayer.onApplicationResumed();
    }

    public void destroy() {
        pkPlayer.destroy();
    }

    public void stop() {
        pkPlayer.stop();
    }

    public void play() {
        if (!prepared) {
            prepare();
        }
        
        pkPlayer.play();
    }

    public void pause() {
        pkPlayer.pause();
    }

    public void replay() {
        pkPlayer.replay();
    }

    public long getCurrentPosition() {
        return pkPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return pkPlayer.getDuration();
    }

    public long getBufferedPosition() {
        return pkPlayer.getBufferedPosition();
    }

    public void setVolume(float volume) {
        pkPlayer.setVolume(volume);
    }

    public boolean isPlaying() {
        return pkPlayer.isPlaying();
    }

    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        pkPlayer.addEventListener(listener, events);
    }
    
    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        pkPlayer.addStateChangeListener(listener);
    }

    public void changeTrack(String uniqueId) {
        pkPlayer.changeTrack(uniqueId);
    }

    public void seekTo(long position) {
        pkPlayer.seekTo(position);
    }

    public AdController getAdController() {
        return pkPlayer.getAdController();
    }

    public String getSessionId() {
        return pkPlayer.getSessionId();
    }

    public double getStartPosition() {
        return startPosition;
    }

    public KalturaPlayer setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public boolean isPreload() {
        return preload;
    }

    public KalturaPlayer setPreload(boolean preload) {
        this.preload = preload;
        return this;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public KalturaPlayer setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    public PKMediaFormat getPreferredFormat() {
        return preferredFormat;
    }

    public KalturaPlayer setPreferredFormat(PKMediaFormat preferredFormat) {
        this.preferredFormat = preferredFormat;
        return this;
    }
    
    public int getPartnerId() {
        return partnerId;
    }
    
    public String getKS() {
        return ks;
    }
    
    public String getServerUrl() {
        return serverUrl;
    }

    // Called by implementation of loadMedia().
    void mediaLoadCompleted(final ResultElement<PKMediaEntry> response, final OnEntryLoadListener onEntryLoadListener) {
        final PKMediaEntry entry = response.getResponse();

        maybeRemoveUnpreferredFormats(entry);

        mediaEntry = entry;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onEntryLoadListener.onMediaEntryLoaded(entry, response.getError());
                if (entry != null) {
                    setMedia(entry);
                }
            }
        });
    }

    public interface OnEntryLoadListener {
        void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error);
    }
}
