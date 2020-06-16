package com.kaltura.tvplayer;

import android.content.Context;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKController;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdController;

import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.player.PKHttpClientManager;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.playback.KalturaPlaybackRequestAdapter;
import com.kaltura.playkit.plugins.playback.KalturaUDRMLicenseRequestAdapter;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.PlaylistProvider;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.config.PhoenixTVPlayerParams;
import com.kaltura.tvplayer.playlist.BasicMediaOptions;
import com.kaltura.tvplayer.playlist.BasicPlaylistOptions;
import com.kaltura.tvplayer.playlist.OTTPlaylistOptions;
import com.kaltura.tvplayer.playlist.OVPPlaylistIdOptions;
import com.kaltura.tvplayer.playlist.OVPPlaylistOptions;
import com.kaltura.tvplayer.playlist.PKBasicPlaylist;
import com.kaltura.tvplayer.playlist.PKPlaylistType;
import com.kaltura.tvplayer.playlist.PlaylistController;

import com.kaltura.tvplayer.playlist.PlaylistEvent;
import com.kaltura.tvplayer.utils.ConfigResolver;
import com.kaltura.tvplayer.utils.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class KalturaPlayer {

    private static final PKLog log = PKLog.get("KalturaPlayer");

    public static final String DEFAULT_OVP_SERVER_URL = "https://cdnapisec.kaltura.com/";
    public static final int COUNT_DOWN_TOTAL = 5000;
    public static final int COUNT_DOWN_INTERVAL = 100;
    public static final String OKHTTP = "okhttp";

    static boolean playerConfigRetrieved;

    private static final String KALTURA_PLAYER_INIT_EXCEPTION = "KalturaPlayer.initialize() was not called or hasn't finished.";
    private static final String KALTURA_PLAYLIST_INIT_EXCEPTION = "KalturaPlayer.initialize() was not called or hasn't finished.";
    public static ErrorElement KalturaPlayerNotInitializedError = new ErrorElement("KalturaPlayerNotInitializedError", KALTURA_PLAYER_INIT_EXCEPTION, 777);
    public static ErrorElement KalturaPlaylistInitializedError = new ErrorElement("KalturaPlayerPlaylistInitializedError", KALTURA_PLAYLIST_INIT_EXCEPTION, 778);

    private enum PrepareState {
        not_prepared,
        preparing,
        prepared
    }

    public enum Type {
        ovp,
        ott,
        basic
    }

    private MessageBus messageBus;
    private boolean pluginsRegistered;
    private Type tvPlayerType;

    private Integer partnerId;
    private Integer ovpPartnerId;
    private String ks;

    protected final String referrer;
    private final Context context;
    private Player pkPlayer;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean autoPlay;
    private boolean preload;
    private Long startPosition;
    private List<PKExternalSubtitle> externalSubtitles;
    private View view;
    private PKMediaEntry mediaEntry;
    private PrepareState prepareState = PrepareState.not_prepared;
    private PlayerTokenResolver tokenResolver = new PlayerTokenResolver();
    private PlayerInitOptions initOptions;
    private PlaylistController playlistController;

    KalturaPlayer(Context context, Type tvPlayerType, PlayerInitOptions initOptions) {

        this.context = context;
        this.tvPlayerType = tvPlayerType;
        this.initOptions = initOptions;
        this.preload = initOptions.preload != null ? initOptions.preload : true;
        this.autoPlay = initOptions.autoplay != null ? initOptions.autoplay : true;
        if (this.autoPlay) {
            this.preload = true; // autoplay implies preload
        }

        messageBus = new MessageBus();
        this.referrer = buildReferrer(context, initOptions.referrer);
        populatePartnersValues();
        this.ks = initOptions.ks;
        if (OKHTTP.equals(PKHttpClientManager.getHttpProvider())) {
            APIOkRequestsExecutor.setClientBuilder(PKHttpClientManager.newClientBuilder()); // share connection-pool with netkit
        }
        registerPlugins(context);
        loadPlayer();
    }

    private boolean kavaPartnerIdIsMissing(PlayerInitOptions initOptions) {
        return (initOptions.tvPlayerParams == null ||
                (initOptions.tvPlayerParams instanceof PhoenixTVPlayerParams && ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId == null));
    }

    protected static String safeServerUrl(Type tvPlayerType, String url, String defaultUrl) {
        String serviceURL = url;
        if (TextUtils.isEmpty(serviceURL)) {
            serviceURL = defaultUrl;
        } else if (Type.ott.equals(tvPlayerType)) {
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

    protected static void initializeDrm(Context context) {
        MediaSupport.initializeDrm(context, (supportedDrmSchemes, isHardwareDrmSupported, provisionPerformed, provisionError) -> {
            String provisionPerformedStatus = "succeeded";
            if (provisionPerformed) {
                if (provisionError != null) {
                    provisionPerformedStatus = "failed";
                }
            }
            log.d("DRM initialized; supportedDrmSchemes: " + supportedDrmSchemes + " isHardwareDrmSupported = " + isHardwareDrmSupported + " provisionPerformedStatus = " + provisionPerformedStatus);
        });

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

        if (partnerId != null && partnerId.equals(KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID)) {
            kavaAnalyticsConfig.setEntryId(KavaAnalyticsConfig.DEFAULT_KAVA_ENTRY_ID);
        }

        if (!TextUtils.isEmpty(ks)) {
            kavaAnalyticsConfig.setKs(ks);
        }

        if (!TextUtils.isEmpty(referrer)) {
            kavaAnalyticsConfig.setReferrer(referrer);
        }

        if (playlistController != null &&
                playlistController.getPlaylist() != null &&
                playlistController.getPlaylistType() == PKPlaylistType.OVP_ID &&
                playlistController.getPlaylist().getId() != null) {
            kavaAnalyticsConfig.setPlaylistId(playlistController.getPlaylist().getId());
        }

        return kavaAnalyticsConfig;
    }

    MessageBus getMessageBus() {
        return messageBus;
    }

    private void loadPlayer() {
        tokenResolver.update(initOptions);
        PKPluginConfigs combinedPluginConfigs = setupPluginsConfiguration();
        pkPlayer = PlayKitManager.loadPlayer(context, combinedPluginConfigs, messageBus);
        updatePlayerSettings();
        if (!combinedPluginConfigs.hasConfig(KavaAnalyticsPlugin.factory.getName()) && Integer.valueOf(KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID).equals(ovpPartnerId)) {
            NetworkUtils.sendKavaImpression(context);
        }
    }

    private void updatePlayerSettings() {
        if (initOptions.referrer != null) {
            KalturaPlaybackRequestAdapter.install(pkPlayer, initOptions.referrer);
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

        if (initOptions.enableDecoderFallback != null) {
            pkPlayer.getSettings().enableDecoderFallback(initOptions.enableDecoderFallback);
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

        if (initOptions.isTunneledAudioPlayback != null) {
            pkPlayer.getSettings().setTunneledAudioPlayback(initOptions.isTunneledAudioPlayback);
        }

        if (initOptions.handleAudioBecomingNoisyEnabled != null) {
            pkPlayer.getSettings().setTunneledAudioPlayback(initOptions.handleAudioBecomingNoisyEnabled);
        }

        if (initOptions.maxVideoSize != null) {
            pkPlayer.getSettings().setMaxVideoSize(initOptions.maxVideoSize);
        }

        if (initOptions.maxVideoBitrate != null) {
            pkPlayer.getSettings().setMaxVideoBitrate(initOptions.maxVideoBitrate);
        }

        if (initOptions.maxAudioBitrate != null) {
            pkPlayer.getSettings().setMaxVideoBitrate(initOptions.maxAudioBitrate);
        }

        if (initOptions.maxAudioChannelCount != null) {
            pkPlayer.getSettings().setMaxAudioChannelCount(initOptions.maxAudioChannelCount);
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

        if (preload) {
            this.mediaEntry = mediaEntry;
            this.prepareState = PrepareState.not_prepared;
            PKPluginConfigs combinedPluginConfigs = setupPluginsConfiguration();
            updateKalturaPluginConfigs(combinedPluginConfigs);
            prepare();
        }
    }


    public void setPlaylist(List<PKMediaEntry> entryList, Long startPosition) {
        externalSubtitles = null;
        if (startPosition != null) {
            setStartPosition(startPosition);
        }
        setMedia(entryList.get(0));
    }

    public void setMedia(PKMediaEntry entry, Long startPosition) {
        externalSubtitles = null;
        if (startPosition != null) {
            setStartPosition(startPosition);
        }
        setMedia(entry);
    }

    protected void registerCommonPlugins(Context context) {
        KnownPlugin.registerAll(context, isOTTPlayer());
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
                .setStartPosition(startPosition);

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
        if (initOptions.pluginConfigs != null) {
            initOptions.pluginConfigs.setPluginConfig(pluginName, pluginConfig);
        }
        pkPlayer.updatePluginConfig(pluginName, pluginConfig);
    }

    private void updateInternalPluginConfig(@NonNull String pluginName, @Nullable Object pluginConfig) {
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
        log.d("destroy KalturaPlayer");
        if (pkPlayer != null) {
            pkPlayer.removeListeners(this);
            pkPlayer.destroy();
        }

        if (playlistController  != null) {
            playlistController.release();
            playlistController = null;
        }
        messageBus = null;
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

    public Type getTvPlayerType() {
        return tvPlayerType;
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

    public long getCurrentProgramTime() {
        return pkPlayer.getCurrentProgramTime();
    }

    public float getPlaybackRate() {
        return pkPlayer.getPlaybackRate();
    }

    public PKMediaFormat getMediaFormat() {
        return pkPlayer.getMediaFormat();
    }

    public float getPositionInWindowMs() {
        return pkPlayer.getPositionInWindowMs();
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

    public Long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Long startPosition) {
        this.startPosition = startPosition;
    }

    public boolean isPreload() {
        return preload;
    }

    public void setPreload(boolean preload) {
        this.preload = preload;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public PlayerInitOptions getInitOptions() {
        return initOptions;
    }

    public int getPartnerId() {
        return (initOptions.partnerId != null) ?  initOptions.partnerId : 0;
    }

    public String getKS() {
        return ks;
    }

    public String getServerUrl() {
        if (initOptions.tvPlayerParams != null && !TextUtils.isEmpty(initOptions.tvPlayerParams.serviceUrl)) {
            return safeServerUrl(tvPlayerType, initOptions.tvPlayerParams.serviceUrl, Type.ovp.equals(tvPlayerType) ? KalturaPlayer.DEFAULT_OVP_SERVER_URL : null);
        }
        return null;
    }

    public PlaylistController getPlaylistController() {
        return playlistController;
    }

    public void setPlaylistController(PlaylistController playlistController) {
        this.playlistController = playlistController;
    }

    // Called by implementation of loadMedia()
    private void mediaLoadCompleted(final ResultElement<PKMediaEntry> response, final OnEntryLoadListener onEntryLoadListener) {

        final PKMediaEntry entry = response.getResponse();
        final ErrorElement loadError = response.getError();
        mainHandler.post(() -> {
            if (entry != null) {
                setMedia(entry);
            }
            if (loadError != null) {
                log.e(loadError.getMessage());
                if (getMessageBus() == null) {
                    return;
                }
                String errMsg = loadError.getMessage();
                if (TextUtils.equals(errMsg,"Asset not found")) {
                    errMsg  = loadError.getName() + " " + loadError.getCode() + " " + loadError.getMessage();
                }
                getMessageBus().post(new KalturaPlayerEvent.LoadMediaError(loadError));
            }
            onEntryLoadListener.onEntryLoadComplete(entry, response.getError());
        });
    }

    private void playlistLoadCompleted(final ResultElement<PKPlaylist> response, final OnPlaylistLoadListener onPlaylistLoadListener) {

        final PKPlaylist playlist = response.getResponse();
        mainHandler.post(() -> {
            if (response.getError() != null) {
                log.e(response.getError().getMessage());

                onPlaylistLoadListener.onPlaylistLoadComplete(null, response.getError());
                if (messageBus != null) {
                    messageBus.post(new PlaylistEvent.PlaylistError(response.getError()));
                }
                return;
            }
            if (messageBus != null) {
                messageBus.post(new PlaylistEvent.PlaylistLoaded(playlist));
                onPlaylistLoadListener.onPlaylistLoadComplete(playlist, null);
            }

        });
    }

    private void populatePartnersValues() {
        if (Type.basic.equals(tvPlayerType) || (Type.ott.equals(tvPlayerType)) && kavaPartnerIdIsMissing(initOptions)) {
            if (Type.basic.equals(tvPlayerType)) {
                this.partnerId = KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID;
            } else {
                this.partnerId = initOptions.partnerId;
            }
            this.ovpPartnerId = KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID;
        } else {
            this.partnerId = initOptions.partnerId;
            if (Type.ott.equals(tvPlayerType)) {
                this.ovpPartnerId = (initOptions.tvPlayerParams != null &&
                        ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId != null &&
                        ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId > 0) ? ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId : null;
            } else {
                this.ovpPartnerId = initOptions.partnerId;
            }
        }
    }

    public void loadPlaylistById(@NonNull OVPPlaylistIdOptions playlistOptions, @NonNull final OnPlaylistControllerListener controllerListener) {

        if (!isValidOVPPlayer())
            return;

        new CountDownTimer(COUNT_DOWN_TOTAL, COUNT_DOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (playerConfigRetrieved || (initOptions != null && initOptions.tvPlayerParams != null)) {
                    cancel();
                    log.d("OVP loadPlaylist by id Done");
                    if (playerConfigRetrieved) {
                        initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(Type.ovp, initOptions.partnerId));
                    }
                    populatePartnersValues();
                    final PlaylistProvider provider = playlistOptions.buildPlaylistProvider(getServerUrl(), getPartnerId(), playlistOptions.ks);
                    provider.load(response -> playlistLoadCompleted(response, (playlist, error) -> {
                        if (error != null) {
                            return;
                        }
                        PlaylistController playlistController = new PKPlaylistController(KalturaPlayer.this, playlist, PKPlaylistType.OVP_ID);
                        playlistController.setPlaylistOptions(playlistOptions);
                        controllerListener.onPlaylistControllerComplete(playlistController, null);
                        setPlaylistController(playlistController);
                        if (messageBus != null) {
                            messageBus.post(new PlaylistEvent.PlaylistStarted(playlist));
                        }
                        playlistController.playItem(playlistOptions.startIndex, autoPlay);
                    }));
                }
            }

            @Override
            public void onFinish() {
                log.e("OVP loadPlaylist by id KalturaPlayerNotInitializedError");
                controllerListener.onPlaylistControllerComplete(null, KalturaPlaylistInitializedError);
            }
        }.start();
    }


    public void loadPlaylist(@NonNull OVPPlaylistOptions playlistOptions, @NonNull final OnPlaylistControllerListener controllerListener) {

        if (!isValidOVPPlayer())
            return;

        if (playlistOptions.ovpMediaOptionsList.isEmpty()) {
            if (messageBus != null) {
                messageBus.post(new PlaylistEvent.PlaylistError(ErrorElement.LoadError.message("ovpMediaOptionsList is empty")));
            }
            return;
        }

        new CountDownTimer(COUNT_DOWN_TOTAL, COUNT_DOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (playerConfigRetrieved || (initOptions != null && initOptions.tvPlayerParams != null)) {
                    cancel();
                    log.d("OVP loadPlaylist Done");
                    if (playerConfigRetrieved) {
                        initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(Type.ovp, initOptions.partnerId));
                    }
                    populatePartnersValues();
                    final PlaylistProvider provider = playlistOptions.buildPlaylistProvider(getServerUrl(), getPartnerId(), playlistOptions.ks);
                    provider.load(response -> playlistLoadCompleted(response, (playlist, error) -> {
                        if (error != null) {
                            return;
                        }
                        PlaylistController playlistController = new PKPlaylistController(KalturaPlayer.this, playlist, PKPlaylistType.OVP_LIST);
                        playlistController.setPlaylistOptions(playlistOptions);
                        controllerListener.onPlaylistControllerComplete(playlistController, null);
                        setPlaylistController(playlistController);
                        if (messageBus != null) {
                            messageBus.post(new PlaylistEvent.PlaylistStarted(playlist));
                        }
                        playlistController.playItem(playlistOptions.startIndex, autoPlay);
                    }));
                }
            }

            @Override
            public void onFinish() {
                log.e("OVP loadPlaylist KalturaPlaylistInitializedError");
                controllerListener.onPlaylistControllerComplete(null, KalturaPlaylistInitializedError);
            }
        }.start();
    }

    public void loadPlaylist(@NonNull OTTPlaylistOptions playlistOptions, @NonNull final OnPlaylistControllerListener controllerListener) {

        if (!isValidOTTPlayer())
            return;

        if (playlistOptions.ottMediaOptionsList.isEmpty()) {
            if (messageBus != null) {
                messageBus.post(new PlaylistEvent.PlaylistError(ErrorElement.LoadError.message("ottMediaOptionsList is empty")));
            }
            return;
        }
        new CountDownTimer(COUNT_DOWN_TOTAL, COUNT_DOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (playerConfigRetrieved || (initOptions != null && initOptions.tvPlayerParams != null)) {
                    cancel();
                    log.d("OTT loadPlaylist Done");
                    if (playerConfigRetrieved) {
                        initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(Type.ott, initOptions.partnerId));
                    }
                    populatePartnersValues();
                    final PlaylistProvider provider = playlistOptions.buildPlaylistProvider(getServerUrl(), getPartnerId(), playlistOptions.ks);
                    provider.load(response -> playlistLoadCompleted(response, (playlist, error) -> {
                        if (error != null) {
                            return;
                        }
                        PlaylistController playlistController = new PKPlaylistController(KalturaPlayer.this, playlist, PKPlaylistType.OTT_LIST);
                        playlistController.setPlaylistOptions(playlistOptions);
                        controllerListener.onPlaylistControllerComplete(playlistController, null);
                        setPlaylistController(playlistController);
                        if (messageBus != null) {
                            messageBus.post(new PlaylistEvent.PlaylistStarted(playlist));
                        }
                        playlistController.playItem(playlistOptions.startIndex, autoPlay);
                    }));
                }
            }

            @Override
            public void onFinish() {
                log.e("OTT loadPlaylist KalturaPlayerNotInitializedError");
                controllerListener.onPlaylistControllerComplete(null, KalturaPlaylistInitializedError);
            }
        }.start();
    }

    public void loadPlaylist(@NonNull BasicPlaylistOptions playlistOptions, @NonNull final OnPlaylistControllerListener controllerListener) {

        if (!isValidBasicPlayer())
            return;

        if (playlistOptions == null || playlistOptions.basicMediaOptionsList == null) {
            return;
        }

        if (playlistOptions.basicMediaOptionsList.isEmpty()) {
            if (messageBus != null) {
                messageBus.post(new PlaylistEvent.PlaylistError(ErrorElement.LoadError.message("playlistMediaEntryList is empty")));
            }
            return;
        }

        List<PKPlaylistMedia> playlistMediaEntryList = new ArrayList<>();
        for (int i = 0; i < playlistOptions.basicMediaOptionsList.size() ; i++) {
            playlistMediaEntryList.add(new BasicMediaOptions(playlistOptions.basicMediaOptionsList.get(i).getPKMediaEntry()));
        }

        PKPlaylist basicPlaylist = new PKBasicPlaylist()
                .setId(playlistOptions.playlistMetadata.getId())
                .setName(playlistOptions.playlistMetadata.getName())
                .setDescription(playlistOptions.playlistMetadata.getDescription())
                .setThumbnailUrl(playlistOptions.playlistMetadata.getThumbnailUrl());
        ((PKBasicPlaylist)basicPlaylist).setBasicMediaOptionsList(playlistMediaEntryList);

        PlaylistController playlistController = new PKPlaylistController(KalturaPlayer.this, basicPlaylist, PKPlaylistType.BASIC_LIST);
        playlistController.setPlaylistOptions(playlistOptions);
        controllerListener.onPlaylistControllerComplete(playlistController, null);
        setPlaylistController(playlistController);
        if (messageBus != null) {
            messageBus.post(new PlaylistEvent.PlaylistStarted(basicPlaylist));
        }
        playlistController.playItem(playlistOptions.startIndex, autoPlay);
    }

    public void loadMedia(@NonNull OTTMediaOptions mediaOptions, @NonNull final OnEntryLoadListener listener) {

        if (!isValidOTTPlayer())
            return;

        prepareLoadMedia(mediaOptions);

        new CountDownTimer(COUNT_DOWN_TOTAL, COUNT_DOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (playerConfigRetrieved || (initOptions != null && initOptions.tvPlayerParams != null)) {
                    cancel();
                    log.d("OTT loadMedia Done");
                    if (playerConfigRetrieved) {
                        initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(Type.ott, initOptions.partnerId));
                    }
                    populatePartnersValues();
                    final MediaEntryProvider provider = mediaOptions.buildMediaProvider(getServerUrl(), getPartnerId());
                    provider.load(response -> mediaLoadCompleted(response, listener));
                }
            }

            @Override
            public void onFinish() {
                log.e("KalturaPlayerNotInitializedError");
                listener.onEntryLoadComplete(null, KalturaPlayerNotInitializedError);
            }
        }.start();
    }

    public void loadMedia(@NonNull OVPMediaOptions mediaOptions, @NonNull final OnEntryLoadListener listener) {

        if (!isValidOVPPlayer())
            return;

        prepareLoadMedia(mediaOptions);

        new CountDownTimer(COUNT_DOWN_TOTAL, COUNT_DOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (playerConfigRetrieved || (initOptions != null && initOptions.tvPlayerParams != null)) {
                    cancel();
                    log.d("OVP loadMedia Done");
                    if (playerConfigRetrieved) {
                        initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(Type.ovp, initOptions.partnerId));
                    }
                    populatePartnersValues();
                    final MediaEntryProvider provider = mediaOptions.buildMediaProvider(getServerUrl(), getPartnerId());
                    provider.load(response -> mediaLoadCompleted(response, listener));
                }
            }

            @Override
            public void onFinish() {
                log.e("KalturaPlayerNotInitializedError");
                listener.onEntryLoadComplete(null, KalturaPlayerNotInitializedError);
            }
        }.start();
    }

    private boolean isValidOVPPlayer() {
        if (Type.basic.equals(tvPlayerType)) {
            return false;
        } else if (Type.ott.equals(tvPlayerType)) {
            return false;
        }
        return true;
    }

    private boolean isValidOTTPlayer() {
        if (Type.basic.equals(tvPlayerType)) {
            return false;
        } else if (Type.ovp.equals(tvPlayerType)) {
            return false;
        }
        return true;
    }

    private boolean isValidBasicPlayer() {
        return Type.basic.equals(tvPlayerType);
    }

    private void prepareLoadMedia(MediaOptions mediaOptions) {
        externalSubtitles = null;
        if (mediaOptions.externalSubtitles != null) {
            externalSubtitles = mediaOptions.externalSubtitles;
        }

        ks = null;
        String mediaKS = null;
        if (isValidOVPPlayer()) {
            if (((OVPMediaOptions) mediaOptions).getOvpMediaAsset() != null) {
                mediaKS = ((OVPMediaOptions) mediaOptions).getOvpMediaAsset().getKs();
            }
        } else if (isValidOTTPlayer()) {
            if (((OTTMediaOptions) mediaOptions).getOttMediaAsset() != null) {
                mediaKS = ((OTTMediaOptions) mediaOptions).getOttMediaAsset().getKs();
            }
        }
        if (!TextUtils.isEmpty(mediaKS)) {
            setKS(mediaKS);
        } else if (!TextUtils.isEmpty(initOptions.ks)) {
            setKS(initOptions.ks);
        }

        setStartPosition(mediaOptions.startPosition);
    }

    private void registerPlugins(Context context) {
        // Plugin registration is static and only done once, but requires a Context.
        if (!pluginsRegistered) {
            registerCommonPlugins(context);
            pluginsRegistered = true;
        }
    }

    private boolean isOTTPlayer() {
        return Type.ott.equals(tvPlayerType);
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
            updateInternalPluginConfig(plugin.getKey(), plugin.getValue());
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

    public interface OnPlaylistLoadListener {
        void onPlaylistLoadComplete(PKPlaylist playlist, ErrorElement error);
    }

    public interface OnPlaylistControllerListener {
        void onPlaylistControllerComplete(PlaylistController playlistController, ErrorElement error);
    }
}
