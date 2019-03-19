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
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

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
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaStatsPlugin;
import com.kaltura.tvplayer.utils.GsonReader;
import com.kaltura.tvplayer.utils.TokenResolver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.kaltura.tvplayer.PlayerInitOptions.CONFIG;
import static com.kaltura.tvplayer.PlayerInitOptions.OPTIONS;
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
    private Player pkPlayer;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private PKMediaFormat preferredMeidaFormat;
    private boolean allowCrossProtocolRedirect;
    private View view;
    private PKMediaEntry mediaEntry;
    private boolean prepared;
    private Resolver tokenResolver = new Resolver();
    private PlayerInitOptions initOptions;

    protected KalturaPlayer(Context context, PlayerInitOptions initOptions) {

        this.context = context;
        this.initOptions = initOptions;
        this.preload = initOptions.preload != null ? initOptions.preload : false;
        this.autoPlay = initOptions.autoplay != null ? initOptions.autoplay : false;
        if (this.autoPlay) {
            this.preload = true; // autoplay implies preload
        }
        this.allowCrossProtocolRedirect = initOptions.allowCrossProtocolEnabled;
        this.referrer  = buildReferrer(context, initOptions.referrer);
        this.partnerId = initOptions.partnerId;
        this.uiConfId  = initOptions.uiConfId;
        this.serverUrl = initOptions.serverUrl;
        this.ks = initOptions.ks;

        registerPlugins(context);
        loadPlayer();
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
        if (preferedMediaFormat != null) {
            this.preferredMeidaFormat = preferedMediaFormat;
        }
    }

    public void setAllowCrossProtocolRedirect(boolean allowCrossProtocolRedirect) {
        this.allowCrossProtocolRedirect = allowCrossProtocolRedirect;
    }

    private static class Resolver implements TokenResolver {
        final Map<String, String> map = new HashMap<>();
        String[] sources;
        String[] destinations;

        Resolver() {
            sources = new String[map.size()];
            destinations = new String[map.size()];
        }

        void refresh(PKMediaEntry mediaEntry) {

            if (mediaEntry != null) {
                if (mediaEntry.getMetadata() != null) {
                    for (Map.Entry<String, String> metadataEntry : mediaEntry.getMetadata().entrySet()) {
                        map.put("{{" + metadataEntry.getKey() + "}}", metadataEntry.getValue());
                    }
                }

                map.put("{{entryId}}", mediaEntry.getId());
                map.put("{{entryName}}", mediaEntry.getName());
                map.put("{{entryType}}", mediaEntry.getMediaType().name());
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

        void refresh(PlayerInitOptions initOptions) {

            if (initOptions != null) {
                map.put("{{uiConfId}}", String.valueOf((initOptions.uiConfId != null) ? initOptions.uiConfId : ""));
                map.put("{{partnerId}}", (initOptions.partnerId < 0) ? "" :String.valueOf(initOptions.partnerId));
                map.put("{{ks}}", (initOptions.ks != null) ? initOptions.ks : "");
                map.put("{{referrer}}", (initOptions.referrer != null) ? initOptions.referrer : "");
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

        void refresh(String key,  String value) {
            if (!TextUtils.isEmpty(key)) {
                map.put("{{" + key + "}}", value);
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

        @Override
        public JsonObject resolve(JsonObject pluginJsonConfig) {
            String configStr = pluginJsonConfig.getAsJsonObject().toString();
            JsonParser parser = new JsonParser();
            return parser.parse(resolve(configStr)).getAsJsonObject();
        }
    }

    private JsonObject kavaDefaults(int partnerId, int uiConfId, String referrer) {
        JsonObject object = new JsonObject();
        object.addProperty("partnerId", partnerId);
        object.addProperty("uiConfId", uiConfId);
        object.addProperty("referrer", referrer);
        return object;
    }

    private JsonObject kalturaStatsDefaults(int partnerId, int uiConfId) {
        return new KalturaStatsConfig(uiConfId, partnerId, "", "", 0, true).toJSONObject();
        // KalturaStatsConfig(int uiconfId, int partnerId, String entryId, String userId, int contextId, boolean hasKanalony)
    }

    private JsonObject prepareKava(JsonObject uiConf, int partnerId, int uiConfId, String referrer) {
        return mergeJsonConfig(uiConf, kavaDefaults(partnerId, uiConfId, referrer));
    }

    private void loadPlayer() {
        PKPluginConfigs combinedPluginConfigs = setupPluginsConfiguration();
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

    @NonNull
    private PKPluginConfigs setupPluginsConfiguration() {
        PKPluginConfigs pluginConfigs = initOptions.pluginConfigs;
        PKPluginConfigs combinedPluginConfigs = new PKPluginConfigs();

        GsonReader uiConf = GsonReader.withObject(initOptions.uiConf);

        //JsonObject providerUIConf = (uiConf != null && uiConf.getObject("config") != null && uiConf.getObject("config").getAsJsonObject("player") != null) ? uiConf.getObject("config").getAsJsonObject("player").getAsJsonObject("proivder") : null;

        //JsonObject playbackUIConf = (uiConf != null && uiConf.getObject("config") != null && uiConf.getObject("config").getAsJsonObject("player") != null) ? uiConf.getObject("config").getAsJsonObject("player").getAsJsonObject("playback") : null;

        JsonObject pluginsUIConf = (uiConf != null && uiConf.getObject(CONFIG) != null && uiConf.getObject(CONFIG).getAsJsonObject(PLAYER) != null) ? uiConf.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLUGINS) : new JsonObject();


        // Special case: Kava plugin
        // KAVA
        if (initOptions.uiConfId != null && pluginsUIConf != null) {
            String name = KavaAnalyticsPlugin.factory.getName();
            JsonObject kavaJsonObject = null;
            if (initOptions.pluginConfigs != null && initOptions.pluginConfigs.hasConfig(name)) {
                kavaJsonObject = (JsonObject) initOptions.pluginConfigs.getPluginConfig(name);
            } else {
                kavaJsonObject = kavaDefaults(partnerId, initOptions.uiConfId, referrer);
            }
            pluginsUIConf.add(name, kavaJsonObject);
        }


        // TODO Remove

        // Special case: Kaltura Stats plugin
        // KalturaStats
        if (initOptions.uiConfId != null && pluginsUIConf != null) {
            String name = KalturaStatsPlugin.factory.getName();
            JsonObject kalturaStatPluginObject = null;
            if (initOptions.pluginConfigs != null && initOptions.pluginConfigs.hasConfig(name)) {
                kalturaStatPluginObject = (JsonObject) initOptions.pluginConfigs.getPluginConfig(name);
            } else {
                kalturaStatPluginObject = kalturaStatsDefaults(partnerId, initOptions.uiConfId);
            }
            pluginsUIConf.add(name, kalturaStatPluginObject);
        }


        if (pluginConfigs != null) {
            Gson gson = new Gson();
            for (Map.Entry<String, Object> entry : pluginConfigs) {
                String pluginName = entry.getKey();
                JsonObject appPluginConfig = (JsonObject) entry.getValue();
                if (pluginsUIConf != null && pluginsUIConf.has(pluginName)) {
                    JsonObject uiconfPluginJsonObject = pluginsUIConf.getAsJsonObject(pluginName);
                    if (uiconfPluginJsonObject.has(OPTIONS)) {
                        uiconfPluginJsonObject = uiconfPluginJsonObject.get(OPTIONS).getAsJsonObject();
                    }
                    JsonObject mergedConfig = mergeJsonConfig(uiconfPluginJsonObject, appPluginConfig);
                    if (mergedConfig != null) {
                        combinedPluginConfigs.setPluginConfig(pluginName, tokenResolver.resolve(mergedConfig));
                    }
                } else if (appPluginConfig != null){
                    combinedPluginConfigs.setPluginConfig(pluginName, tokenResolver.resolve(appPluginConfig));
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
                    combinedPluginConfigs.setPluginConfig(pluginName, tokenResolver.resolve(config));
                }
            }
        }

        addKalturaPluginConfigs(combinedPluginConfigs);
        return combinedPluginConfigs;
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
            log.d("key = " + key);

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
                                log.d( "no key " + key + " in target value = " + (JsonPrimitive) sourceValue);
                            } else {
                                target.add(key, null);
                                log.d("no key " + key + " in target value = null");
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
        }
        return null;
    }

    public View setPlayerView(int playerWidth, int playerHeight) {

        ViewGroup.LayoutParams params = pkPlayer.getView().getLayoutParams();
        if (params != null) {
            params.width  = playerWidth;
            params.height = playerHeight;
            pkPlayer.getView().setLayoutParams(params);
        }
        this.view = pkPlayer.getView();
        return view;
    }

    public void setMedia(PKMediaEntry mediaEntry) {
        tokenResolver.refresh(mediaEntry);
        tokenResolver.refresh(initOptions);

        if (this.mediaEntry == null) {
            log.d( "setMedia new Player configuration");
        } else {
            log.d( "setMedia Change Media configuration");
        }

        PKPluginConfigs combinedPluginConfigs = setupPluginsConfiguration();
        updateKalturaPluginConfigs(combinedPluginConfigs);

        this.mediaEntry = mediaEntry;
        prepared = false;

        if (preload) {
            prepare();
        }
    }

    public void setMedia(PKMediaEntry entry, MediaOptions mediaOptions) {
        setStartPosition(mediaOptions.getStartPosition());
        setPreferrdMediaFormat(mediaOptions.getPreferredMediaFormat());
        setAllowCrossProtocolRedirect(initOptions.allowCrossProtocolEnabled);
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
                .setStartPosition((long) (startPosition));

        pkPlayer.getSettings().setPreferredMediaFormat(preferredMeidaFormat);
        pkPlayer.getSettings().setAllowCrossProtocolRedirect(allowCrossProtocolRedirect);
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

    public AdController getController(Class<AdController> adControllerClass) {
        return pkPlayer.getController(adControllerClass);
    }

    public void play() {
        if (!prepared) {
            prepare();
        }
        if(pkPlayer != null) {
            AdController adController = pkPlayer.getController(AdController.class);
            if (adController != null && adController.isAdDisplayed()) {
                adController.play();
            } else {
                pkPlayer.play();
            }
        }
    }

    public void pause() {
        if(pkPlayer != null) {
            AdController adController = pkPlayer.getController(AdController.class);
            if (adController != null && adController.isAdDisplayed()) {
                adController.pause();
            } else {
                pkPlayer.pause();
            }
        }
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

    public <E extends PKEvent> void addListener(Object groupId, Class<E> type, PKEvent.Listener<E> listener) {
        pkPlayer.addListener(groupId, type, listener);
    }

    public void addListener(Object groupId, Enum type, PKEvent.Listener listener) {
        pkPlayer.addListener(groupId, type, listener);
    }

    public void removeListeners(@NonNull Object groupId) {
        pkPlayer.removeListeners(groupId);
    }

    public void removeListener(@NonNull PKEvent.Listener listener) {
        pkPlayer.removeListener(listener);
    }

    public void changeTrack(String uniqueId) {
        pkPlayer.changeTrack(uniqueId);
    }

    public void seekTo(long position) {
        pkPlayer.seekTo(position);
    }

    public AdController getAdController() {
        return pkPlayer.getController(AdController.class);
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

    public PlayerInitOptions getInitOptions() {
        return initOptions;
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
    //public abstract void updatePlugins();
    protected abstract void registerPlugins(Context context);
    protected abstract void addKalturaPluginConfigs(PKPluginConfigs combined);
    protected abstract void updateKalturaPluginConfigs(PKPluginConfigs combined);
    protected abstract void updateKS(String ks);

    public interface OnEntryLoadListener {
        void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error);
    }
}
