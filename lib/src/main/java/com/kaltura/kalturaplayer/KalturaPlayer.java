package com.kaltura.kalturaplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.api.ovp.SimpleOvpSessionProvider;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KalturaPlayer {
    private static final String DEFAULT_SERVER_URL = "https://cdnapisec.kaltura.com/";
    private static final PKLog log = PKLog.get("KalturaPlayer");

    private final Context context;
    private final int partnerId;
    private String ks;

    private SimpleOvpSessionProvider sessionProvider;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private Player player;
    
    // Options
    private boolean autoPlay;
    private boolean autoPrepare;
    private double startPosition;
    
    // Init-only options
    private final boolean useStaticMediaProvider;
    private final PKMediaFormat preferredFormat;
    private final String serverUrl;
    private final String referrer;
    
    private static boolean pluginsRegistered;
    private View view;
    private PKMediaEntry mediaEntry;

    public KalturaPlayer(Context context, int partnerId, String ks, PKPluginConfigs pluginConfigs, Options options) {

        this.context = context;
        this.partnerId = partnerId;
        this.ks = ks;

        if (options == null) {
            options = new Options();
        }

        String serverUrl = options.serverUrl;
        if (serverUrl != null) {
            this.serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        } else {
            this.serverUrl = DEFAULT_SERVER_URL;
        }
        

        this.autoPrepare = options.autoPrepare;
        this.autoPlay = options.autoPlay;


        this.useStaticMediaProvider = options.useStaticMediaProvider;
        this.preferredFormat = options.preferredFormat;
        this.referrer = buildReferrer(context, options.referrer);
        
        if (!useStaticMediaProvider) {
            sessionProvider = new SimpleOvpSessionProvider(this.serverUrl, partnerId, ks);
        }

        // Plugin registration is static and only done once, but requires a Context.
        if (!pluginsRegistered) {
            PlayKitManager.registerPlugins(context, KavaAnalyticsPlugin.factory);
            pluginsRegistered = true;
        }

        loadPlayer(pluginConfigs);
    }
    
    public KalturaPlayer(Context context, int partnerId, String ks, PKPluginConfigs pluginConfigs) {
        this(context, partnerId, ks, pluginConfigs, null);
    }

    public KalturaPlayer(Context context, int partnerId, String ks) {
        this(context, partnerId, ks, null, null);
    }
    
    
    
    private void loadPlayer(PKPluginConfigs pluginConfigs) {
        // Load a player preconfigured to use stats plugins and the playManifest adapter.

        PKPluginConfigs combined = new PKPluginConfigs();

        KavaAnalyticsConfig kavaConfig = getKavaAnalyticsConfig();

        combined.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), kavaConfig);

        // Copy application-provided configs.
        if (pluginConfigs != null) {
            for (Map.Entry<String, Object> entry : pluginConfigs) {
                combined.setPluginConfig(entry.getKey(), entry.getValue());
            }
        }

        player = PlayKitManager.loadPlayer(context, combined);

        PlayManifestRequestAdapter.install(player, referrer);
    }
    
    public void setParentView(ViewGroup viewGroup) {

        if (this.view != null) {
            // Remove from current parent
            final ViewParent parent = view.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(this.view);
            }
            
        } else {
            FrameLayout view = new FrameLayout(context);
            view.addView(player.getView(), FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

            PlaybackControlsView controlsView = new PlaybackControlsView(context);

            final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.BOTTOM | Gravity.START);
            view.addView(controlsView, layoutParams);

            controlsView.setPlayer(this);

            this.view = view;
        }

        viewGroup.addView(view);
    }
    
    /**
     * Load entry using the media provider and call the listener.
     * If {@link #autoPrepare} is true, send the loaded media to the player.
     */
     
    public void loadMedia(@NonNull String entryId, @NonNull final OnEntryLoadListener onEntryLoadListener) {

        MediaEntryProvider provider;
        if (useStaticMediaProvider) {
            provider = StaticMediaEntryBuilder.provider(partnerId, ks, serverUrl, entryId, preferredFormat);
        } else {
            provider = new KalturaOvpMediaProvider()
                    .setSessionProvider(sessionProvider).setEntryId(entryId);
        }

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(final ResultElement<PKMediaEntry> response) {
                final PKMediaEntry entry = response.getResponse();
                
                maybeRemoveUnpreferredFormats(entry);

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onEntryLoadListener.onMediaEntryLoaded(entry, response.getError());
                        if (autoPrepare && response.isSuccess()) {
                            prepare(entry);
                        }
                    }
                });
            }
        });
    }

    private void maybeRemoveUnpreferredFormats(PKMediaEntry entry) {
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
        
        if (autoPrepare) {
            prepare();
        }
    }

    public void prepare() {
        prepare(mediaEntry);
    }

    public void prepare(PKMediaEntry mediaEntry) {
        
        final PKMediaConfig config = new PKMediaConfig()
                .setMediaEntry(mediaEntry)
                .setStartPosition((long) (startPosition * 1000));
        
        player.prepare(config);

        if (autoPlay) {
            player.play();
        }
    }

    public String getKs() {
        return ks;
    }

    public void setKs(String ks) {
        this.ks = ks;
        if (sessionProvider != null) {
            sessionProvider.setKs(ks);
        }
        
        // Update Kava
        player.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig() {
        return new KavaAnalyticsConfig()
                .setKs(ks).setPartnerId(partnerId).setReferrer(referrer);
    }

    private static String buildReferrer(Context context, String referrer) {
        if (referrer == null) {
            referrer = context.getPackageName();
        }

        // If referrer does not have a scheme, add 'app' as scheme.
        Uri uri = Uri.parse(referrer);
        if (TextUtils.isEmpty(uri.getScheme())) {
            return uri.buildUpon().scheme("app").build().toString();
        } else {
            return referrer;
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

    public boolean isLiveStream() {
        return player.isLiveStream();
    }

    public double getStartPosition() {
        return startPosition;
    }

    public KalturaPlayer setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public boolean isAutoPrepare() {
        return autoPrepare;
    }

    public KalturaPlayer setAutoPrepare(boolean autoPrepare) {
        this.autoPrepare = autoPrepare;
        return this;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public KalturaPlayer setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    public static class Options {
        public boolean autoPlay;
        public boolean autoPrepare;
        public boolean useStaticMediaProvider;
        public PKMediaFormat preferredFormat;
        public String serverUrl;
        public String referrer;
        
        public Options(boolean autoPlay, boolean autoPrepare, boolean useStaticMediaProvider, PKMediaFormat preferredFormat,
                       String serverUrl, String referrer) {
            
            this.autoPlay = autoPlay;
            this.autoPrepare = autoPrepare;
            this.useStaticMediaProvider = useStaticMediaProvider;
            this.preferredFormat = preferredFormat;
            this.serverUrl = serverUrl;
            this.referrer = referrer;
        }

        public Options() {}
    }

    public interface OnEntryLoadListener {
        void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error);
    }
}
