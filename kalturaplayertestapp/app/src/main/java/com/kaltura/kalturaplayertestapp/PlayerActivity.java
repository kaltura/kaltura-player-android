package com.kaltura.kalturaplayertestapp;

import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.kaltura.kalturaplayertestapp.converters.Media;
import com.kaltura.kalturaplayertestapp.converters.PlayerConfig;
import com.kaltura.kalturaplayertestapp.converters.PluginDescriptor;

import com.kaltura.kalturaplayertestapp.models.ima.UiConfFormatIMAConfig;
import com.kaltura.kalturaplayertestapp.models.ima.UiConfFormatIMADAIConfig;
import com.kaltura.kalturaplayertestapp.tracks.TracksSelectionController;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.plugins.ads.AdCuePoints;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.AdInfo;
import com.kaltura.playkit.plugins.ima.IMAPlugin;
import com.kaltura.playkit.plugins.imadai.IMADAIPlugin;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsEvent;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsEvent;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaStatsEvent;
import com.kaltura.playkit.plugins.ovp.KalturaStatsPlugin;
import com.kaltura.playkit.plugins.youbora.YouboraEvent;
import com.kaltura.playkit.plugins.youbora.YouboraPlugin;
import com.kaltura.playkit.plugins.youbora.pluginconfig.YouboraConfig;
import com.kaltura.playkitvr.VRController;
import com.kaltura.tvplayer.KalturaPlayer;

import com.kaltura.tvplayer.PlaybackControlsView;
import com.kaltura.tvplayer.PlayerConfigManager;
import com.kaltura.tvplayer.PlayerInitOptions;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.OVPMediaOptions;
import com.kaltura.tvplayer.config.player.UiConf;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import static com.kaltura.playkit.PlayerEvent.Type.ENDED;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_AUDIO;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_TEXT;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_VIDEO;


public class PlayerActivity extends AppCompatActivity implements Observer {

    private static final PKLog log = PKLog.get("PlayerActivity");
    private static final int REMOVE_CONTROLS_TIMEOUT = 3000;
    public static final String PLAYER_CONFIG_JSON_KEY = "player_config_json_key";
    public static final String PLAYER_CONFIG_TITLE_KEY = "player_config_title_key";
    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private boolean backButtonPressed;
    private Gson gson = new Gson();
    private KalturaPlayer player;
    private JsonObject playerConfig;
    private PlayerInitOptions initOptions;
    private String playerConfigTitle;
    private String playerInitOptionsJson;

    private Integer uiConfId;
    private String ks;
    private List<Media> mediaList;
    private Integer uiConfPartnerId;
    private EventsAdapter eventsListRecyclerAdapter;
    private RecyclerView eventsListView;
    private List<String> eventsList = new ArrayList<>();
    private List<String> searchedEventsList = new ArrayList<>();
    private String searchLogPattern = "";
    private ProgressBar progressBar;
    private SearchView searchView;
    private TracksSelectionController tracksSelectionController;
    private PlayerConfig appPlayerInitConfig;
    private int currentPlayedMediaIndex = 0;
    private PlaybackControlsView playbackControlsView;
    private AdCuePoints adCuePoints;
    private boolean allAdsCompeted;
    private PlaybackControlsManager playbackControlsManager;
    private boolean isFirstOnResume = true;
    private boolean isPlayingOnPause;

    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        networkChangeReceiver = new NetworkChangeReceiver();
        eventsListView = findViewById(R.id.events_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        eventsListView.setLayoutManager(layoutManager);
        eventsListRecyclerAdapter = new EventsAdapter();
        eventsListView.setAdapter(eventsListRecyclerAdapter);
        searchView = findViewById(R.id.search_events);
        progressBar = findViewById(R.id.videoProgressBar);
        addSearchListener();

        playerConfigTitle = getIntent().getExtras().getString(PlayerActivity.PLAYER_CONFIG_TITLE_KEY);
        playerInitOptionsJson = getIntent().getExtras().getString(PlayerActivity.PLAYER_CONFIG_JSON_KEY);

        if (playerConfigTitle == null || playerInitOptionsJson == null) {
            throw new IllegalArgumentException("Must pass extra " + PlayerActivity.PLAYER_CONFIG_JSON_KEY);
        }
        initDrm();

        appPlayerInitConfig = gson.fromJson(playerInitOptionsJson, PlayerConfig.class);
        if (appPlayerInitConfig != null) {

            final String playerType = appPlayerInitConfig.playerType;
            buildPlayer(appPlayerInitConfig, currentPlayedMediaIndex, playerType);

//        if (appPlayerInitConfig.getUiConf() == null) {
//            log.d("App config json is invalid");
//            buildPlayer( appPlayerInitConfig, currentPlayedMediaIndex, playerType);
//        } else {
//            PlayerConfigManager.retrieve(Integer.valueOf(appPlayerInitConfig.getUiConf().getId()), Integer.valueOf(appPlayerInitConfig.getUiConf().getPartnerId()), appPlayerInitConfig.getKs(), appPlayerInitConfig.getUiConf().getBaseUrl(), new PlayerConfigManager.OnPlayerConfigLoaded() {
//                @Override
//                public void onConfigLoadComplete(int id, JsonObject studioUiConfJson, ErrorElement error, int freshness) {
//                    appPlayerInitConfig.setPlayerConfig(studioUiConfJson);
//                    buildPlayer(appPlayerInitConfig, currentPlayedMediaIndex, playerType);
//                }
//            });
//        }

        } else {
            showMessage(R.string.error_empty_input);
        }
    }

