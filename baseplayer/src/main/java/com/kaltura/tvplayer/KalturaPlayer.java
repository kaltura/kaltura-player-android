package com.kaltura.tvplayer;

// TODO: rename module from baseplayer to tvplayer


import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaStatsConfig;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.base.OnMediaLoadCompletion;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.utils.TokenResolver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.kaltura.playkit.utils.Consts.DEFAULT_KAVA_PARTNER_ID;

public class KalturaPlayer  {

    private static final PKLog log = PKLog.get("KalturaPlayer");

    public static final String DEFAULT_OVP_SERVER_URL =
            BuildConfig.DEBUG ? "http://cdnapi.kaltura.com/" : "https://cdnapisec.kaltura.com/";

    private static KalturaPlayerType kalturaPlayerType;

    private enum KalturaPlayerType {
        ovp,
        ott,
        basic
    }

    private boolean pluginsRegistered;

    protected String serverUrl;

    private String ks;
    private Integer partnerId;
    private Integer uiConfPartnerId;
    private final Integer uiConfId;
    protected final String referrer;
    private final Context context;
    private Player pkPlayer;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private View view;
    private PKMediaEntry mediaEntry;
    private boolean prepared;
    private Resolver tokenResolver = new Resolver();
    private PlayerInitOptions initOptions;


    public static KalturaPlayer createOVPPlayer(Context context, PlayerInitOptions initOptions) {
        kalturaPlayerType = KalturaPlayerType.ovp;
        KalturaPlayer kalturaPlayer = new KalturaPlayer(context, initOptions);
        kalturaPlayer.serverUrl = KalturaPlayer.safeServerUrl(initOptions.serverUrl, KalturaPlayer.DEFAULT_OVP_SERVER_URL);
        return kalturaPlayer;
    }

    public static KalturaPlayer createOTTPlayer(Context context, PlayerInitOptions initOptions) {
        kalturaPlayerType = KalturaPlayerType.ott;
        KalturaPlayer kalturaPlayer = new KalturaPlayer(context, initOptions);
        kalturaPlayer.serverUrl = KalturaPlayer.safeServerUrl(initOptions.serverUrl, null);
        return kalturaPlayer;
    }

    public static KalturaPlayer createBasicPlayer(Context context, PlayerInitOptions initOptions) {
        kalturaPlayerType = KalturaPlayerType.basic;
        KalturaPlayer kalturaPlayer = new KalturaPlayer(context, initOptions);
        return kalturaPlayer;
    }

