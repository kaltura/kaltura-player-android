package com.kaltura.tvplayer;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.tvplayer.utils.GsonReader;
import com.kaltura.tvplayer.utils.TokenResolver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.kaltura.tvplayer.PlayerInitOptions.CONFIG;
import static com.kaltura.tvplayer.PlayerInitOptions.PLAYER;
import static com.kaltura.tvplayer.PlayerInitOptions.PLUGINS;

public abstract class KalturaPlayer <MOT extends MediaOptions> {

    private static final PKLog log = PKLog.get("KalturaPlayer");

    public static final String DEFAULT_OVP_SERVER_URL =
            BuildConfig.DEBUG ? "http://cdnapi.kaltura.com/" : "https://cdnapisec.kaltura.com/";


    protected String serverUrl;
    private String ks;
    private int partnerId;
    private final Integer uiConfId;
    protected final String referrer;
    private final Context context;
    protected Player pkPlayer;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private PKMediaFormat preferredMeidaFormat;
    private View view;
    private PKMediaEntry mediaEntry;
    private boolean prepared;
    private Resolver tokenResolver = new Resolver(mediaEntry);
    private PlayerInitOptions initOptions;

    protected KalturaPlayer(Context context, PlayerInitOptions initOptions) {

        this.context = context;

        this.preload = initOptions.preload != null ? initOptions.preload : false;
        this.autoPlay = initOptions.autoplay != null ? initOptions.autoplay : false;
        if (this.autoPlay) {
            this.preload = true; // autoplay implies preload
        }

        this.referrer = buildReferrer(context, initOptions.referrer);
        this.partnerId = initOptions.partnerId;
        this.uiConfId = initOptions.uiConfId;
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

    public void setPreferrdMediaFormat(PKMediaFormat preferedMediaFormat) {
        this.preferredMeidaFormat = preferedMediaFormat;
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
        this.initOptions = initOptions;
        PKPluginConfigs pluginConfigs = initOptions.pluginConfigs;
        PKPluginConfigs combinedPluginConfigs = new PKPluginConfigs();

        GsonReader uiConf = GsonReader.withObject(initOptions.uiConf);

        //JsonObject providerUIConf = (uiConf != null && uiConf.getObject("config") != null && uiConf.getObject("config").getAsJsonObject("player") != null) ? uiConf.getObject("config").getAsJsonObject("player").getAsJsonObject("proivder") : null;

        //JsonObject playbackUIConf = (uiConf != null && uiConf.getObject("config") != null && uiConf.getObject("config").getAsJsonObject("player") != null) ? uiConf.getObject("config").getAsJsonObject("player").getAsJsonObject("playback") : null;

        JsonObject pluginsUIConf = (uiConf != null && uiConf.getObject(CONFIG) != null && uiConf.getObject(CONFIG).getAsJsonObject(PLAYER) != null) ? uiConf.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLUGINS) : new JsonObject();


        // Special case: Kaltura Analytics plugins
        // KAVA
        if (initOptions.uiConfId != null && pluginsUIConf != null) {
            String name = KavaAnalyticsPlugin.factory.getName();
            JsonObject appKavaJsonObject = null;
            if (initOptions.pluginConfigs.hasConfig(name)) {
                appKavaJsonObject = (JsonObject) initOptions.pluginConfigs.getPluginConfig(name);
            } else {
                appKavaJsonObject = kavaDefaults(partnerId, initOptions.uiConfId, referrer);
            }
            JsonObject uic = mergeJsonConfig(appKavaJsonObject, pluginsUIConf.getAsJsonObject(name));
            if (uic != null) {
                pluginsUIConf.add(name, uic);
            }
        }

        if (pluginConfigs != null) {
            Gson gson = new Gson();
            for (Map.Entry<String, Object> entry : pluginConfigs) {
                String pluginName = entry.getKey();
                JsonObject config = (JsonObject) entry.getValue();
                if (pluginsUIConf != null && pluginsUIConf.has(pluginName)) {
                    JsonObject mergedConfig = mergeJsonConfig(pluginsUIConf.getAsJsonObject(pluginName), config);
                    if (mergedConfig != null) {
                        combinedPluginConfigs.setPluginConfig(pluginName, mergedConfig);
                    }
                } else if (config != null){
                    combinedPluginConfigs.setPluginConfig(pluginName, config);
                }
            }
        }



        // Add the plugins that are ONLY mentioned in UIConf and not merged yet
        if (pluginsUIConf != null && pluginsUIConf.keySet() != null) {
            for (String pluginName : pluginsUIConf.keySet()) {
                if (combinedPluginConfigs.hasConfig(pluginName)) {
                    continue;
                }
                JsonObject config = pluginsUIConf.getAsJsonObject(pluginName);
                if (config != null) {
                    combinedPluginConfigs.setPluginConfig(pluginName, config);
                }
            }

            //addKalturaPluginConfigs(combinedPluginConfigs);

            pkPlayer = PlayKitManager.loadPlayer(context, combinedPluginConfigs); //pluginConfigs
            if (initOptions.audioLanguageMode != null && initOptions.audioLanguage != null) {
                pkPlayer.getSettings().setPreferredAudioTrack(new PKTrackConfig().setPreferredMode(initOptions.audioLanguageMode).setTrackLanguage(initOptions.audioLanguage));
            }
            if (initOptions.textLanguageMode != null && initOptions.textLanguage != null) {
                pkPlayer.getSettings().setPreferredTextTrack(new PKTrackConfig().setPreferredMode(initOptions.textLanguageMode).setTrackLanguage(initOptions.textLanguage));
            }

            if (initOptions.allowCrossProtocolEnabled) {
                pkPlayer.getSettings().setAllowCrossProtocolRedirect(initOptions.allowCrossProtocolEnabled);
            }
            PlayManifestRequestAdapter.install(pkPlayer, referrer);
        }
    }