    public void changeMedia() {
        if (player != null) {
            tracksSelectionController = null;
            player.stop();
        }

        updatePluginsConfig(mediaList.get(currentPlayedMediaIndex));
        if ("ovp".equals(appPlayerInitConfig.playerType.toLowerCase())) {
            OVPMediaOptions ovpMediaOptions = buildOvpMediaOptions(0, currentPlayedMediaIndex);
            if (ovpMediaOptions == null) {
                return;
            }
            player.loadMedia(ovpMediaOptions, (entry, error) -> {
                String entryId = "";
                if (entry != null) {
                    entryId = entry.getId();
                }
                log.d("OVPMedia onEntryLoadComplete; " + entryId + "; " + error);
                handleOnEntryLoadComplete(error);
            });
        } else if ("ott".equals(appPlayerInitConfig.playerType.toLowerCase())){
            OTTMediaOptions ottMediaOptions = buildOttMediaOptions(0, currentPlayedMediaIndex);
            if (ottMediaOptions == null) {
                return;
            }
            player.loadMedia(ottMediaOptions, (entry, error) -> {
                String entryId = "";
                if (entry != null) {
                    entryId = entry.getId();
                }
                log.d("OTTMedia onEntryLoadComplete; " + entryId + " ; " + error);
                handleOnEntryLoadComplete(error);
            });
        } else if ("basic".equals(appPlayerInitConfig.playerType.toLowerCase())) {
            PKMediaEntry mediaEntry = appPlayerInitConfig.mediaList.get(currentPlayedMediaIndex).pkMediaEntry;
            if (appPlayerInitConfig.mediaList != null && appPlayerInitConfig.mediaList.get(currentPlayedMediaIndex) != null) {
                if (appPlayerInitConfig.vrSettings != null) {
                    mediaEntry.setIsVRMediaType(true);
                }
                player.setMedia(mediaEntry, 0L);
            }
        }
        else {
            log.e("Error no such player type <" + appPlayerInitConfig.playerType + ">");
        }
    }

    private void updatePluginsConfig(Media media) {
        if (initOptions.pluginConfigs.hasConfig(IMAPlugin.factory.getName())) {
            JsonObject imaJson = (JsonObject) initOptions.pluginConfigs.getPluginConfig(IMAPlugin.factory.getName());
            if (media.mediaAdTag != null) {
                imaJson.addProperty("adTagUrl", media.mediaAdTag);
            }
            //IMAConfig imaPluginConfig = gson.fromJson(imaJson, IMAConfig.class);
            //Example to update the AdTag
            //imaPluginConfig.setAdTagUrl("http://externaltests.dev.kaltura.com/playKitApp/adManager/customAdTags/vmap/inline/ima_pre_mid_post_bumber2.xml");
            initOptions.pluginConfigs.setPluginConfig(IMAPlugin.factory.getName(), imaJson);
        } else if (initOptions.pluginConfigs.hasConfig(IMADAIPlugin.factory.getName())) {
            JsonObject imadaiJson = (JsonObject) initOptions.pluginConfigs.getPluginConfig(IMADAIPlugin.factory.getName());
            //IMADAIConfig imaPluginConfig = gson.fromJson(imadaiJson, IMADAIConfig.class);
            initOptions.pluginConfigs.setPluginConfig(IMAPlugin.factory.getName(), imadaiJson);
        }

//        //EXAMPLE if there are no auto replacers in this format ->  {{key}}
//        if (initOptions.pluginConfigs.hasConfig(YouboraPlugin.factory.getName())) {
//            JsonObject youboraJson = (JsonObject) initOptions.pluginConfigs.getPluginConfig(YouboraPlugin.factory.getName());
//            YouboraConfig youboraPluginConfig = gson.fromJson(youboraJson, YouboraConfig.class);
//            Properties properties = new Properties();
//            properties.setGenre("AAAA");
//            properties.setOwner("SONY");
//            properties.setQuality("HD");
//            properties.setPrice("122");
//            properties.setYear("2018");
//            youboraPluginConfig.setProperties(properties);
//            initOptions.pluginConfigs.setPluginConfig(YouboraPlugin.factory.getName(), youboraPluginConfig.toJson());
//        }
    }

    private void handleOnEntryLoadComplete(ErrorElement error) {
        if (error != null) {
            log.d("Load Error Extra = " + error.getExtra());
            Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
            playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
            playbackControlsManager.showControls(View.VISIBLE);
        } else {
            if (!initOptions.autoplay) {
                playbackControlsManager.showControls(View.VISIBLE);
            }
        }
    }