    protected KalturaPlayer(Context context, PlayerInitOptions initOptions) {

        this.context = context;
        this.initOptions = initOptions;
        this.preload = initOptions.preload != null ? initOptions.preload : false;
        this.autoPlay = initOptions.autoplay != null ? initOptions.autoplay : false;
        if (this.autoPlay) {
            this.preload = true; // autoplay implies preload
        }
        this.referrer = buildReferrer(context, initOptions.referrer);
        if (kalturaPlayerType == KalturaPlayerType.basic || (kalturaPlayerType == KalturaPlayerType.ott && uiConfPartnerId == null)) {
            this.partnerId = DEFAULT_KAVA_PARTNER_ID;
            this.uiConfPartnerId = DEFAULT_KAVA_PARTNER_ID;
        } else {
            this.partnerId = (initOptions.partnerId != null && initOptions.partnerId > 0) ? initOptions.partnerId : null;
            this.uiConfPartnerId = (initOptions.uiConfPartnerId != null && initOptions.uiConfPartnerId > 0) ? initOptions.uiConfPartnerId : null;
        }
        this.uiConfId = initOptions.uiConfId;
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
                if (mediaEntry.getMediaType() != null) {
                    map.put("{{entryType}}", mediaEntry.getMediaType().name());
                }
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
                if (initOptions.uiConfId != null) {
                     map.put("{{uiConfId}}", String.valueOf(initOptions.uiConfId));
                }
                if (initOptions.partnerId != null) {
                    map.put("{{partnerId}}", String.valueOf(initOptions.partnerId));
                }
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

    private JsonObject kavaDefaults(Integer partnerId, Integer uiconfId, String referrer) {
        JsonObject kavaAnalyticsConfigJson = new JsonObject();
        kavaAnalyticsConfigJson.addProperty(KavaAnalyticsConfig.BASE_URL, KavaAnalyticsConfig.DEFAULT_BASE_URL);
        kavaAnalyticsConfigJson.addProperty(KavaAnalyticsConfig.DVR_THRESHOLD, Consts.DISTANCE_FROM_LIVE_THRESHOLD);
        kavaAnalyticsConfigJson.addProperty(KavaAnalyticsConfig.PARTNER_ID, partnerId);
        if (uiconfId != null && uiconfId > 0) {
            kavaAnalyticsConfigJson.addProperty(KavaAnalyticsConfig.UICONF_ID, uiconfId);
        }
        if (partnerId == Integer.valueOf(Consts.DEFAULT_KAVA_PARTNER_ID)) {
            kavaAnalyticsConfigJson.addProperty(KavaAnalyticsConfig.ENTRY_ID, Consts.DEFAULT_KAVA_ENTRY_ID);
        } else if (mediaEntry != null && mediaEntry.getId() != null) {
            kavaAnalyticsConfigJson.addProperty(KavaAnalyticsConfig.ENTRY_ID, mediaEntry.getId());
        }
        if (!TextUtils.isEmpty(ks)) {
            kavaAnalyticsConfigJson.addProperty(KavaAnalyticsConfig.KS, ks);
        }
        if (!TextUtils.isEmpty(referrer)) {
            kavaAnalyticsConfigJson.addProperty(KavaAnalyticsConfig.REFERRER, referrer);
        }
        return kavaAnalyticsConfigJson;
    }

    private JsonObject kalturaStatsDefaults(int partnerId, int uiConfId) {
        return new KalturaStatsConfig(uiConfId, partnerId, "", "", 0, true).toJson();
        // KalturaStatsConfig(int uiconfId, int partnerId, String entryId, String userId, int contextId, boolean hasKanalony)
    }

    private void loadPlayer() {
        PKPluginConfigs combinedPluginConfigs = setupPluginsConfiguration();
        pkPlayer = PlayKitManager.loadPlayer(context, combinedPluginConfigs); //pluginConfigs
        updatePlayerSettings();

        PlayManifestRequestAdapter.install(pkPlayer, referrer);
    }

    private void updatePlayerSettings() {
        if (initOptions.audioLanguageMode != null && initOptions.audioLanguage != null) {
            pkPlayer.getSettings().setPreferredAudioTrack(new PKTrackConfig().setPreferredMode(initOptions.audioLanguageMode).setTrackLanguage(initOptions.audioLanguage));
        }
        if (initOptions.textLanguageMode != null && initOptions.textLanguage != null) {
            pkPlayer.getSettings().setPreferredTextTrack(new PKTrackConfig().setPreferredMode(initOptions.textLanguageMode).setTrackLanguage(initOptions.textLanguage));
        }

        if (initOptions.allowCrossProtocolEnabled != null) {
            pkPlayer.getSettings().setAllowCrossProtocolRedirect(initOptions.allowCrossProtocolEnabled);
        }

        if (initOptions.preferredMediaFormat != null) {
            pkPlayer.getSettings().setPreferredMediaFormat(initOptions.preferredMediaFormat);
        }

        if (initOptions.allowClearLead != null) {
            pkPlayer.getSettings().allowClearLead(initOptions.allowClearLead);
        }

        if (initOptions.secureSurface != null) {
            pkPlayer.getSettings().setSecureSurface(initOptions.secureSurface);
        }

        if (initOptions.setSubtitleStyle != null) {
            pkPlayer.getSettings().setSubtitleStyle(initOptions.setSubtitleStyle);//(new SubtitleStyleSettings("Default"));
        }

        if (initOptions.adAutoPlayOnResume != null) {
            pkPlayer.getSettings().setAdAutoPlayOnResume(initOptions.adAutoPlayOnResume);
        }

        if (initOptions.vrPlayerEnabled != null) {
            pkPlayer.getSettings().setVRPlayerEnabled(initOptions.vrPlayerEnabled);
        }

        if (initOptions.aspectRatioResizeMode != null) {
            pkPlayer.getSettings().setSurfaceAspectRatioResizeMode(initOptions.aspectRatioResizeMode);//(PKAspectRatioResizeMode.fit);
        }

        if (initOptions.contentRequestAdapter != null) {
            //PKRequestParams.Adapter contentAdapter = null;
            pkPlayer.getSettings().setContentRequestAdapter(initOptions.contentRequestAdapter);//(contentAdapter);
        }

        if (initOptions.licenseRequestAdapter != null) {
            //PKRequestParams.Adapter licenseAdapter = null;
            pkPlayer.getSettings().setLicenseRequestAdapter(initOptions.licenseRequestAdapter);//(licenseAdapter);
        }

        if (initOptions.loadControlBuffers != null) {
            pkPlayer.getSettings().setPlayerBuffers(initOptions.loadControlBuffers);//(new LoadControlBuffers());
        }

        if (initOptions.abrSettings != null) {
            pkPlayer.getSettings().setABRSettings(initOptions.abrSettings);//(new ABRSettings());
        }
    }

    @NonNull
    private PKPluginConfigs setupPluginsConfiguration() {
        PKPluginConfigs pluginConfigs = initOptions.pluginConfigs;
        PKPluginConfigs combinedPluginConfigs = new PKPluginConfigs();

        if (pluginConfigs != null) {
            Gson gson = new Gson();
            for (Map.Entry<String, Object> entry : pluginConfigs) {
                String pluginName = entry.getKey();
                if (entry.getValue() instanceof JsonObject) {
                    JsonObject appPluginConfig = (JsonObject) entry.getValue();
                    if (appPluginConfig != null) {
                        combinedPluginConfigs.setPluginConfig(pluginName, tokenResolver.resolve(appPluginConfig));
                    }
                }
            }

        }
        addKalturaPluginConfigs(combinedPluginConfigs);
        return combinedPluginConfigs;
    }

    // TODO: 2019-05-08 Remove commented-out code 

//    @NonNull
//    @Deprecated
//    private PKPluginConfigs setupPluginsConfigurationOld() {
//        PKPluginConfigs pluginConfigs = initOptions.pluginConfigs;
//        PKPluginConfigs combinedPluginConfigs = new PKPluginConfigs();
//
//        GsonReader uiConf = GsonReader.withObject(initOptions.uiConfJsonObjet);
//
//        //JsonObject providerUIConf = (uiConfJsonObjet != null && uiConfJsonObjet.getObject("config") != null && uiConfJsonObjet.getObject("config").getAsJsonObject("player") != null) ? uiConfJsonObjet.getObject("config").getAsJsonObject("player").getAsJsonObject("proivder") : null;
//
//        //JsonObject playbackUIConf = (uiConfJsonObjet != null && uiConfJsonObjet.getObject("config") != null && uiConfJsonObjet.getObject("config").getAsJsonObject("player") != null) ? uiConfJsonObjet.getObject("config").getAsJsonObject("player").getAsJsonObject("playback") : null;
//
//        JsonObject pluginsUIConf = (uiConf != null && uiConf.getObject(CONFIG) != null && uiConf.getObject(CONFIG).getAsJsonObject(PLAYER) != null) ? uiConf.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLUGINS) : new JsonObject();
//
//        Set<String> pluginsInUiConf =  pluginsUIConf.keySet();
//        if (pluginsInUiConf != null) {
//            Iterator<String> it = pluginsInUiConf.iterator();
//            while (it.hasNext()) {
//                String pluginName = it.next();
//                if (pluginsUIConf.getAsJsonObject(pluginName).has(OPTIONS)){
//                    pluginsUIConf.add(pluginName, pluginsUIConf.getAsJsonObject(pluginName).get(OPTIONS).getAsJsonObject());
//                    break;
//                }
//            }
//        }
//
//        // Special case: Kava plugin
//        // KAVA
//        if (initOptions.uiConfId != null && pluginsUIConf != null) {
//            String name = KavaAnalyticsPlugin.factory.getName();
//            JsonObject kavaJsonObject = null;
//            if (initOptions.pluginConfigs != null && initOptions.pluginConfigs.hasConfig(name)) {
//                kavaJsonObject = (JsonObject) initOptions.pluginConfigs.getPluginConfig(name);
//            } else {
//                kavaJsonObject = kavaDefaults(partnerId, referrer);
//            }
//            pluginsUIConf.add(name, kavaJsonObject);
//        }
//
//        // TODO Remove?
//
//        // Special case: Kaltura Stats plugin
//        // KalturaStats
//        if (initOptions.uiConfId != null && pluginsUIConf != null) {
//            String name = KalturaStatsPlugin.factory.getName();
//            JsonObject kalturaStatPluginObject = null;
//            if (initOptions.pluginConfigs != null && initOptions.pluginConfigs.hasConfig(name)) {
//                kalturaStatPluginObject = (JsonObject) initOptions.pluginConfigs.getPluginConfig(name);
//            } else {
//                kalturaStatPluginObject = kalturaStatsDefaults(partnerId, uiConfId);
//            }
//            pluginsUIConf.add(name, kalturaStatPluginObject);
//        }
//
//        if (pluginConfigs != null) {
//            Gson gson = new Gson();
//            for (Map.Entry<String, Object> entry : pluginConfigs) {
//                String pluginName = entry.getKey();
//                JsonObject appPluginConfig = (JsonObject) entry.getValue();
//                if (pluginsUIConf != null && pluginsUIConf.has(pluginName)) {
//                    JsonObject uiconfPluginJsonObject = pluginsUIConf.getAsJsonObject(pluginName);
//                    JsonObject mergedConfig = mergeJsonConfig(uiconfPluginJsonObject, appPluginConfig);
//                    if (mergedConfig != null) {
//                        combinedPluginConfigs.setPluginConfig(pluginName, tokenResolver.resolve(mergedConfig));
//                    }
//                } else if (appPluginConfig != null){
//                    combinedPluginConfigs.setPluginConfig(pluginName, tokenResolver.resolve(appPluginConfig));
//                }
//            }
//        }
//
//        // Add the plugins that are ONLY mentioned in UIConf and not merged yet
//        if (pluginsUIConf != null && pluginsUIConf.keySet() != null) {
//            for (String pluginName : pluginsUIConf.keySet()) {
//                if (combinedPluginConfigs.hasConfig(pluginName)) {
//                    continue;
//                }
//                JsonObject config = pluginsUIConf.getAsJsonObject(pluginName);
//                if (config != null) {
//                    combinedPluginConfigs.setPluginConfig(pluginName, tokenResolver.resolve(config));
//                }
//            }
//        }
//
//        addKalturaPluginConfigs(combinedPluginConfigs);
//        return combinedPluginConfigs;
//    }

//    public JsonObject mergeJsonConfig(JsonObject source, JsonObject target) {
//        if (source == null && target != null) {
//            return target;
//        } else if (source != null && target == null) {
//            return source;
//        } else if (source == null || target == null) {
//            return null;
//        }
//
//        for (String key: source.keySet()) {
//            log.d("key = " + key);
//
//            Object sourceValue = source.get(key);
//            boolean isSourceValueIsNull = (sourceValue == null || (sourceValue != null && source.get(key).isJsonNull()));
//            if (!target.has(key)) {
//                // new value for "key":
//                if (!isSourceValueIsNull) {
//                    if (sourceValue instanceof JsonArray) {
//                        target.add(key, (JsonArray) sourceValue);
//                    }
//                    if (sourceValue instanceof JsonPrimitive) {
//                        target.add(key, (JsonPrimitive) sourceValue);
//                    }
//                } else {
//                    target.add(key,null);
//                }
//            } else {
//                // existing value for "key" - recursively deep merge:
//                if (sourceValue instanceof JsonObject) {
//                    JsonObject valueJson = (JsonObject) sourceValue;
//                    mergeJsonConfig(valueJson, target.get(key).getAsJsonObject());
//                } else {
//                    Object targetValue = target.get(key);
//                    boolean isTargetValueIsNull = (targetValue == null || (targetValue != null && target.get(key).isJsonNull()));
//                    if (sourceValue instanceof JsonArray) {
//                        if (!isTargetValueIsNull) {
//                            target.add(key, (JsonArray) targetValue);
//                        }
//                    } else if (sourceValue instanceof JsonPrimitive) {
//                        if (target.has(key)) {
//                            if (!isTargetValueIsNull) {
//                                JsonPrimitive targetPrimitiveVal = (JsonPrimitive) targetValue;
//                                if (targetPrimitiveVal.isString()) {
//                                    if ("".equals(targetPrimitiveVal.getAsString()) && !isSourceValueIsNull) {
//                                        target.add(key, (JsonPrimitive) sourceValue);
//                                    } else {
//                                        target.add(key, targetPrimitiveVal);
//                                    }
//                                } else if (!targetPrimitiveVal.isString()) {
//                                    target.add(key, targetPrimitiveVal);
//                                }
//                            } else if (!isSourceValueIsNull) {
//                                target.add(key, (JsonPrimitive) sourceValue);
//                            }
//                        } else {
//                            if (!isSourceValueIsNull) {
//                                target.add(key, (JsonPrimitive) sourceValue);
//                                log.d( "no key " + key + " in target value = " + (JsonPrimitive) sourceValue);
//                            } else {
//                                target.add(key, null);
//                                log.d("no key " + key + " in target value = null");
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return target;
//    }

    public View getView() {
        if (this.view != null) {
            return view;
        }
        return null;
    }

    // TODO: 2019-05-08 I don't think this method should return the view 
    // It should be called createPlayerView and return void. Add getPlayerView().
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

    public void setMedia(@NonNull PKMediaEntry mediaEntry) {
        tokenResolver.refresh(mediaEntry);
        tokenResolver.refresh(initOptions);

        PKPluginConfigs combinedPluginConfigs = setupPluginsConfiguration();
        updateKalturaPluginConfigs(combinedPluginConfigs);

        this.mediaEntry = mediaEntry;
        prepared = false;

        if (preload) {
            prepare();
        }
    }

    public void setMedia(PKMediaEntry entry, Long startPosition) {
        if (startPosition != null) {
            setStartPosition(startPosition);
        }
        setMedia(entry);
    }

    protected void registerCommonPlugins(Context context) {
        KnownPlugin.registerAll(context);
    }

    public void setKS(String ks) {
        this.ks = ks;
    }

    public void prepare() {

        // TODO: 2019-05-08 Also add a flag "preparing" to make sure we're not in the middle of prepare
        // when preload is true, if the app calls setMedia() and immediately calls prepare(), we'll have a
        // problem -- the player will prepare twice. need to prevent it.
        if (prepared) {
            return;
        }

        final PKMediaConfig config = new PKMediaConfig()
                .setMediaEntry(mediaEntry)
                .setStartPosition((long) (startPosition));

        pkPlayer.prepare(config);
        prepared = true;

        if (autoPlay) {
            pkPlayer.play();
        }
    }

    public PKMediaEntry getMediaEntry() {
        return mediaEntry;
    }

    public Integer getUiConfId() {
        return uiConfId;
    }


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

    public <T extends PKController> T getController(Class<T> type) {
        return pkPlayer.getController(type);
    }

    public void play() {
        if (!prepared) {
            prepare();
        }
        if(pkPlayer != null) {
            pkPlayer.play();
        }
    }

    public void pause() {
        if(pkPlayer != null) {
            pkPlayer.pause();
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

    // TODO: 2019-05-08 remove this?
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

    // TODO: 2019-05-08 Why is this needed?
    public double getStartPosition() {
        return startPosition;
    }

    public KalturaPlayer setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    // TODO: 2019-05-08 Why is this needed?
    public boolean isPreload() {
        return preload;
    }

    public KalturaPlayer setPreload(boolean preload) {
        this.preload = preload;
        return this;
    }

    // TODO: 2019-05-08 Why is this needed?
    public boolean isAutoPlay() {
        return autoPlay;
    }

    public KalturaPlayer setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    // TODO: 2019-05-08 Why is this needed?
    public PlayerInitOptions getInitOptions() {
        return initOptions;
    }

    // TODO: 2019-05-08 Why is this needed?
    public int getPartnerId() {
        return partnerId;
    }

    // TODO: 2019-05-08 Why is this needed?
    public String getKS() {
        return ks;
    }

    // TODO: 2019-05-08 Why is this needed?
    public String getServerUrl() {
        return serverUrl;
    }

    // Called by implementation of loadMedia().
    private void mediaLoadCompleted(final ResultElement<PKMediaEntry> response, final OnEntryLoadListener onEntryLoadListener) {
        final PKMediaEntry entry = response.getResponse();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (entry != null) {
                    setMedia(entry);
                }
                onEntryLoadListener.onEntryLoadComplete(entry, response.getError());
            }
        });
    }

