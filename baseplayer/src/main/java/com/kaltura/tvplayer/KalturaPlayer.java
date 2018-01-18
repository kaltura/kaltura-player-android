package com.kaltura.tvplayer;

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.TokenResolver;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.utils.GsonReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public abstract class KalturaPlayer <MOT extends MediaOptions> {

    private static final PKLog log = PKLog.get("KalturaPlayer");

    public static final String DEFAULT_OVP_SERVER_URL = 
            BuildConfig.DEBUG ? "http://cdnapi.kaltura.com/" : "https://cdnapisec.kaltura.com/";
    
    protected String serverUrl;
    private String ks;
    private int partnerId;
    
    protected final String referrer;
    private final Context context;
    protected Player pkPlayer;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private View view;
    private PKMediaEntry mediaEntry;
    private boolean prepared;
    private Resolver tokenResolver = new Resolver(mediaEntry);

    protected KalturaPlayer(Context context, PlayerInitOptions initOptions) {

        this.context = context;
        
        this.preload = initOptions.preload != null ? initOptions.preload : false;
        this.autoPlay = initOptions.autoplay != null ? initOptions.autoplay : false;
        if (this.autoPlay) {
            this.preload = true; // autoplay implies preload
        }

        this.referrer = buildReferrer(context, initOptions.referrer);
        this.partnerId = initOptions.partnerId;
        this.ks = initOptions.ks;
        
        registerPlugins(context);

        loadPlayer(initOptions);
    }

    protected static String safeServerUrl(String url, String defaultUrl) {
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

    private static class Resolver implements TokenResolver {
        final Map<String, String> map = new HashMap<>();
        String[] sources;
        String[] destinations;
        
        Resolver(PKMediaEntry mediaEntry) {
            refresh(mediaEntry);
        }
        
        void refresh(PKMediaEntry mediaEntry) {
            // TODO: more tokens.
            if (mediaEntry != null) {
                map.put("{{entryId}}", mediaEntry.getId());
            }

            sources = new String[map.size()];
            destinations = new String[map.size()];

            final Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                final Map.Entry<String, String> entry = it.next();
                sources[i] = entry.getKey();
                destinations[i] = entry.getValue();
                i++;
            }
        }

        @Override
        public String resolve(String string) {
            if (string == null || sources.length == 0 || destinations.length == 0) {
                return string;
            }
            return TextUtils.replace(string, sources, destinations).toString();
        }
    }
    
    private JsonObject kavaDefaults(int partnerId, int uiConfId, String referrer) {
        JsonObject object = new JsonObject();
        object.addProperty("partnerId", partnerId);
        object.addProperty("uiConfId", uiConfId);
        object.addProperty("referrer", referrer);
        return object;
    }
    
    private JsonObject prepareKava(JsonObject uiConf, int partnerId, int uiConfId, String referrer) {
        return mergeJsonConfig(uiConf, kavaDefaults(partnerId, uiConfId, referrer));
    }
    
    private void loadPlayer(PlayerInitOptions initOptions) {

        // Assuming that at this point, all plugins are already registered.
        
        PKPluginConfigs combined = new PKPluginConfigs();
        combined.setTokenResolver(tokenResolver);


        PKPluginConfigs pluginConfigs = initOptions.pluginConfigs;
        GsonReader uiConf = GsonReader.withObject(initOptions.uiConf);
        JsonObject pluginsUIConf = uiConf != null ? uiConf.getObject("plugins") : null;
        
        
        // Special case: Kaltura Analytics plugins
        
        // KAVA
        String name = KavaAnalyticsPlugin.factory.getName();
        JsonObject uic = mergeJsonConfig(GsonReader.getObject(pluginsUIConf, name), kavaDefaults(partnerId, initOptions.uiConfId, referrer));
        pluginsUIConf.add(name, uic);
        
        
        

        if (pluginConfigs != null) {
            if (pluginsUIConf != null) {
                for (Map.Entry<String, Object> entry : pluginConfigs) {
                    final String pluginName = entry.getKey();
                    final Object config = entry.getValue();
                    JsonObject uiConfObject = GsonReader.getObject(pluginsUIConf, pluginName);

                    final PKPlugin.Factory factory = PlayKitManager.getPluginFactory(pluginName);

                    Object mergedConfig = factory.mergeConfig(config, uiConfObject);
                    if (mergedConfig == null) {
                        if (config instanceof JsonObject) {
                            mergedConfig = mergeJsonConfig((JsonObject) config, uiConfObject);
                        } else {
                            // no merge support
                            mergedConfig = config;
                        }
                    }

                    combined.setPluginConfig(pluginName, mergedConfig);
                }
            } else {
                for (Map.Entry<String, Object> entry : pluginConfigs) {
                    combined.setPluginConfig(entry.getKey(), entry.getValue());
                }
            }
        }
        
        // Add the plugins that are ONLY mentioned in UIConf
        if (pluginsUIConf != null) {
            for (Map.Entry<String, JsonElement> entry : pluginsUIConf.entrySet()) {
                String pluginName = entry.getKey();
                if (combined.hasConfig(pluginName)) {
                    // Already handled.
                    continue;
                }

                JsonElement entryValue = entry.getValue();
                if (!(entryValue instanceof JsonObject)) {
                    log.w("Ignoring invalid config format for plugin " + pluginName);
                    continue;
                }
                
                JsonObject jsonObject = (JsonObject) entryValue;
                final PKPlugin.Factory factory = PlayKitManager.getPluginFactory(pluginName);
                Object config = factory.mergeConfig(null, jsonObject);
                
                combined.setPluginConfig(pluginName, config);
            }
        }
        


//        addKalturaPluginConfigs(combined);

        pkPlayer = PlayKitManager.loadPlayer(context, combined);

        PlayManifestRequestAdapter.install(pkPlayer, referrer);
    }

    private JsonObject mergeJsonConfig(JsonObject original, JsonObject uiConf) {

        JsonObject merged = original.deepCopy();

        for (Map.Entry<String, JsonElement> entry : uiConf.entrySet()) {
            if (!merged.has(entry.getKey())) {
                merged.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }
        
        return null;
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

    public void setMedia(PKMediaEntry mediaEntry) {
        this.mediaEntry = mediaEntry;
        tokenResolver.refresh(mediaEntry);
        prepared = false;

        if (preload) {
            prepare();
        }
    }

    public void setMedia(PKMediaEntry entry, MediaOptions mediaOptions) {
        setStartPosition(mediaOptions.startPosition);
        setKS(mediaOptions.ks);
        
        setMedia(entry);
    }

    protected void registerCommonPlugins(Context context) {
        KnownPlugin.registerAll(context);
    }
    

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

    public PKMediaEntry getMediaEntry() {
        return mediaEntry;
    }

    public int getUiConfId() {
        return 42; //TODO
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
    protected void mediaLoadCompleted(final ResultElement<PKMediaEntry> response, final OnEntryLoadListener onEntryLoadListener) {
        final PKMediaEntry entry = response.getResponse();

        mediaEntry = entry;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                onEntryLoadListener.onEntryLoadComplete(entry, response.getError());
                if (entry != null) {
                    setMedia(entry);
                }
            }
        });
    }

    public abstract void loadMedia(MOT mediaOptions, OnEntryLoadListener listener);
    protected abstract void registerPlugins(Context context);
    protected abstract void addKalturaPluginConfigs(PKPluginConfigs combined);
    protected abstract void updateKS(String ks);

    public interface OnEntryLoadListener {
        void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error);
    }
}