    public void clearLogView() {
        eventsList.clear();
        searchedEventsList.clear();
        searchLogPattern = "";
    }

    public int getCurrentPlayedMediaIndex() {
        return currentPlayedMediaIndex;
    }

    public void setCurrentPlayedMediaIndex(int currentPlayedMediaIndex) {
        this.currentPlayedMediaIndex = currentPlayedMediaIndex;
    }

    private void addSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                String queryToLowerCase = query.toLowerCase();
                searchLogPattern = queryToLowerCase;
                searchedEventsList.clear();
                for (String eventItem : eventsList) {
                    if (eventItem.toLowerCase().contains(queryToLowerCase)) {
                        searchedEventsList.add(eventItem);
                    }
                }
                eventsListRecyclerAdapter.notifyData(searchedEventsList);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    searchedEventsList.clear();
                    searchLogPattern = "";
                    eventsListRecyclerAdapter.notifyData(eventsList);
                    return false;
                }
                return false;
            }
        });
    }

    private void buildPlayer(PlayerConfig appPlayerInitConfig, int playListMediaIndex, String playerType) {
        KalturaPlayer player = null;

        JsonArray appPluginConfigJsonObject = appPlayerInitConfig.plugins;
//        int playerUiConfId = -1;
//        if (appPlayerInitConfig.uiConf != null) {
//            playerUiConfId = Integer.valueOf(appPlayerInitConfig.uiConf.id);
//        }
        mediaList = appPlayerInitConfig.mediaList;

        Integer partnerId = (appPlayerInitConfig.partnerId != null) ? Integer.valueOf(appPlayerInitConfig.partnerId) : null;
        initOptions = new PlayerInitOptions(partnerId, new UiConf((appPlayerInitConfig.uiConf == null || appPlayerInitConfig.uiConf.id == null) ? null : Integer.valueOf(appPlayerInitConfig.uiConf.id), (appPlayerInitConfig.uiConf == null || appPlayerInitConfig.uiConf.partnerId == null) ? null : Integer.valueOf(appPlayerInitConfig.uiConf.partnerId)))
                .setAutoPlay(appPlayerInitConfig.autoPlay)
                .setKs(appPlayerInitConfig.ks)
                .setPreload(appPlayerInitConfig.preload)
                .setReferrer(appPlayerInitConfig.referrer)
                .setServerUrl(appPlayerInitConfig.baseUrl)
                .setAllowCrossProtocolEnabled(appPlayerInitConfig.allowCrossProtocolEnabled)
                .setPreferredMediaFormat(appPlayerInitConfig.preferredFormat)
                .setSecureSurface(appPlayerInitConfig.secureSurface)
                .setAspectRatioResizeMode(appPlayerInitConfig.aspectRatioResizeMode)
                .setAbrSettings(appPlayerInitConfig.abrSettings)
                .setLoadControlBuffers(appPlayerInitConfig.loadControlBuffers)
                .setSubtitleStyle(appPlayerInitConfig.setSubtitleStyle)
                .setAllowClearLead(appPlayerInitConfig.allowClearLead)
                .setAdAutoPlayOnResume(appPlayerInitConfig.adAutoPlayOnResume)
                .setVrPlayerEnabled(appPlayerInitConfig.vrPlayerEnabled)
                .setVRSettings(appPlayerInitConfig.vrSettings)
                .setIsVideoViewHidden(appPlayerInitConfig.isVideoViewHidden)
                .setContentRequestAdapter(appPlayerInitConfig.contentRequestAdapter)
                .setLicenseRequestAdapter(appPlayerInitConfig.licenseRequestAdapter)
                .setPluginConfigs(convertPluginsJsonArrayToPKPlugins(appPluginConfigJsonObject));

        if (appPlayerInitConfig.trackSelection != null) {
            if (appPlayerInitConfig.trackSelection.audioSelectionMode != null) {
                initOptions.setAudioLanguage(appPlayerInitConfig.trackSelection.audioSelectionLanguage, PKTrackConfig.Mode.valueOf(appPlayerInitConfig.trackSelection.audioSelectionMode));
            }
            if (appPlayerInitConfig.trackSelection.textSelectionMode != null) {
                initOptions.setTextLanguage(appPlayerInitConfig.trackSelection.textSelectionLanguage, PKTrackConfig.Mode.valueOf(appPlayerInitConfig.trackSelection.textSelectionMode));
            }
        }

        if ("ovp".equals(playerType.toLowerCase())) {
            player = KalturaPlayer.createOVPPlayer(PlayerActivity.this, initOptions);
            setPlayer(player);

            OVPMediaOptions ovpMediaOptions = buildOvpMediaOptions(appPlayerInitConfig.startPosition, playListMediaIndex);
            player.loadMedia(ovpMediaOptions, (entry, error) -> {
                if (error != null) {
                    log.d("OVPMedia Error Extra = " + error.getExtra());
                    Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
                    playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
                    if (playbackControlsView != null) {
                        playbackControlsManager.showControls(View.VISIBLE);
                    }
                } else {
                    log.d("OVPMedia onEntryLoadComplete entry =" + entry.getId());
                }
            });
        } else if ("ott".equals(playerType.toLowerCase())) {
            player = KalturaPlayer.createOTTPlayer(PlayerActivity.this, initOptions);
            setPlayer(player);
            OTTMediaOptions ottMediaOptions = buildOttMediaOptions(appPlayerInitConfig.startPosition, playListMediaIndex);
            player.loadMedia(ottMediaOptions, (entry, error) -> {
                if (error != null) {
                    log.d("OTTMedia Error Extra = " + error.getExtra());
                    Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
                    playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
                    if (playbackControlsView != null) {
                        playbackControlsManager.showControls(View.VISIBLE);
                    }
                } else {
                    log.d("OTTMedia onEntryLoadComplete  entry = " + entry.getId());
                }
            });
        } else if ("basic".equals(playerType.toLowerCase())) {
            player = KalturaPlayer.createBasicPlayer(PlayerActivity.this, initOptions);
            setPlayer(player);
            PKMediaEntry mediaEntry = appPlayerInitConfig.mediaList.get(currentPlayedMediaIndex).pkMediaEntry;
            if (appPlayerInitConfig.mediaList != null && appPlayerInitConfig.mediaList.get(currentPlayedMediaIndex) != null) {
                if (appPlayerInitConfig.vrSettings != null) {
                    mediaEntry.setIsVRMediaType(true);
                }
                player.setMedia(mediaEntry, (long) appPlayerInitConfig.startPosition);
            }
        }
        else {
            log.e("Failed to initialize player...");
            return;
        }

        playbackControlsManager = new PlaybackControlsManager(this, player, playbackControlsView);
        if (!initOptions.autoplay) {
            playbackControlsManager.showControls(View.VISIBLE);
        } else {
            playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.pause);
        }

        if (appPlayerInitConfig.mediaList != null) {
            if (appPlayerInitConfig.mediaList.size() > 1) {
                playbackControlsManager.addChangeMediaButtonsListener(appPlayerInitConfig.mediaList.size());
            }
            playbackControlsManager.updatePrevNextBtnFunctionality(currentPlayedMediaIndex, appPlayerInitConfig.mediaList.size());
        }
    }

    private OTTMediaOptions buildOttMediaOptions(int startPosition, int playListMediaIndex) {
        Media ottMedia = mediaList.get(playListMediaIndex);
        if (ottMedia == null) {
            return null;
        }
        OTTMediaOptions ottMediaOptions = new OTTMediaOptions();
        ottMediaOptions.assetId = ottMedia.assetId;
        ottMediaOptions.assetType = ottMedia.getAssetType();
        ottMediaOptions.contextType = ottMedia.getPlaybackContextType();
        ottMediaOptions.assetReferenceType = ottMedia.getAssetReferenceType();
        ottMediaOptions.protocol = ottMedia.getProtocol();
        ottMediaOptions.ks = ottMedia.ks;
        ottMediaOptions.startPosition = startPosition;


        if (!TextUtils.isEmpty(ottMedia.format)) {
            ottMediaOptions.formats = new String[]{ottMedia.format};
        }
        if (ottMedia.fileId != null) {
            ottMediaOptions.fileIds = new String[]{String.valueOf(ottMedia.fileId)};
        }

        return ottMediaOptions;
    }

    @NonNull
    private OVPMediaOptions buildOvpMediaOptions(int startPosition, int playListMediaIndex) {
        Media ovpMedia = mediaList.get(playListMediaIndex);
        OVPMediaOptions ovpMediaOptions = new OVPMediaOptions();
        ovpMediaOptions.entryId = ovpMedia.entryId;
        ovpMediaOptions.ks = ovpMedia.ks;
        ovpMediaOptions.startPosition = startPosition;
        return ovpMediaOptions;
    }

    private void setPlayerListeners() {
        //////// AD Events

        player.addListener(this, AdEvent.error, event -> {
            log.d("AD ERROR");
            updateEventsLogsList("ad:\n" + event.eventType().name());
            AdEvent.Error adError = (AdEvent.Error) event;
            playbackControlsManager.setAdPlayerState(adError.type);
        });

        player.addListener(this, AdEvent.cuepointsChanged, event -> {
            log.d("AD CUEPOINTS CHANGED");
            updateEventsLogsList("ad:\n" + event.eventType().name());
            adCuePoints = event.cuePoints;
        });

        player.addListener(this, AdEvent.completed, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD COMPLETED");
        });

        player.addListener(this, AdEvent.allAdsCompleted, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD ALL_ADS_COMPLETED");
            playbackControlsManager.setAdPlayerState(AdEvent.Type.ALL_ADS_COMPLETED);
            allAdsCompeted = true;
            if (isPlaybackEndedState()) {
                progressBar.setVisibility(View.GONE);
                playbackControlsManager.showControls(View.VISIBLE);
                playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.replay);
            }
        });

        player.addListener(this, AdEvent.contentPauseRequested, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD CONTENT_PAUSE_REQUESTED");
            playbackControlsManager.setAdPlayerState(AdEvent.Type.CONTENT_PAUSE_REQUESTED);

            if (!initOptions.autoplay && adCuePoints != null && !IMADAIPlugin.factory.getName().equals(adCuePoints.getAdPluginName())) {
                playbackControlsManager.showControls(View.INVISIBLE);
            }
            progressBar.setVisibility(View.VISIBLE);
        });

        player.addListener(this, AdEvent.contentResumeRequested, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD CONTENT_RESUME_REQUESTED");
            playbackControlsManager.setAdPlayerState(AdEvent.Type.CONTENT_RESUME_REQUESTED);
            playbackControlsManager.showControls(View.INVISIBLE);
        });

        player.addListener(this, AdEvent.loaded, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD LOADED");
            playbackControlsManager.setAdPlayerState(AdEvent.Type.LOADED);
        });

        player.addListener(this, AdEvent.started, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD STARTED");
            playbackControlsManager.setAdPlayerState(AdEvent.Type.STARTED);
            allAdsCompeted = false;
            AdInfo adInfo = ((AdEvent.AdStartedEvent) event).adInfo;
            if (!initOptions.autoplay && adCuePoints != null && !IMADAIPlugin.factory.getName().equals(adCuePoints.getAdPluginName())) {
                playbackControlsManager.showControls(View.INVISIBLE);
            }

            progressBar.setVisibility(View.INVISIBLE);
        });

        player.addListener(this, AdEvent.paused, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD PAUSED");
            playbackControlsManager.setAdPlayerState(AdEvent.Type.PAUSED);
            playbackControlsManager.showControls(View.VISIBLE);
            playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
        });

        player.addListener(this, AdEvent.resumed, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD RESUMED");
            playbackControlsManager.setAdPlayerState(AdEvent.Type.RESUMED);
            playbackControlsManager.showControls(View.INVISIBLE);
        });

        player.addListener(this, AdEvent.tapped, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD TAPPED");
            playbackControlsManager.handleContainerClick();
        });

        player.addListener(this, AdEvent.skipped, event -> {
            updateEventsLogsList("ad:\n" + event.eventType().name());
            log.d("AD SKIPPED");
            playbackControlsManager.setAdPlayerState(AdEvent.Type.SKIPPED);
        });

        player.addListener(this, AdEvent.adBufferStart, event -> {
            log.d("AD_BUFFER_START pos = " + event.adPosition);
            progressBar.setVisibility(View.VISIBLE);
        });

        player.addListener(this, AdEvent.adBufferEnd, event -> {
            log.d("AD_BUFFER_END pos = " + event.adPosition);
            progressBar.setVisibility(View.INVISIBLE);
        });

        /////// PLAYER EVENTS