    public JsonObject mergeJsonConfig(JsonObject source, JsonObject target) {
         if (source == null && target != null) {
            return target;
        } else if (source != null && target == null) {
            return source;
        } else if (source == null || target == null) {
            return null;
        }

        for (String key: source.keySet()) {
            Log.d("XXX", "key = " + key);

            Object sourceValue = source.get(key);
            boolean isSourceValueIsNull = (sourceValue == null || (sourceValue != null && source.get(key).isJsonNull()));
            if (!target.has(key)) {
                // new value for "key":
                if (!isSourceValueIsNull) {
                    if (sourceValue instanceof JsonArray) {
                        target.add(key, (JsonArray) sourceValue);
                    }
                    if (sourceValue instanceof JsonPrimitive) {
                        target.add(key, (JsonPrimitive) sourceValue);
                    }
                } else {
                    target.add(key,null);
                }
            } else {
                // existing value for "key" - recursively deep merge:
                if (sourceValue instanceof JsonObject) {
                    JsonObject valueJson = (JsonObject) sourceValue;
                    mergeJsonConfig(valueJson, target.get(key).getAsJsonObject());
                } else {
                    Object targetValue = target.get(key);
                    boolean isTargetValueIsNull = (targetValue == null || (targetValue != null && target.get(key).isJsonNull()));
                    if (sourceValue instanceof JsonArray) {
                        if (!isTargetValueIsNull) {
                            target.add(key, (JsonArray) targetValue);
                        }
                    } else if (sourceValue instanceof JsonPrimitive) {
                        if (target.has(key)) {
                            if (!isTargetValueIsNull) {
                                JsonPrimitive targetPrimitiveVal = (JsonPrimitive) targetValue;
                                if (targetPrimitiveVal.isString()) {
                                    if ("".equals(targetPrimitiveVal.getAsString()) && !isSourceValueIsNull) {
                                        target.add(key, (JsonPrimitive) sourceValue);
                                    } else {
                                        target.add(key, targetPrimitiveVal);
                                    }
                                } else if (!targetPrimitiveVal.isString()) {
                                    target.add(key, targetPrimitiveVal);
                                }
                            } else if (!isSourceValueIsNull) {
                                target.add(key, (JsonPrimitive) sourceValue);
                            }
                        } else {
                            if (!isSourceValueIsNull) {
                                target.add(key, (JsonPrimitive) sourceValue);
                                Log.d("XXX", "no key " + key + " in target value = " + (JsonPrimitive) sourceValue);
                            } else {
                                target.add(key, null);
                                Log.d("XXX", "no key " + key + " in target value = null");
                            }
                        }
                    }
                }
            }
        }
        return target;
    }

    public View getView() {

        if (this.view != null) {
            return view;

        } else {
            FrameLayout view = new FrameLayout(context);
            view.setBackgroundColor(Color.BLACK);
            PlaybackControlsView controlsView = new PlaybackControlsView(context);
            final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.BOTTOM | Gravity.START);
            view.addView(controlsView, layoutParams);
            view.addView(pkPlayer.getView(), FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
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
        setStartPosition(mediaOptions.getStartPosition());
        setPreferrdMediaFormat(mediaOptions.getPreferredMediaFormat());
        setKS(mediaOptions.getKs());
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
                .setStartPosition((long) (startPosition))
                .setPreferredMediaFormat(preferredMeidaFormat);

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
        return uiConfId;
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
