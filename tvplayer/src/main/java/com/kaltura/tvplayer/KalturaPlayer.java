package com.kaltura.tvplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

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
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdController;

import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.playback.KalturaUDRMLicenseRequestAdapter;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.config.KalturaPlayerNotInitializedException;
import com.kaltura.tvplayer.config.PhoenixTVPlayerParams;
import com.kaltura.tvplayer.utils.ConfigResolver;
import com.kaltura.tvplayer.utils.NetworkUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

public class KalturaPlayer  {

    private static final PKLog log = PKLog.get("KalturaPlayer");

    public static final String DEFAULT_OVP_SERVER_URL =
            BuildConfig.DEBUG ? "http://cdnapi.kaltura.com/" : "https://cdnapisec.kaltura.com/";

    private static Boolean playerConfigRetreived;

    private enum PrepareState {
        not_prepared,
        preparing,
        prepared
    }

    private boolean pluginsRegistered;

    private TVPlayerType tvPlayerType;
    private Integer partnerId;
//    protected String serverUrl;
    private Integer ovpPartnerId;
    private String ks;

    protected final String referrer;
    private final Context context;
    private Player pkPlayer;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private double startPosition;
    private List<PKExternalSubtitle> externalSubtitles;
    private View view;
    private PKMediaEntry mediaEntry;
    private PrepareState prepareState = PrepareState.not_prepared;
    private PlayerTokenResolver tokenResolver = new PlayerTokenResolver();
    private PlayerInitOptions initOptions;

    public static void initialize(Context context, TVPlayerType tvPlayerType, int apiPartnerId, String serverUrl) {

        PlayerConfigManager.retrieve(context, tvPlayerType, apiPartnerId, serverUrl, (config, error, freshness) -> {
           if (error != null) {
               log.e("initialize KalturaPlayerType failed");
           } else {
               playerConfigRetreived = true;
           }
        });
    }

