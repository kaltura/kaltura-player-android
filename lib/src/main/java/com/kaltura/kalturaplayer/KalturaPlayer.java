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

public abstract class KalturaPlayer <MediaOptions> {
    final SimpleOvpSessionProvider sessionProvider;
    final String referrer;
    private final Context context;
    Player player;
    PKMediaFormat preferredFormat;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private View view;
    private PKMediaEntry mediaEntry;

    
    public KalturaPlayer(Context context, int partnerId, InitOptions initOptions) {

        this.context = context;

        if (initOptions == null) {
            initOptions = new InitOptions();
        }
        
        if (initOptions.serverUrl == null) {
            initOptions.serverUrl = getDefaultServerUrl();
        }
        
        this.sessionProvider = new SimpleOvpSessionProvider(initOptions.serverUrl, partnerId, initOptions.ks);

        this.preload = initOptions.preload || initOptions.autoPlay; // autoPlay implies preload
        this.autoPlay = initOptions.autoPlay;

        this.preferredFormat = initOptions.preferredFormat;
        this.referrer = buildReferrer(context, initOptions.referrer);

        registerPlugins(context);

        loadPlayer(initOptions.pluginConfigs);
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

    abstract void registerPlugins(Context context);
    
    abstract String getDefaultServerUrl();
    
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

        player = PlayKitManager.loadPlayer(context, combined);

        PlayManifestRequestAdapter.install(player, referrer);
    }

    abstract void addKalturaPluginConfigs(PKPluginConfigs combined);


    public View getView() {

        if (this.view != null) {
            return view;
            
        } else {
            FrameLayout view = new FrameLayout(context);
            view.setBackgroundColor(Color.BLACK);
            view.addView(player.getView(), FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

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

        if (preload) {
            prepare();
        }
    }
    
    public abstract void setMedia(PKMediaEntry mediaEntry, MediaOptions mediaOptions);
    public abstract void loadMedia(MediaOptions mediaOptions, OnEntryLoadListener listener);

    public void setKS(String ks) {
        sessionProvider.setKs(ks);
        updateKS(ks);
    }

    protected abstract void updateKS(String ks);


    public void prepare() {
        final PKMediaConfig config = new PKMediaConfig()
                .setMediaEntry(mediaEntry)
                .setStartPosition((long) (startPosition * 1000));

        player.prepare(config);

        if (autoPlay) {
            player.play();
        }
    }

    // Player controls
    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        player.updatePluginConfig(pluginName, pluginConfig);
    }

    public void onApplicationPaused() {
        player.onApplicationPaused();
    }

    public void onApplicationResumed() {
        player.onApplicationResumed();
    }

    public void destroy() {
        player.destroy();
    }

    public void stop() {
        player.stop();
    }

    public void play() {
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void replay() {
        player.replay();
    }

    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public long getDuration() {
        return player.getDuration();
    }

    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    public void setVolume(float volume) {
        player.setVolume(volume);
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void addEventListener(@NonNull PKEvent.Listener listener, Enum... events) {
        player.addEventListener(listener, events);
    }
    
    public void addStateChangeListener(@NonNull PKEvent.Listener listener) {
        player.addStateChangeListener(listener);
    }

    public void changeTrack(String uniqueId) {
        player.changeTrack(uniqueId);
    }

    public void seekTo(long position) {
        player.seekTo(position);
    }

    public AdController getAdController() {
        return player.getAdController();
    }

    public String getSessionId() {
        return player.getSessionId();
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

    void mediaLoadCompleted(final ResultElement<PKMediaEntry> response, final OnEntryLoadListener onEntryLoadListener) {
        final PKMediaEntry entry = response.getResponse();

        maybeRemoveUnpreferredFormats(entry);

        mediaEntry = entry;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onEntryLoadListener.onMediaEntryLoaded(entry, response.getError());
                setMedia(entry);
            }
        });
    }

    public interface OnEntryLoadListener {
        void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error);
    }

    public interface KSProvider {
        void getKS(KSResult result);
    }
    
    public interface KSResult {
        void complete(String ks, Exception error);
    }
    
    public static class InitOptions {
        public String ks;
        public PKPluginConfigs pluginConfigs;
        public boolean autoPlay;
        public boolean preload;
        public PKMediaFormat preferredFormat;
        public String serverUrl;
        public String referrer;

        public InitOptions(boolean autoPlay, boolean preload) {
            this(autoPlay, preload, null);
        }

        public InitOptions(boolean autoPlay, boolean preload, PKMediaFormat preferredFormat) {
            this(autoPlay, preload, preferredFormat, null);
        }

        public InitOptions(boolean autoPlay, boolean preload, PKMediaFormat preferredFormat,
                           String referrer) {
            this(autoPlay, preload, preferredFormat, referrer, null);
        }

        public InitOptions(boolean autoPlay, boolean preload, PKMediaFormat preferredFormat,
                           String referrer, String serverUrl) {
            
            this.autoPlay = autoPlay;
            this.preload = preload;
            this.preferredFormat = preferredFormat;
            this.serverUrl = serverUrl;
            this.referrer = referrer;
        }
        
        public InitOptions() {}

        public static InitOptions autoPlay() {
            return new InitOptions(true, true, null, null, null);
        }
    }
}