//        player.addListener(this, PlayerEvent.play, event -> {
//            log.d("Player PLAY");
//
//        });


        player.addListener(this, PlayerEvent.loadedMetadata, event -> {
            log.d("Player Event LoadedMetadata");
            updateEventsLogsList("player:\n" + event.eventType().name());
        });

        player.addListener(this, PlayerEvent.durationChanged, event -> {
            log.d("Player Event DurationChanged");
            updateEventsLogsList("player:\n" + event.eventType().name());
        });

        player.addListener(this, PlayerEvent.playing, event -> {
            log.d("Player Event PLAYING");
            updateEventsLogsList("player:\n" + event.eventType().name());
            progressBar.setVisibility(View.INVISIBLE);
            playbackControlsManager.setContentPlayerState(event.eventType());
            playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.pause);
            playbackControlsManager.showControls(View.INVISIBLE);
        });

        player.addListener(this, PlayerEvent.pause, event -> {
            log.d("Player Event PAUSE");
            updateEventsLogsList("player:\n" + event.eventType().name());
        });

        player.addListener(this, PlayerEvent.stopped, event -> {
            log.d("PLAYER PLAYING");
            updateEventsLogsList("player:\n" + event.eventType().name());
            playbackControlsManager.showControls(View.INVISIBLE);
        });

        player.addListener(this, PlayerEvent.ended, event -> {
            log.d("PLAYER ENDED");
            if (adCuePoints == null || (adCuePoints != null && !adCuePoints.hasPostRoll()) || IMADAIPlugin.factory.getName().equals(adCuePoints.getAdPluginName())) {
                playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.replay);
            }
            progressBar.setVisibility(View.GONE);
            if (!isPostrollAvailableInAdCuePoint()) {
                playbackControlsManager.showControls(View.VISIBLE);
            }
        });

        player.addListener(this, PlayerEvent.textTrackChanged, event -> {
            log.d("PLAYER textTrackChanged");
            if (tracksSelectionController != null && tracksSelectionController.getTracks() != null) {
                for (int i = 0; i <= tracksSelectionController.getTracks().getTextTracks().size() - 1; i++) {
                    log.d(tracksSelectionController.getTracks().getTextTracks().size() + ", PLAYER textTrackChanged " + tracksSelectionController.getTracks().getTextTracks().get(i).getUniqueId() + "/" + event.newTrack.getUniqueId());
                    if (event.newTrack.getUniqueId().equals(tracksSelectionController.getTracks().getTextTracks().get(i).getUniqueId())) {
                        if (tracksSelectionController != null) {
                            tracksSelectionController.setTrackLastSelectionIndex(TRACK_TYPE_TEXT, i);
                        }
                        break;
                    }
                }
            }
        });

        player.addListener(this, PlayerEvent.audioTrackChanged, event -> {
            log.d("PLAYER audioTrackChanged");
            if (tracksSelectionController != null && tracksSelectionController.getTracks() != null) {
                for (int i = 0; i <= tracksSelectionController.getTracks().getAudioTracks().size() - 1; i++) {
                    if (event.newTrack.getUniqueId().equals(tracksSelectionController.getTracks().getAudioTracks().get(i).getUniqueId())) {
                        tracksSelectionController.setTrackLastSelectionIndex(TRACK_TYPE_AUDIO, i);
                        break;
                    }
                }
            }
        });

        player.addListener(this, PlayerEvent.videoTrackChanged, event -> {
            log.d("PLAYER videoTrackChanged");
            if (tracksSelectionController != null && tracksSelectionController.getTracks() != null) {
                for (int i = 0; i <= tracksSelectionController.getTracks().getVideoTracks().size() - 1; i++) {
                    if (event.newTrack.getUniqueId().equals(tracksSelectionController.getTracks().getVideoTracks().get(i).getUniqueId())) {
                        tracksSelectionController.setTrackLastSelectionIndex(TRACK_TYPE_VIDEO, i);
                        break;
                    }
                }
            }
        });

        player.addListener(this, PlayerEvent.tracksAvailable, event -> {
            log.d("PLAYER tracksAvailable");
            updateEventsLogsList("player:\n" + event.eventType().name());
            //Obtain the actual tracks info from it.
            PKTracks tracks = event.tracksInfo;
            tracksSelectionController = new TracksSelectionController(PlayerActivity.this, player, tracks);
            playbackControlsManager.setTracksSelectionController(tracksSelectionController);
            int defaultAudioTrackIndex = tracks.getDefaultAudioTrackIndex();
            int defaultTextTrackIndex = tracks.getDefaultTextTrackIndex();
            if (tracks.getAudioTracks().size() > 0) {
                log.d("Default Audio lang = " + tracks.getAudioTracks().get(defaultAudioTrackIndex).getLabel());
            }
            if (tracks.getTextTracks().size() > 0) {
                log.d("Default Text lang = " + tracks.getTextTracks().get(defaultTextTrackIndex).getLabel());
            }
            if (tracks.getVideoTracks().size() > 0) {
                log.d("Default video isAdaptive = " + tracks.getVideoTracks().get(tracks.getDefaultAudioTrackIndex()).isAdaptive() + " bitrate = " + tracks.getVideoTracks().get(tracks.getDefaultAudioTrackIndex()).getBitrate());
            }
        });

        player.addListener(this, PlayerEvent.error, event -> {
            log.d("PLAYER ERROR");
        });

        player.addListener(this, PlayerEvent.sourceSelected, event -> {
            log.d("PLAYER SOURCE SELECTED");
            updateEventsLogsList("player:\n" + event.eventType().name());
            log.d("Selected Source = " + event.source.getUrl());
        });

        player.addListener(this, PlayerEvent.canPlay, event -> {
            log.d("PLAYER CAN PLAY");
            VRController vrController = player.getController(VRController.class);
            if (vrController != null) {
                vrController.setOnClickListener(v -> {
                    //application code for handaling ui operations
                    playbackControlsManager.showControls(View.VISIBLE);
                });
            } else {
                if (adCuePoints != null && IMADAIPlugin.factory.getName().equals(adCuePoints.getAdPluginName())) {
                    if (!initOptions.autoplay) {
                        playbackControlsManager.showControls(View.VISIBLE);
                    } else {
                        playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.pause);
                    }
                }
            }

            updateEventsLogsList("player:\n" + event.eventType().name());
        });

        player.addListener(this, PlayerEvent.stateChanged, event -> {
            log.d("PLAYER stateChangeEvent " + event.eventType().name() + " = " + event.newState);
            updateEventsLogsList("player:\n" + event.eventType().name() + ":" + event.newState);

            switch (event.newState) {
                case IDLE:
                    log.d("StateChange Idle");
                    break;
                case LOADING:
                    log.d("StateChange Loading");
                    break;
                case READY:
                    log.d("StateChange Ready");
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case BUFFERING:
                    log.d("StateChange Buffering");
                    AdController adController = player.getController(AdController.class);
                    if (adController == null || (adController != null && !adController.isAdDisplayed())) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        });

        player.addListener(this, PlayerEvent.seeking, event -> {
            log.d("PLAYER SEEKING " + event);

            PlayerActivity.this.updateEventsLogsList("player:\n" + event.eventType().name());
        });

        player.addListener(this, PlayerEvent.seeked, event -> {
            log.d("PLAYER SEEKED");
            updateEventsLogsList("player:\n" + event.eventType().name());
        });


        player.addListener(this, KalturaStatsEvent.reportSent, event -> {
            String reportedEventName = event.reportedEventName;
            if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                updateEventsLogsList("stats:\n" + reportedEventName);
            }
        });

        player.addListener(this, KavaAnalyticsEvent.reportSent, event -> {
            String reportedEventName = event.reportedEventName;
            if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                updateEventsLogsList("kava:\n" + reportedEventName);
            }
        });

        player.addListener(this, YouboraEvent.reportSent, event -> {
            String reportedEventName = event.reportedEventName;
            if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                updateEventsLogsList("youbora:\n" + reportedEventName);
            }

        });

        player.addListener(this, PhoenixAnalyticsEvent.reportSent, event -> {
            String reportedEventName = event.reportedEventName;
            if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                updateEventsLogsList("phoenix:\n" + reportedEventName);
            }

        });
    }

    private void updateEventsLogsList(String eventMsg) {
        Date date = new Date();
        eventMsg = dateFormat.format(date) + " " + eventMsg;
        eventsList.add(eventMsg);
        if (!TextUtils.isEmpty(searchLogPattern)) {
            if (eventMsg.toLowerCase().contains(searchLogPattern)) {
                searchedEventsList.add(eventMsg);
                eventsListRecyclerAdapter.notifyData(searchedEventsList);
            }
        } else {
            eventsListRecyclerAdapter.notifyData(eventsList);
        }
    }

    private PKPluginConfigs convertPluginsJsonArrayToPKPlugins(JsonArray pluginConfigs) {
        PKPluginConfigs pkPluginConfigs = new PKPluginConfigs();
        PluginDescriptor[] pluginDescriptors = gson.fromJson(pluginConfigs, PluginDescriptor[].class);

        if (pluginDescriptors != null) {
            for (PluginDescriptor pluginDescriptor : pluginDescriptors) {
                String pluginName = pluginDescriptor.getPluginName();
                if (YouboraPlugin.factory.getName().equalsIgnoreCase(pluginName)) {
                    YouboraConfig youboraPlugin = gson.fromJson(pluginDescriptor.getParams().get("options"), YouboraConfig.class);
                    pkPluginConfigs.setPluginConfig(YouboraPlugin.factory.getName(), youboraPlugin.toJson());
                } else if (KavaAnalyticsPlugin.factory.getName().equalsIgnoreCase(pluginName)) {
                    KavaAnalyticsConfig kavaPluginConfig = gson.fromJson(pluginDescriptor.getParams(), KavaAnalyticsConfig.class);
                    pkPluginConfigs.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), kavaPluginConfig.toJson());
                } else if (IMAPlugin.factory.getName().equalsIgnoreCase(pluginName)) {
                    UiConfFormatIMAConfig imaPluginConfig = gson.fromJson(pluginDescriptor.getParams(), UiConfFormatIMAConfig.class);
                    pkPluginConfigs.setPluginConfig(IMAPlugin.factory.getName(), imaPluginConfig.toJson());
                }  else if (IMADAIPlugin.factory.getName().equalsIgnoreCase(pluginName)) {
                    UiConfFormatIMADAIConfig imaPluginConfig = gson.fromJson(pluginDescriptor.getParams(), UiConfFormatIMADAIConfig.class);
                    pkPluginConfigs.setPluginConfig(IMADAIPlugin.factory.getName(), imaPluginConfig.toJson());
                } else if (PhoenixAnalyticsPlugin.factory.getName().equalsIgnoreCase(pluginName)) {
                    PhoenixAnalyticsConfig phoenixAnalyticsConfig = gson.fromJson(pluginDescriptor.getParams(), PhoenixAnalyticsConfig.class);
                    pkPluginConfigs.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixAnalyticsConfig.toJson());
                } else if (KalturaStatsPlugin.factory.getName().equalsIgnoreCase(pluginName)) {
                    KalturaStatsConfig kalturaStatsPluginConfig = gson.fromJson(pluginDescriptor.getParams(), KalturaStatsConfig.class);
                    pkPluginConfigs.setPluginConfig(KalturaStatsPlugin.factory.getName(), kalturaStatsPluginConfig.toJson());
                } else if (KalturaLiveStatsPlugin.factory.getName().equalsIgnoreCase(pluginName)) {
                    KalturaLiveStatsConfig kalturaLiveStatsPluginConfig = gson.fromJson(pluginDescriptor.getParams(), KalturaLiveStatsConfig.class);
                    pkPluginConfigs.setPluginConfig(KalturaLiveStatsPlugin.factory.getName(), kalturaLiveStatsPluginConfig.toJson());
                }
            }
        }
        return pkPluginConfigs;
    }

    @Override
    public boolean onSupportNavigateUp() {
        backButtonPressed = true;
        onBackPressed();
        return true;
    }

    private void loadPlayerConfig() {
        PlayerConfigManager.initialize(this);
        if (initOptions == null || uiConfId == null) {
            log.e("initOptions or uiConfId are null");
            return;
        }

        if (uiConfPartnerId == null) {
            uiConfPartnerId = initOptions.partnerId;
        }
        PlayerConfigManager.retrieve(uiConfId, initOptions.partnerId, initOptions.ks, initOptions.serverUrl, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(PlayerActivity.this, "Loaded config, freshness =" + freshness, Toast.LENGTH_LONG).show();
                playerConfig = config;
            }
        });
    }

    void initDrm() {
        MediaSupport.initializeDrm(this, new MediaSupport.DrmInitCallback() {
            @Override
            public void onDrmInitComplete(Set<PKDrmParams.Scheme> supportedDrmSchemes, boolean provisionPerformed, Exception provisionError) {
                if (provisionPerformed) {
                    if (provisionError != null) {
                        log.e("DRM Provisioning failed", provisionError);
                    } else {
                        log.d("DRM Provisioning succeeded");
                    }
                }
                log.d("DRM initialized; supported: " + supportedDrmSchemes);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.removeListeners(this);
            player.stop();
            player.destroy();
            player = null;
            eventsList.clear();
        }
        networkChangeReceiver = null;
    }

    public void setPlayer(final KalturaPlayer player) {
        if (playbackControlsManager != null) {
            playbackControlsManager.setAdPlayerState(null);
            playbackControlsManager.setContentPlayerState(null);
        }
        this.player = player;
        if (player == null) {
            log.e( "Player is null");
            return;
        }
        setPlayerListeners();
        final ViewGroup container = findViewById(R.id.player_container_layout);
        playbackControlsView = findViewById(R.id.player_controls);
        playbackControlsView.setVisibility(View.INVISIBLE);
        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    player.setPlayerView(ViewGroup.LayoutParams.MATCH_PARENT, 600);
                } else {
                    player.setPlayerView(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                }
                container.setOnClickListener(view -> {
                    if (playbackControlsManager != null) {
                        playbackControlsManager.handleContainerClick();
                    }
                });
                container.addView(player.getPlayerView());
                playbackControlsView.setPlayer(player);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checking the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if(getSupportActionBar()!=null) {
                getSupportActionBar().hide();
            }
            searchView.setVisibility(View.GONE);
            eventsListView.setVisibility(View.GONE);
            //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            player.setPlayerView(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            //unhide your objects here.
            if(getSupportActionBar()!=null) {
                getSupportActionBar().show();
            }
            searchView.setVisibility(View.VISIBLE);
            eventsListView.setVisibility(View.VISIBLE);
            player.setPlayerView(ViewGroup.LayoutParams.MATCH_PARENT, 600);
        }
    }

    private boolean isPlaybackEndedState() {
        return playbackControlsManager.getPlayerState() == ENDED || (allAdsCompeted && isPostrollAvailableInAdCuePoint());
    }

    private boolean isPostrollAvailableInAdCuePoint() {
        if (adCuePoints == null) {
            return false;
        }
        return adCuePoints.hasPostRoll();
    }

    @Override
    protected void onResume() {
        log.d("Player Activity onResume");
        super.onResume();
        NetworkChangeReceiver.getObservable().addObserver(this);
        this.registerReceiver(networkChangeReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        if (isFirstOnResume) {
            isFirstOnResume = false;
            return;
        }
        if (player != null) {
            log.d("onResume -> Player Activity onResume");
            player.onApplicationResumed();
            setPlayerListeners();
            if (isPlayingOnPause) {
                isPlayingOnPause = false;
                if (playbackControlsView != null && playbackControlsView.getPlayPauseToggle() != null) {
                    playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
                }
            }
        }
    }

    @Override
    protected void onPause() {

        if (player != null) {
            if (player.isPlaying()) {
                isPlayingOnPause = true;
            }
            player.onApplicationPaused();
        }
        super.onPause();
        unregisterReceiver(networkChangeReceiver);
        NetworkChangeReceiver.getObservable().deleteObserver(this);
        if (!backButtonPressed && playbackControlsManager != null) {
            playbackControlsManager.showControls(View.VISIBLE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            backButtonPressed = true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void update(Observable observable, Object objectStatus) {
        Boolean isConnected = (Boolean) objectStatus;
        if (isConnected) {
            onNetworkConnected();
        } else {
            onNetworkDisConnected();
        }
    }

    protected void onNetworkConnected() {
        showMessage(R.string.network_connected);
        if (player != null) {
            player.onApplicationResumed();
            player.play();
        }
    }

    protected void onNetworkDisConnected() {
        showMessage(R.string.network_disconnected);
        if (player != null) {
            player.onApplicationPaused();
        }
    }

    private void showMessage(int string) {
        RelativeLayout itemView = findViewById(R.id.player_container);
        Snackbar snackbar = Snackbar.make(itemView, string, Snackbar.LENGTH_LONG);
        snackbar.show();
    }
}
