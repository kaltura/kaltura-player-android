package com.kaltura.kalturaplayertestapp;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
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
import com.kaltura.kalturaplayertestapp.tracks.TracksSelectionController;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.ads.AdCuePoints;
import com.kaltura.playkit.ads.AdEvent;
import com.kaltura.playkit.ads.AdInfo;
import com.kaltura.playkit.ads.PKAdErrorType;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.plugins.ima.IMAConfig;
import com.kaltura.playkit.plugins.ima.IMAPlugin;
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
import com.kaltura.playkit.plugins.youbora.pluginconfig.Properties;
import com.kaltura.playkit.plugins.youbora.pluginconfig.YouboraConfig;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlaybackControlsView;
import com.kaltura.tvplayer.PlayerConfigManager;
import com.kaltura.tvplayer.PlayerInitOptions;
import com.kaltura.tvplayer.ott.KalturaOTTPlayer;
import com.kaltura.tvplayer.ott.OTTMediaOptions;
import com.kaltura.tvplayer.ovp.KalturaOvpPlayer;
import com.kaltura.tvplayer.ovp.OVPMediaOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.kaltura.playkit.PlayerEvent.Type.CAN_PLAY;
import static com.kaltura.playkit.PlayerEvent.Type.ENDED;
import static com.kaltura.playkit.PlayerEvent.Type.PLAYING;
import static com.kaltura.playkit.PlayerEvent.Type.SOURCE_SELECTED;
import static com.kaltura.playkit.PlayerEvent.Type.STOPPED;
import static com.kaltura.playkit.PlayerEvent.Type.TRACKS_AVAILABLE;

public class PlayerActivity extends AppCompatActivity {

    private static final PKLog log = PKLog.get("PlayerActivity");
    private static final int REMOVE_CONTROLS_TIMEOUT = 3000;
    public static final String PLAYER_CONFIG_JSON_KEY = "player_config_json_key";
    public static final String PLAYER_CONFIG_TITLE_KEY = "player_config_title_key";
    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
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
    private SearchView searchView;
    private TracksSelectionController tracksSelectionController;
    private PlayerConfig appPlayerInitConfig;
    private int currentPlayedMediaIndex = 0;
    private PlaybackControlsView playbackControlsView;
    private AdCuePoints adCuePoints;
    private boolean allAdsCompeted;
    private PlaybackControlsManager playbackControlsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        eventsListView = findViewById(R.id.events_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        eventsListView.setLayoutManager(layoutManager);
        eventsListRecyclerAdapter = new EventsAdapter();
        eventsListView.setAdapter(eventsListRecyclerAdapter);
        searchView = findViewById(R.id.search_events);
        addSearchListener();

        playerConfigTitle = getIntent().getExtras().getString(PlayerActivity.PLAYER_CONFIG_TITLE_KEY);
        playerInitOptionsJson = getIntent().getExtras().getString(PlayerActivity.PLAYER_CONFIG_JSON_KEY);

        if (playerConfigTitle == null|| playerInitOptionsJson == null) {
            throw new IllegalArgumentException("Must pass extra " + PlayerActivity.PLAYER_CONFIG_JSON_KEY);
        }
        initDrm();

        appPlayerInitConfig = gson.fromJson(playerInitOptionsJson, PlayerConfig.class);
        final String playerType = appPlayerInitConfig.getPlayerType();


        if (appPlayerInitConfig.getUiConf() == null) {
            log.d("App config json is invalid");
            buildPlayer(null, appPlayerInitConfig, currentPlayedMediaIndex, playerType);
        } else {
            PlayerConfigManager.retrieve(Integer.valueOf(appPlayerInitConfig.getUiConf().getId()), Integer.valueOf(appPlayerInitConfig.getUiConf().getPartnerId()), appPlayerInitConfig.getKs(), appPlayerInitConfig.getUiConf().getBaseUrl(), new PlayerConfigManager.OnPlayerConfigLoaded() {
                @Override
                public void onConfigLoadComplete(int id, JsonObject studioUiConfJson, ErrorElement error, int freshness) {
                    buildPlayer(studioUiConfJson, appPlayerInitConfig, currentPlayedMediaIndex, playerType);
                }
            });
        }
    }