    public void loadMedia(OVPMediaOptions mediaOptions, final OnEntryLoadListener listener) {

        if (kalturaPlayerType == KalturaPlayerType.basic) {
            log.e("loadMedia api for player type KalturaPlayerType.basic is not supported");
            return;
        }

        if (kalturaPlayerType == KalturaPlayerType.ott) {
            log.e("loadMedia with OVPMediaOptions for player type KalturaPlayerType.ott is not supported");
            return;
        }

        if (mediaOptions.getKs() != null) {
            setKS(mediaOptions.getKs());
        }

        setStartPosition(mediaOptions.getStartPosition());

        MediaEntryProvider provider = new KalturaOvpMediaProvider(getServerUrl(), getPartnerId(), getKS())
                .setEntryId(mediaOptions.entryId).setReferrer(referrer);

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, listener);
            }
        });
    }

    public void loadMedia(OTTMediaOptions mediaOptions, final OnEntryLoadListener listener) {

        if (kalturaPlayerType == KalturaPlayerType.basic) {
            log.e("loadMedia api for player type KalturaPlayerType.basic is not supported");
            return;
        }

        if (kalturaPlayerType == KalturaPlayerType.ovp) {
            log.e("loadMedia with OTTMediaOptions for player type KalturaPlayerType.ovp is not supported");
            return;
        }

        if (mediaOptions.getKs() != null) {
            setKS(mediaOptions.getKs());
        }

        setStartPosition(mediaOptions.getStartPosition());

        final PhoenixMediaProvider provider = new PhoenixMediaProvider(getServerUrl(), getPartnerId(), getKS())
                .setAssetId(mediaOptions.assetId).setReferrer(referrer);

        if (mediaOptions.getProtocol() != null) {
            provider.setProtocol(mediaOptions.getProtocol());
        }

        if (mediaOptions.fileIds != null) {
            provider.setFileIds(mediaOptions.fileIds);
        }

        if (mediaOptions.contextType != null) {
            provider.setContextType(mediaOptions.contextType);
        }

        if (mediaOptions.assetType != null) {
            provider.setAssetType(mediaOptions.assetType);
        }

        if (mediaOptions.formats != null) {
            provider.setFormats(mediaOptions.formats);
        }

        if (mediaOptions.assetReferenceType != null) {
            provider.setAssetReferenceType(mediaOptions.assetReferenceType);
        }

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, listener);
            }
        });
    }

    protected void registerPlugins(Context context) {
        // Plugin registration is static and only done once, but requires a Context.
        if (!pluginsRegistered) {
            registerCommonPlugins(context);
            if (isOTTPlayer()) {
                registerPluginsOTT(context);
            }
            pluginsRegistered = true;
        }
    }

    private boolean isOTTPlayer() {
        return KalturaPlayerType.ott.equals(kalturaPlayerType);
    }

    protected void registerPluginsOTT(Context context) {
        PlayKitManager.registerPlugins(context, PhoenixAnalyticsPlugin.factory);
    }

    protected void addKalturaPluginConfigs(PKPluginConfigs combinedPluginConfigs) {
        if (!combinedPluginConfigs.hasConfig(KavaAnalyticsPlugin.factory.getName())) {
            log.d("Adding Automatic Kava Plugin");
            combinedPluginConfigs.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), tokenResolver.resolve(kavaDefaults(uiConfPartnerId, uiConfId, referrer)));
        }
        if (isOTTPlayer() && !combinedPluginConfigs.hasConfig(PhoenixAnalyticsPlugin.factory.getName())) {
            addKalturaPluginConfigsOTT(combinedPluginConfigs);
        }
    }

    protected void addKalturaPluginConfigsOTT(PKPluginConfigs combined) {
        //NOT ADDING PHOENIX IF KS IS NOT VALID
        if (!TextUtils.isEmpty(getKS())) {
            PhoenixAnalyticsConfig phoenixConfig = getPhoenixAnalyticsConfig();
            if (phoenixConfig != null) {
                combined.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixConfig);
            }
        }
    }

    protected void updateKalturaPluginConfigs(PKPluginConfigs combined) {
        log.d("updateKalturaPluginConfigs");
        for (Map.Entry<String, Object> plugin : combined) {
            if (plugin.getValue() instanceof JsonObject) {
                if (isOTTPlayer() && PhoenixAnalyticsPlugin.factory.getName().equals(plugin.getKey()) && !TextUtils.isEmpty(getKS())) {
                    PhoenixAnalyticsConfig phoenixConfig = getPhoenixAnalyticsConfig();
                    if (phoenixConfig != null) {
                        updatePluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixConfig);
                    }
                } else {
                    updatePluginConfig(plugin.getKey(), plugin.getValue());
                }
            } else {
                log.e("updateKalturaPluginConfigs " + plugin.getKey()  + " is not a JsonObject");
            }
        }
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig() {
        KavaAnalyticsConfig kavaAnalyticsConfig = new  KavaAnalyticsConfig().setPartnerId(getPartnerId()).setReferrer(referrer);
        if (getUiConfId() != null) {
            kavaAnalyticsConfig.setUiConfId(getUiConfId());
        }
        return kavaAnalyticsConfig;
    }

    // TODO: 2019-05-08 Remove
    private KalturaLiveStatsConfig getLiveStatsConfig() {
        final PKMediaEntry mediaEntry = getMediaEntry();
        return new KalturaLiveStatsConfig(getPartnerId(), mediaEntry != null ? mediaEntry.getId() : null);
    }

    private PhoenixAnalyticsConfig getPhoenixAnalyticsConfig() {
        // Special case: Phoenix plugin
        // Phoenix
        String name = PhoenixAnalyticsPlugin.factory.getName();
        JsonObject phoenixAnalyticObject;
        if (getInitOptions().pluginConfigs.hasConfig(name)) {
            phoenixAnalyticObject = (JsonObject) getInitOptions().pluginConfigs.getPluginConfig(name);
        } else {
            phoenixAnalyticObject = phoenixAnalyticDefaults(getPartnerId(), getServerUrl(), getKS(), Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH);
        }
        return new Gson().fromJson(phoenixAnalyticObject, PhoenixAnalyticsConfig.class);
    }

    private JsonObject phoenixAnalyticDefaults(int partnerId, String serverUrl, String ks, int timerInterval) {
        return new PhoenixAnalyticsConfig(partnerId, serverUrl, ks, timerInterval).toJson();
    }

    public interface OnEntryLoadListener {
        void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error);
    }
}