    public static KalturaPlayer createOVPPlayer(Context context, PlayerInitOptions initOptions) throws KalturaPlayerNotInitializedException{
        if ((playerConfigRetreived != null && playerConfigRetreived) || (initOptions != null && initOptions.tvPlayerParams != null)) {
            if (playerConfigRetreived) {
                initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(TVPlayerType.ovp, initOptions.partnerId));
            }
            KalturaPlayer kalturaPlayer = new KalturaPlayer(context, TVPlayerType.ovp, initOptions);
            return kalturaPlayer;
        } else {
            throw new KalturaPlayerNotInitializedException();
        }
    }

    public static KalturaPlayer createOTTPlayer(Context context, PlayerInitOptions initOptions) throws KalturaPlayerNotInitializedException {
        if ((playerConfigRetreived != null && playerConfigRetreived) || (initOptions != null && initOptions.tvPlayerParams != null)) {
            if (playerConfigRetreived) {
                initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(TVPlayerType.ott, initOptions.partnerId));
            }
            return new KalturaPlayer(context, TVPlayerType.ott, initOptions);
        } else {
            throw new KalturaPlayerNotInitializedException();
        }
    }

    public static KalturaPlayer createBasicPlayer(Context context, PlayerInitOptions initOptions) {
        KalturaPlayer kalturaPlayer = new KalturaPlayer(context, TVPlayerType.basic, initOptions);
        return kalturaPlayer;
    }

    protected KalturaPlayer(Context context, TVPlayerType tvPlayerType, PlayerInitOptions initOptions) {

        this.context = context;
        this.tvPlayerType = tvPlayerType;
        this.initOptions = initOptions;
        this.preload = initOptions.preload != null ? initOptions.preload : false;
        this.autoPlay = initOptions.autoplay != null ? initOptions.autoplay : false;
        if (this.autoPlay) {
            this.preload = true; // autoplay implies preload
        }
        this.referrer = buildReferrer(context, initOptions.referrer);
        if (tvPlayerType == TVPlayerType.basic || (tvPlayerType == TVPlayerType.ott && kavaPartnerIdIsMissing(initOptions))) {
            if (tvPlayerType == TVPlayerType.basic) {
                this.partnerId = KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID;
            } else {
                this.partnerId= initOptions.tvPlayerParams.partnerId;
            }
            this.ovpPartnerId = KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID;
        } else {
            this.partnerId = (initOptions.tvPlayerParams.partnerId != null && initOptions.tvPlayerParams.partnerId > 0) ? initOptions.tvPlayerParams.partnerId : null;
            if (tvPlayerType == TVPlayerType.ott) {
                this.ovpPartnerId = (initOptions.tvPlayerParams != null && ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId != null &&
                        ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId > 0) ? ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId : null;
            } else {
                this.ovpPartnerId =  initOptions.tvPlayerParams.partnerId;
            }
        }

        this.ks = initOptions.ks;

        registerPlugins(context);
        loadPlayer();
    }

    private boolean kavaPartnerIdIsMissing(PlayerInitOptions initOptions) {
        return (initOptions.tvPlayerParams == null ||
                (initOptions.tvPlayerParams instanceof PhoenixTVPlayerParams && ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId == null));
    }

    protected static String safeServerUrl(TVPlayerType tvPlayerType, String url, String defaultUrl) {
        String serviceURL = url;
        if (TextUtils.isEmpty(serviceURL)) {
            serviceURL = defaultUrl;
        } else if (tvPlayerType != null && TVPlayerType.ott.equals(tvPlayerType)) {
            if (!serviceURL.endsWith(OvpConfigs.ApiPrefix) && !serviceURL.endsWith(OvpConfigs.ApiPrefix.substring(0, OvpConfigs.ApiPrefix.length() - 1))) {
                if (!serviceURL.endsWith(File.separator)) {
                    serviceURL += File.separator;
                }
                serviceURL += OvpConfigs.ApiPrefix;
            }
        }

        if (serviceURL != null && !serviceURL.endsWith(File.separator)) {
            serviceURL =  serviceURL + File.separator;
        }
        return serviceURL;
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

    private Object resolve(Object config) {
        return ConfigResolver.resolve(config, tokenResolver);
    }

    private KavaAnalyticsConfig getKavaDefaultsConfig(Integer partnerId, String referrer) {

        KavaAnalyticsConfig kavaAnalyticsConfig = new KavaAnalyticsConfig();
        if (initOptions.tvPlayerParams != null) {
            if (initOptions.tvPlayerParams.analyticsUrl != null) {
                String analyticsUrl = initOptions.tvPlayerParams.analyticsUrl;
                if (!analyticsUrl.contains(OvpConfigs.ApiPrefix)) {
                    analyticsUrl += "/api_v3/index.php";
                }
                kavaAnalyticsConfig.setBaseUrl(analyticsUrl);
            }
            if (initOptions.tvPlayerParams.uiConfId != null && initOptions.tvPlayerParams.uiConfId > 0) {
                kavaAnalyticsConfig.setUiConfId(initOptions.tvPlayerParams.uiConfId);
            }
        } else {
            kavaAnalyticsConfig.setBaseUrl(KavaAnalyticsConfig.DEFAULT_BASE_URL);
        }

        kavaAnalyticsConfig.setDvrThreshold(Consts.DISTANCE_FROM_LIVE_THRESHOLD);
        kavaAnalyticsConfig.setPartnerId(partnerId);

        if (partnerId != null && partnerId.equals(Integer.valueOf(KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID))) {
            kavaAnalyticsConfig.setEntryId(KavaAnalyticsConfig.DEFAULT_KAVA_ENTRY_ID);
        } else if (mediaEntry != null && mediaEntry.getId() != null){
            if (!isOTTPlayer()) {
                kavaAnalyticsConfig.setEntryId(mediaEntry.getId());
            }
        }

        if (!TextUtils.isEmpty(ks)) {
            kavaAnalyticsConfig.setKs(ks);
        }
        if (!TextUtils.isEmpty(referrer)) {
            kavaAnalyticsConfig.setReferrer(referrer);
        }
        return kavaAnalyticsConfig;
    }

    private void loadPlayer() {
        tokenResolver.update(initOptions);
        PKPluginConfigs combinedPluginConfigs = setupPluginsConfiguration();
        pkPlayer = PlayKitManager.loadPlayer(context, combinedPluginConfigs); //pluginConfigs
        updatePlayerSettings();
        if (Integer.valueOf(KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID).equals(ovpPartnerId)) {
            NetworkUtils.sendKavaImpression(context);
        }
    }

    private void updatePlayerSettings() {
        if (initOptions.referrer != null) {
            PlayManifestRequestAdapter.install(pkPlayer, initOptions.referrer);
            KalturaUDRMLicenseRequestAdapter.install(pkPlayer, initOptions.referrer);
        }

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

        if (initOptions.isVideoViewHidden != null) {
            pkPlayer.getSettings().setHideVideoViews(initOptions.isVideoViewHidden);
        }

        if (initOptions.aspectRatioResizeMode != null) {
            pkPlayer.getSettings().setSurfaceAspectRatioResizeMode(initOptions.aspectRatioResizeMode);//(PKAspectRatioResizeMode.fit);
        }

        if (initOptions.contentRequestAdapter != null) {
            pkPlayer.getSettings().setContentRequestAdapter(initOptions.contentRequestAdapter);//(contentAdapter);
        }

        if (initOptions.licenseRequestAdapter != null) {
            pkPlayer.getSettings().setLicenseRequestAdapter(initOptions.licenseRequestAdapter);//(licenseAdapter);
        }

        if (initOptions.loadControlBuffers != null) {
            pkPlayer.getSettings().setPlayerBuffers(initOptions.loadControlBuffers);//(new LoadControlBuffers());
        }

        if (initOptions.abrSettings != null) {
            pkPlayer.getSettings().setABRSettings(initOptions.abrSettings);//(new ABRSettings());
        }

        if (initOptions.vrPlayerEnabled != null) {
            pkPlayer.getSettings().setVRPlayerEnabled(initOptions.vrPlayerEnabled);
        }

        if (initOptions.vrSettings != null) {
            pkPlayer.getSettings().setVRSettings(initOptions.vrSettings);
        }

        if (initOptions.forceSinglePlayerEngine != null) {
            pkPlayer.getSettings().forceSinglePlayerEngine(initOptions.forceSinglePlayerEngine);
        }

        if (initOptions.cea608CaptionsEnabled != null) {
            pkPlayer.getSettings().setCea608CaptionsEnabled(initOptions.cea608CaptionsEnabled);
        }

        if (initOptions.mpgaAudioFormatEnabled != null) {
            pkPlayer.getSettings().setMpgaAudioFormatEnabled(initOptions.mpgaAudioFormatEnabled);
        }

        if (initOptions.useTextureView != null) {
            pkPlayer.getSettings().useTextureView(initOptions.useTextureView);
        }

    }

    @NonNull
    private PKPluginConfigs setupPluginsConfiguration() {
        PKPluginConfigs pluginConfigs = initOptions.pluginConfigs;
        PKPluginConfigs combinedPluginConfigs = new PKPluginConfigs();

        if (pluginConfigs != null) {
            for (Map.Entry<String, Object> entry : pluginConfigs) {
                String pluginName = entry.getKey();
                combinedPluginConfigs.setPluginConfig(pluginName, resolve(entry.getValue()));
            }
        }
        addKalturaPluginConfigs(combinedPluginConfigs);
        return combinedPluginConfigs;
    }

    public View getPlayerView() {
        return view;
    }

    public void setPlayerView(int playerWidth, int playerHeight) {

        ViewGroup.LayoutParams params = pkPlayer.getView().getLayoutParams();
        if (params != null) {
            params.width  = playerWidth;
            params.height = playerHeight;
            pkPlayer.getView().setLayoutParams(params);
        }
        this.view = pkPlayer.getView();
    }

    public void setMedia(@NonNull PKMediaEntry mediaEntry) {
        tokenResolver.update(mediaEntry, getKS());

        if (externalSubtitles != null) {
            if (mediaEntry.getExternalSubtitleList() == null) {
                mediaEntry.setExternalSubtitleList(externalSubtitles);
            } else {
                mediaEntry.getExternalSubtitleList().addAll(externalSubtitles);
            }
        }

        PKPluginConfigs combinedPluginConfigs = setupPluginsConfiguration();
        updateKalturaPluginConfigs(combinedPluginConfigs);

        this.mediaEntry = mediaEntry;
        prepareState = PrepareState.not_prepared;

        if (preload) {
            prepare();
        }
    }

    public void setMedia(PKMediaEntry entry, Long startPosition) {
        externalSubtitles = null;
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

        if (prepareState == PrepareState.preparing) {
            return;
        }

        final PKMediaConfig config = new PKMediaConfig()
                .setMediaEntry(mediaEntry)
                .setStartPosition((long) (startPosition));

        pkPlayer.prepare(config);
        prepareState = PrepareState.preparing;
        pkPlayer.addListener(this, PlayerEvent.canPlay, new PKEvent.Listener<PlayerEvent>() {
            @Override
            public void onEvent(PlayerEvent event) {
                prepareState = PrepareState.prepared;
                pkPlayer.removeListener(this);
            }
        });
        if (autoPlay) {
            pkPlayer.play();
        }
    }

    public PKMediaEntry getMediaEntry() {
        return mediaEntry;
    }

    public void updatePluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
        pkPlayer.updatePluginConfig(pluginName, pluginConfig);
    }

    public void updateSubtitleStyle(SubtitleStyleSettings subtitleStyleSettings) {
        if (pkPlayer != null) {
            pkPlayer.updateSubtitleStyle(subtitleStyleSettings);
        }
    }

    public void setPlaybackRate(float rate) {
        if (pkPlayer != null) {
            pkPlayer.setPlaybackRate(rate);
        }
    }

    public void updateSurfaceAspectRatioResizeMode(PKAspectRatioResizeMode resizeMode) {
        pkPlayer.updateSurfaceAspectRatioResizeMode(resizeMode);
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
        if (prepareState == PrepareState.not_prepared) {
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
        if (initOptions.tvPlayerParams != null) {
            return initOptions.tvPlayerParams.partnerId;
        }
        return 0;
    }

    public String getKS() {
        return ks;
    }

    public String getServerUrl() {
        if (initOptions.tvPlayerParams.serviceUrl != null) {
            return initOptions.tvPlayerParams.serviceUrl;
        }
        return null;
    }

    // Called by implementation of loadMedia().
    private void mediaLoadCompleted(final ResultElement<PKMediaEntry> response, final OnEntryLoadListener onEntryLoadListener) {
        PKMediaEntry responseEntry = response.getResponse();
        final PKMediaEntry entry = responseEntry;
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

        if (!isValidOVPPlayer())
            return;

        prepareLoadMedia(mediaOptions);

        final KalturaOvpMediaProvider provider = new KalturaOvpMediaProvider(getServerUrl(), getPartnerId(), getKS())
                .setEntryId(mediaOptions.entryId).setUseApiCaptions(mediaOptions.useApiCaptions).setReferrer(referrer);
        provider.load(response -> mediaLoadCompleted(response, listener));
    }

    private boolean isValidOVPPlayer() {
        if (tvPlayerType == TVPlayerType.basic) {
            log.e("loadMedia api for player type KalturaPlayerType.basic is not supported");
            return false;
        } else if (tvPlayerType == TVPlayerType.ott) {
            log.e("loadMedia with OVPMediaOptions for player type KalturaPlayerType.ott is not supported");
            return false;
        }
        return true;
    }

    public void loadMedia(OTTMediaOptions mediaOptions, final OnEntryLoadListener listener) {

        if (!isValidOTTPlayerType())
            return;

        prepareLoadMedia(mediaOptions);

        final PhoenixMediaProvider provider = new PhoenixMediaProvider(getServerUrl(), getPartnerId(), getKS())
                .setAssetId(mediaOptions.assetId).setReferrer(referrer);

        if (mediaOptions.protocol != null) {
            provider.setProtocol(mediaOptions.protocol);
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

        provider.load(response -> mediaLoadCompleted(response, listener));
    }

    private boolean isValidOTTPlayerType() {
        if (tvPlayerType == TVPlayerType.basic) {
            log.e("loadMedia api for player type KalturaPlayerType.basic is not supported");
            return false;
        } else if (tvPlayerType == TVPlayerType.ovp) {
            log.e("loadMedia with OTTMediaOptions for player type KalturaPlayerType.ovp is not supported");
            return false;
        }
        return true;
    }

    private void prepareLoadMedia(MediaOptions mediaOptions) {
        externalSubtitles = null;
        if (mediaOptions.externalSubtitles != null) {
            externalSubtitles = mediaOptions.externalSubtitles;
        }

        ks = null;
        if (!TextUtils.isEmpty(mediaOptions.ks)) {
            setKS(mediaOptions.ks);
        } else if (!TextUtils.isEmpty(initOptions.ks)) {
            setKS(initOptions.ks);
        }

        setStartPosition(mediaOptions.startPosition);
    }

    private void registerPlugins(Context context) {
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
        return TVPlayerType.ott.equals(tvPlayerType);
    }

    private void registerPluginsOTT(Context context) {
        PlayKitManager.registerPlugins(context, PhoenixAnalyticsPlugin.factory);
    }

    private void addKalturaPluginConfigs(PKPluginConfigs combinedPluginConfigs) {
        if (!Integer.valueOf(KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID).equals(ovpPartnerId) && !combinedPluginConfigs.hasConfig(KavaAnalyticsPlugin.factory.getName())) {
            log.d("Adding Automatic Kava Plugin");
            combinedPluginConfigs.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), resolve(getKavaDefaultsConfig(ovpPartnerId, referrer)));
        }

        if (isOTTPlayer() && !combinedPluginConfigs.hasConfig(PhoenixAnalyticsPlugin.factory.getName())) {
            addKalturaPluginConfigsOTT(combinedPluginConfigs);
        }
    }

    private void addKalturaPluginConfigsOTT(PKPluginConfigs combined) {
        PhoenixAnalyticsConfig phoenixConfig = getPhoenixAnalyticsConfig();
        if (phoenixConfig != null) {
            combined.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixConfig);
        }
    }

    private void updateKalturaPluginConfigs(PKPluginConfigs combined) {
        log.d("updateKalturaPluginConfigs");
        for (Map.Entry<String, Object> plugin : combined) {
            updatePluginConfig(plugin.getKey(), plugin.getValue());
        }
    }

    private PhoenixAnalyticsConfig getPhoenixAnalyticsConfig() {
        String name = PhoenixAnalyticsPlugin.factory.getName();
        if (getInitOptions() != null ) {
            PKPluginConfigs pkPluginConfigs = getInitOptions().pluginConfigs;
            if (pkPluginConfigs != null && pkPluginConfigs.hasConfig(name)) {
                return (PhoenixAnalyticsConfig) pkPluginConfigs.getPluginConfig(name);
            }
        }
        return  new PhoenixAnalyticsConfig(getPartnerId(), getServerUrl(), getKS(), Consts.DEFAULT_ANALYTICS_TIMER_INTERVAL_HIGH_SEC);
    }

    public interface OnEntryLoadListener {
        void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error);
    }
}