    public void changeMedia() {
        if (player != null) {
            player.stop();
        }
        updatePluginsConfig();
        if (player instanceof KalturaOvpPlayer) {
            OVPMediaOptions ovpMediaOptions = buildOvpMediaOptions(0, appPlayerInitConfig.getPreferredFormat(), currentPlayedMediaIndex);
            player.loadMedia(ovpMediaOptions, new KalturaPlayer.OnEntryLoadListener() {
                @Override
                public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
                    log.d("OVPMedia onEntryLoadComplete; " + entry + "; " + error);
                    handleOnEntryLoadCompleate(error);
                }
            });
        } else {
            OTTMediaOptions ottMediaOptions = buildOttMediaOptions(0, appPlayerInitConfig.getPreferredFormat(), currentPlayedMediaIndex);
            player.loadMedia(ottMediaOptions, new KalturaPlayer.OnEntryLoadListener() {
                @Override
                public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
                    log.d("OTTMedia onEntryLoadComplete; " + entry + "; " + error);
                    handleOnEntryLoadCompleate(error);
                }
            });
        }
    }

    private void updatePluginsConfig() {
        if (initOptions.pluginConfigs.hasConfig(IMAPlugin.factory.getName())) {
            JsonObject imaJson = (JsonObject) initOptions.pluginConfigs.getPluginConfig(IMAPlugin.factory.getName());
            IMAConfig imaPluginConfig = gson.fromJson(imaJson, IMAConfig.class);

            //Example to update the AdTag
            //imaPluginConfig.setAdTagUrl("http://externaltests.dev.kaltura.com/playKitApp/adManager/customAdTags/vmap/inline/ima_pre_mid_post_bumber2.xml");
            initOptions.pluginConfigs.setPluginConfig(IMAPlugin.factory.getName(), imaPluginConfig.toJson());
        }

        //EXAMPLE if there are no auto replacers in this format ->  {{key}}
//        if (initOptions.pluginConfigs.hasConfig(YouboraPlugin.factory.getName())) {
//            JsonObject imaJson = (JsonObject) initOptions.pluginConfigs.getPluginConfig(YouboraPlugin.factory.getName());
//            YouboraConfig youboraPluginConfig = gson.fromJson(imaJson, YouboraConfig.class);
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

    private void handleOnEntryLoadCompleate(ErrorElement error) {
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

    private void buildPlayer(JsonObject studioUiConfJson, PlayerConfig appPlayerInitConfig, int playListMediaIndex, String playerType) {
        KalturaPlayer player = null;
        appPlayerInitConfig.setPlayerConfig(studioUiConfJson);
        JsonArray appPluginConfigJsonObject = appPlayerInitConfig.getPluginConfigs();
        int playerUiConfId = -1;
        if (appPlayerInitConfig.getUiConf() != null) {
            playerUiConfId = Integer.valueOf(appPlayerInitConfig.getUiConf().getId());
        }
        initOptions = new PlayerInitOptions(Integer.valueOf(appPlayerInitConfig.getPartnerId()), playerUiConfId, appPlayerInitConfig.getPlayerConfig())
                .setAutoPlay(appPlayerInitConfig.getAutoPlay())
                .setKs(appPlayerInitConfig.getKs())
                .setPreload(appPlayerInitConfig.getPreload())
                .setReferrer(appPlayerInitConfig.getReferrer())
                .setServerUrl(appPlayerInitConfig.getBaseUrl())
                .setAllowCrossProtocolEnabled(false)
                .setPluginConfigs(convertPluginsJsonArrayToPKPlugins(appPluginConfigJsonObject));

        mediaList = appPlayerInitConfig.getMediaList();

        if ("ovp".equals(playerType.toLowerCase())) {
            player = KalturaOvpPlayer.create(PlayerActivity.this, initOptions);

            OVPMediaOptions ovpMediaOptions = buildOvpMediaOptions(appPlayerInitConfig.getStartPosition(), appPlayerInitConfig.getPreferredFormat(), playListMediaIndex);
            player.loadMedia(ovpMediaOptions, new KalturaPlayer.OnEntryLoadListener() {
                @Override
                public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
                    log.d("OVPMedia onEntryLoadComplete; " + entry + "; " + error);
                    if (error != null) {
                        log.d("OVPMedia Error Extra = " + error.getExtra());
                        Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();

                        playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
                        playbackControlsManager.showControls(View.VISIBLE);
                    } else {
                        if (!initOptions.autoplay) {
                            playbackControlsManager.showControls(View.VISIBLE);
                        }
                    }
                }
            });
        } else  if ("ott".equals(playerType.toLowerCase())) {
            player = KalturaOTTPlayer.create(PlayerActivity.this, initOptions);
            OTTMediaOptions ottMediaOptions = buildOttMediaOptions(appPlayerInitConfig.getStartPosition(), appPlayerInitConfig.getPreferredFormat() , playListMediaIndex);
            player.loadMedia(ottMediaOptions, new KalturaPlayer.OnEntryLoadListener() {
                @Override
                public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
                    log.d("OTTMedia onEntryLoadComplete; " + entry + "; " + error);
                    if (error != null) {
                        log.d("OTTMedia Error Extra = " + error.getExtra());
                        Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
                        playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
                        playbackControlsManager.showControls(View.VISIBLE);
                    } else {
                        if (!initOptions.autoplay) {
                            playbackControlsManager.showControls(View.VISIBLE);
                        }
                    }
                }
            });
        }

        if (player != null) {
            setPlayer(player);
            playbackControlsManager = new PlaybackControlsManager(this, player, playbackControlsView);
            if (appPlayerInitConfig.getMediaList().size() > 1) {
                playbackControlsManager.addChangeMediaButtonsListener(appPlayerInitConfig.getMediaList().size());
            }
            playbackControlsManager.updatePrevNextBtnFunctionality(currentPlayedMediaIndex, appPlayerInitConfig.getMediaList().size());
        } else {
            log.e("Failed to initialize player...");
        }
        setPlayerListeners();
    }

    @NonNull
    private OTTMediaOptions buildOttMediaOptions(int startPosition, String preferredFormat, int playListMediaIndex) {
        Media ottMedia = mediaList.get(playListMediaIndex);
        OTTMediaOptions ottMediaOptions = new OTTMediaOptions()
                .setAssetId(ottMedia.getAssetId())
                .setAssetType(ottMedia.getAssetType())
                .setContextType(ottMedia.getPlaybackContextType());
        if (!TextUtils.isEmpty(ottMedia.getFormat())) {
            ottMediaOptions.setFormats(new String [] {ottMedia.getFormat()});
        }
        if (ottMedia.getFileId() != null) {
            ottMediaOptions.setFileIds(new String [] {String.valueOf(ottMedia.getFileId())});
        }

        ottMediaOptions.setKS(ottMedia.getKs());
        ottMediaOptions.setStartPosition(startPosition);
        ottMediaOptions.setPreferredMediaFormat(preferredFormat, initOptions.preferredMediaFormat);
        return ottMediaOptions;
    }

    @NonNull
    private OVPMediaOptions buildOvpMediaOptions(int startPosition, String preferredFormat, int playListMediaIndex) {
        Media ovpMedia = mediaList.get(playListMediaIndex);
        OVPMediaOptions ovpMediaOptions = new OVPMediaOptions().setEntryId(ovpMedia.getEntryId());
        ovpMediaOptions.setKS(ovpMedia.getKs());
        ovpMediaOptions.setStartPosition(startPosition);
        ovpMediaOptions.setPreferredMediaFormat(preferredFormat, initOptions.preferredMediaFormat);
        return ovpMediaOptions;
    }

    private void setPlayerListeners() {
        player.addEventListener(new PKEvent.Listener() {
                                    @Override
                                    public void onEvent(PKEvent event) {
                                        log.d("onEvent " + event.eventType().name());

                                        Enum receivedEventType = event.eventType();
                                        if (event instanceof AdEvent) {
                                            updateEventsLogsList("ad:\n" + event.eventType().name());

                                            if (receivedEventType == AdEvent.Type.ERROR) {
                                                AdEvent.Error adError = (AdEvent.Error) event;
                                                playbackControlsManager.setAdPlayerState(adError.type);
                                            } else if (receivedEventType == CAN_PLAY) {

                                            } else if (receivedEventType == AdEvent.Type.CUEPOINTS_CHANGED) {
                                                adCuePoints = ((AdEvent.AdCuePointsUpdateEvent) event).cuePoints;
                                            } else if (receivedEventType == AdEvent.Type.ALL_ADS_COMPLETED) {

                                                playbackControlsManager.setAdPlayerState(AdEvent.Type.ALL_ADS_COMPLETED);
                                                allAdsCompeted = true;
                                                if (isPlaybackEndedState()) {
                                                    playbackControlsManager.showControls(View.VISIBLE);
                                                }
                                            } else if (receivedEventType == AdEvent.Type.CONTENT_PAUSE_REQUESTED) {
                                                playbackControlsManager.setAdPlayerState(AdEvent.Type.CONTENT_PAUSE_REQUESTED);
                                                playbackControlsManager.showControls(View.INVISIBLE);
                                            } else if (receivedEventType == AdEvent.Type.CONTENT_RESUME_REQUESTED) {
                                                playbackControlsManager.setAdPlayerState(AdEvent.Type.CONTENT_RESUME_REQUESTED);
                                                playbackControlsManager.showControls(View.INVISIBLE);
                                            } else if (receivedEventType == AdEvent.Type.LOADED) {
                                                playbackControlsManager.setAdPlayerState(AdEvent.Type.LOADED);
                                            } else if (receivedEventType == AdEvent.Type.STARTED) {
                                                playbackControlsManager.setAdPlayerState(AdEvent.Type.STARTED);
                                                allAdsCompeted = false;
                                                AdInfo adInfo = ((AdEvent.AdStartedEvent) event).adInfo;
                                                playbackControlsManager.showControls(View.INVISIBLE);
                                            } else if (receivedEventType == AdEvent.Type.TAPPED) {
                                                playbackControlsManager.handleContainerClick();
                                            } else if (receivedEventType == AdEvent.Type.COMPLETED) {

                                            } else if (receivedEventType == AdEvent.Type.PAUSED) {
                                                playbackControlsManager.setAdPlayerState(AdEvent.Type.PAUSED);
                                            } else if (receivedEventType == AdEvent.Type.RESUMED) {
                                                playbackControlsManager.setAdPlayerState(AdEvent.Type.RESUMED);
                                                playbackControlsManager.showControls(View.INVISIBLE);
                                            }
                                            else if (receivedEventType == AdEvent.Type.SKIPPED) {
                                                playbackControlsManager.setAdPlayerState(AdEvent.Type.SKIPPED);
                                            }
                                        }
                                        if (event instanceof PlayerEvent) {
                                            updateEventsLogsList("player:\n" + event.eventType().name());
                                            playbackControlsManager.setContentPlayerState(event.eventType());
                                            if (receivedEventType == PLAYING) {
                                                playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.pause);
                                                playbackControlsManager.showControls(View.INVISIBLE);
                                            } else if (receivedEventType == STOPPED) {
                                                playbackControlsManager.showControls(View.INVISIBLE);
                                            } else if (receivedEventType == SOURCE_SELECTED) {
                                                PlayerEvent.SourceSelected sourceSelected = (PlayerEvent.SourceSelected) event;
                                                log.d("Selected Source = " + sourceSelected.source.getUrl());
                                            } else if (receivedEventType == ENDED) {
                                                playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.replay);
                                                if (!isPostrollAvailableInAdCuePoint()) {
                                                    //playbackControlsManager.showControls(View.VISIBLE);
                                                }
                                            } else if (receivedEventType == TRACKS_AVAILABLE) {
                                                PlayerEvent.TracksAvailable tracksAvailable = (PlayerEvent.TracksAvailable) event;
                                                //Obtain the actual tracks info from it.
                                                PKTracks tracks = tracksAvailable.tracksInfo;
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
                                            }
                                        } else if (receivedEventType == AdEvent.Type.CUEPOINTS_CHANGED || receivedEventType == AdEvent.Type.PAUSED || receivedEventType == AdEvent.Type.RESUMED ||
                                                receivedEventType == AdEvent.Type.STARTED || receivedEventType == AdEvent.Type.COMPLETED||
                                                receivedEventType == AdEvent.Type.ALL_ADS_COMPLETED){

                                        }
                                    }
                                }, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.STOPPED, PlayerEvent.Type.CAN_PLAY, PlayerEvent.Type.SOURCE_SELECTED, PlayerEvent.Type.SEEKING, PlayerEvent.Type.SEEKED, PlayerEvent.Type.PLAYING,  PlayerEvent.Type.ENDED, PlayerEvent.Type.TRACKS_AVAILABLE,
                AdEvent.Type.ERROR, AdEvent.Type.LOADED, AdEvent.Type.SKIPPED, AdEvent.Type.TAPPED, AdEvent.Type.CONTENT_PAUSE_REQUESTED, AdEvent.Type.CONTENT_RESUME_REQUESTED, AdEvent.Type.STARTED, AdEvent.Type.LOADED, AdEvent.Type.PAUSED, AdEvent.Type.RESUMED,
                AdEvent.Type.COMPLETED, AdEvent.Type.ALL_ADS_COMPLETED,AdEvent.Type.CUEPOINTS_CHANGED);


        player.addStateChangeListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {

                PlayerEvent.StateChanged stateChanged = (PlayerEvent.StateChanged) event;
                log.d("stateChangeEvent " + event.eventType().name() + " = " + stateChanged.newState);
                updateEventsLogsList("player:\n" + event.eventType().name() + ":" + stateChanged.newState);
                switch (stateChanged.newState){
                    case IDLE:
                        log.d("StateChange Idle");
                        break;
                    case LOADING:
                        log.d("StateChange Loading");
                        break;
                    case READY:
                        log.d("StateChange Ready");
                        break;
                    case BUFFERING:
                        log.d("StateChange Buffering");
                        break;
                }
            }
        });

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                KalturaStatsEvent.KalturaStatsReport reportEvent = (KalturaStatsEvent.KalturaStatsReport) event;
                String reportedEventName = reportEvent.reportedEventName;
                if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                    updateEventsLogsList("stats:\n" + reportedEventName);
                }
            }
        }, KalturaStatsEvent.Type.REPORT_SENT);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                KavaAnalyticsEvent.KavaAnalyticsReport reportEvent= (KavaAnalyticsEvent.KavaAnalyticsReport) event;
                String reportedEventName = reportEvent.reportedEventName;
                if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                    updateEventsLogsList("kava:\n" + reportedEventName);
                }
            }
        }, KavaAnalyticsEvent.Type.REPORT_SENT);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                YouboraEvent.YouboraReport reportEvent = (YouboraEvent.YouboraReport) event;
                String reportedEventName = reportEvent.reportedEventName;
                if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                    updateEventsLogsList("youbora:\n" + reportedEventName);
                }

            }
        }, YouboraEvent.Type.REPORT_SENT);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                PhoenixAnalyticsEvent.PhoenixAnalyticsReport reportEvent = (PhoenixAnalyticsEvent.PhoenixAnalyticsReport) event;
                String reportedEventName = reportEvent.reportedEventName;
                if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                    updateEventsLogsList("phoenix:\n" + reportedEventName);
                }

            }
        }, PhoenixAnalyticsEvent.Type.REPORT_SENT);
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
                if (YouboraPlugin.factory.getName().equals(pluginName)) {
                    YouboraConfig youboraPlugin = gson.fromJson(pluginDescriptor.getParams().get("options"), YouboraConfig.class);
                    pkPluginConfigs.setPluginConfig(YouboraPlugin.factory.getName(), youboraPlugin.toJson());
                } else if (KavaAnalyticsPlugin.factory.getName().equals(pluginName)) {
                    KavaAnalyticsConfig kavaPluginConfig = gson.fromJson(pluginDescriptor.getParams(), KavaAnalyticsConfig.class);
                    pkPluginConfigs.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), kavaPluginConfig.toJson());
                } else if (IMAPlugin.factory.getName().equals(pluginName)) {
                    UiConfFormatIMAConfig imaPluginConfig = gson.fromJson(pluginDescriptor.getParams(), UiConfFormatIMAConfig.class);
                    pkPluginConfigs.setPluginConfig(IMAPlugin.factory.getName(), imaPluginConfig.toJson());
                } else if (PhoenixAnalyticsPlugin.factory.getName().equals(pluginName)) {
                    PhoenixAnalyticsConfig phoenixAnalyticsConfig = gson.fromJson(pluginDescriptor.getParams(), PhoenixAnalyticsConfig.class);
                    pkPluginConfigs.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixAnalyticsConfig.toJson());
                } else if (KalturaStatsPlugin.factory.getName().equals(pluginName)) {
                    KalturaStatsConfig kalturaStatsPluginConfig = gson.fromJson(pluginDescriptor.getParams(), KalturaStatsConfig.class);
                    pkPluginConfigs.setPluginConfig(KalturaStatsPlugin.factory.getName(), kalturaStatsPluginConfig.toJson());
                } else if (KalturaLiveStatsPlugin.factory.getName().equals(pluginName)) {
                    KalturaLiveStatsConfig kalturaLiveStatsPluginConfig = gson.fromJson(pluginDescriptor.getParams(), KalturaLiveStatsConfig.class);
                    pkPluginConfigs.setPluginConfig(KalturaLiveStatsPlugin.factory.getName(), kalturaLiveStatsPluginConfig.toJson());
                }
            }
        }
        return pkPluginConfigs;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadPlayerConfig() {
        PlayerConfigManager.initialize(this);
        if (uiConfId == null) {
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
            player.stop();
            player.destroy();
            player = null;
            eventsList.clear();
        }
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
                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (playbackControlsManager != null) {
                            playbackControlsManager.handleContainerClick();
                        }
                    }
                });
                container.addView(player.getView());
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

        if (player != null) {
            player.onApplicationResumed();
            playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
        }

    }

    @Override
    protected void onPause() {
        if (player != null) {
            player.onApplicationPaused();
        }
        super.onPause();
        playbackControlsManager.showControls(View.VISIBLE);
    }
}
