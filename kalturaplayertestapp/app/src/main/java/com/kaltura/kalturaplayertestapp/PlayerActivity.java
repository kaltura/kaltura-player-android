package com.kaltura.kalturaplayertestapp;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.kaltura.kalturaplayertestapp.converters.Media;
import com.kaltura.kalturaplayertestapp.converters.PlayerConfig;
import com.kaltura.kalturaplayertestapp.converters.PluginDescriptor;

import com.kaltura.kalturaplayertestapp.models.ima.UiConfFormatIMAConfig;
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
import com.kaltura.playkit.plugins.ima.IMAPlugin;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsEvent;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.kava.KavaEvents;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaStatsEvent;
import com.kaltura.playkit.plugins.ovp.KalturaStatsPlugin;
import com.kaltura.playkit.plugins.youbora.YouboraEvent;
import com.kaltura.playkit.plugins.youbora.YouboraPlugin;
import com.kaltura.playkit.plugins.youbora.pluginconfig.YouboraConfig;
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

import static com.kaltura.kalturaplayertestapp.Utils.safeString;
import static com.kaltura.playkit.PlayerEvent.Type.CAN_PLAY;
import static com.kaltura.playkit.PlayerEvent.Type.ENDED;
import static com.kaltura.playkit.PlayerEvent.Type.PLAYING;

//import org.greenrobot.eventbus.EventBus;

public class PlayerActivity extends AppCompatActivity {

    private static final PKLog log = PKLog.get("PlayerActivity");
    public static final String PLAYER_CONFIG_JSON_KEY = "player_config_json_key";
    public static final String PLAYER_CONFIG_TITLE_KEY = "player_config_title_key";
    private Gson gson = new Gson();
    private KalturaPlayer player;
    private JsonObject playerConfig;
    private PlayerInitOptions initOptions;
    private String playerConfigTitle;
    private String playerInitOptionsJson;
    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");


    private Integer uiConfId;
    private String ks;
    private List<Media> mediaList;
    private Integer uiConfPartnerId;
    private EventsAdapter recyclerAdapter;
    private RecyclerView eventsListView;
    private List<String> eventsList = new ArrayList<>();
    private SearchView searchView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        eventsListView = findViewById(R.id.events_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        eventsListView.setLayoutManager(layoutManager);
        recyclerAdapter = new EventsAdapter();
        eventsListView.setAdapter(recyclerAdapter);
        searchView = findViewById(R.id.search_events);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                String queryToLowerCase = query.toLowerCase();
                List<String> searchedEvents = new ArrayList<>();
                for (String eventItem : eventsList) {
                    if (eventItem.toLowerCase().contains(queryToLowerCase)) {
                        searchedEvents.add(eventItem);
                    }
                }
                recyclerAdapter.notifyData(searchedEvents);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    recyclerAdapter.notifyData(eventsList);
                    return false;
                }
                return false;
            }
        });


        playerConfigTitle = getIntent().getExtras().getString(PlayerActivity.PLAYER_CONFIG_TITLE_KEY);
        playerInitOptionsJson = getIntent().getExtras().getString(PlayerActivity.PLAYER_CONFIG_JSON_KEY);

        if (playerConfigTitle == null|| playerInitOptionsJson == null) {
            throw new IllegalArgumentException("Must pass extra " + PlayerActivity.PLAYER_CONFIG_JSON_KEY);
        }
        initDrm();

        JsonElement jElement = new JsonParser().parse(playerInitOptionsJson);
        JsonObject config = jElement.getAsJsonObject();

        final String playerType = safeString(config, "playerType");


        final PlayerConfig appPlayerInitConfig = gson.fromJson(config.toString(), PlayerConfig.class);

        if (appPlayerInitConfig.getUiConf() == null) {
            log.e("App config json is invalid");
        } else {
            PlayerConfigManager.retrieve(Integer.valueOf(appPlayerInitConfig.getUiConf().getId()), Integer.valueOf(appPlayerInitConfig.getUiConf().getPartnerId()), appPlayerInitConfig.getKs(), appPlayerInitConfig.getUiConf().getBaseUrl(), new PlayerConfigManager.OnPlayerConfigLoaded() {
                @Override
                public void onConfigLoadComplete(int id, JsonObject studioUiConfJson, ErrorElement error, int freshness) {
                    buildPlayer(studioUiConfJson, appPlayerInitConfig, playerType);
                }
            });
        }
    }

    private void buildPlayer(JsonObject studioUiConfJson, PlayerConfig appPlayerInitConfig, String playerType) {
        KalturaPlayer player = null;
        appPlayerInitConfig.setPlayerConfig(studioUiConfJson);
        JsonArray appPluginConfigJsonObject = appPlayerInitConfig.getPluginConfigs();
        initOptions = new PlayerInitOptions(Integer.valueOf(appPlayerInitConfig.getPartnerId()), Integer.valueOf(appPlayerInitConfig.getUiConf().getId()), appPlayerInitConfig.getPlayerConfig())
                .setAutoPlay(appPlayerInitConfig.getAutoPlay())
                .setKs(appPlayerInitConfig.getKs())
                .setPreload(appPlayerInitConfig.getPreload())
                .setReferrer(appPlayerInitConfig.getReferrer())
                .setServerUrl(appPlayerInitConfig.getBaseUrl())
                .setAllowCrossProtocolEnabled(false)
                .setPluginConfigs(convertPluginsJsonArrayToPKPlugins(appPluginConfigJsonObject));

        mediaList = appPlayerInitConfig.getMediaList();
        //TvPlayerUtils.parseInitOptions(config.getAsJsonObject("initOptions"));

        if ("ovp".equals(playerType.toLowerCase())) {
            player = KalturaOvpPlayer.create(PlayerActivity.this, initOptions);
            OVPMediaOptions ovpMediaOptions = new OVPMediaOptions().setEntryId(mediaList.get(0).getEntryId());
            ovpMediaOptions.setKS(mediaList.get(0).getKs());
            ovpMediaOptions.setStartPosition(appPlayerInitConfig.getStartPosition());
            ovpMediaOptions.setPreferredMediaFormat(appPlayerInitConfig.getPreferredFormat(), initOptions.preferredMediaFormat);

            player.loadMedia(ovpMediaOptions, new KalturaPlayer.OnEntryLoadListener() {
                @Override
                public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
                    log.d("OVPMedia onEntryLoadComplete; " + entry + "; " + error);
                }
            });
        } else  if ("ott".equals(playerType.toLowerCase())) {
            player = KalturaOTTPlayer.create(PlayerActivity.this, initOptions);
            Media ottMedia = mediaList.get(0);
            OTTMediaOptions ottMediaOptions = new OTTMediaOptions()
                    .setAssetId(ottMedia.getAssetId())
                    .setAssetType(ottMedia.getAssetType())
                    .setContextType(ottMedia.getPlaybackContextType());
            ottMediaOptions.setKS(ottMedia.getKs());
            ottMediaOptions.setStartPosition(appPlayerInitConfig.getStartPosition());
            ottMediaOptions.setPreferredMediaFormat(appPlayerInitConfig.getPreferredFormat(), initOptions.preferredMediaFormat);
            player.loadMedia(ottMediaOptions, new KalturaPlayer.OnEntryLoadListener() {
                @Override
                public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
                    log.d("OTTMedia onEntryLoadComplete; " + entry + "; " + error);
                }
            });
        }
        if (player != null) {
            setPlayer(player);
        } else {
            log.e("Failed to initialze player...");
        }
        setPlayerListeners();
    }

    private void setPlayerListeners() {
        player.addEventListener(new PKEvent.Listener() {
                                    @Override
                                    public void onEvent(PKEvent event) {
                                        log.d("XXX onEvent " + event.eventType().name());

                                        Enum receivedEventType = event.eventType();
                                        if (event instanceof AdEvent) {
                                            Date date = new Date();
                                            eventsList.add(dateFormat.format(date) + " ad:\n" + event.eventType().name());
                                            recyclerAdapter.notifyData(eventsList);
                                            if (receivedEventType == AdEvent.Type.ERROR) {
                                                AdEvent.Error adError = (AdEvent.Error) event;
                                            } else if (receivedEventType == CAN_PLAY) {

                                            } else if (receivedEventType == PLAYING) {

                                            } else if (receivedEventType == ENDED) {

                                            } else if (receivedEventType == AdEvent.Type.CUEPOINTS_CHANGED) {
                                                AdCuePoints adCuePoints = ((AdEvent.AdCuePointsUpdateEvent) event).cuePoints;

                                            } else if (receivedEventType == AdEvent.Type.ALL_ADS_COMPLETED) {

                                            } else if (receivedEventType == AdEvent.Type.CONTENT_PAUSE_REQUESTED) {

                                            } else if (receivedEventType == AdEvent.Type.CONTENT_RESUME_REQUESTED) {

                                            } else if (receivedEventType == AdEvent.Type.LOADED) {

                                            } else if (receivedEventType == AdEvent.Type.STARTED) {
                                                AdInfo adInfo = ((AdEvent.AdStartedEvent) event).adInfo;
                                            } else if (receivedEventType == AdEvent.Type.TAPPED) {

                                            } else if (receivedEventType == AdEvent.Type.COMPLETED) {

                                            } else if (receivedEventType == AdEvent.Type.SKIPPED) {

                                            }
                                        }
                                        if (event instanceof PlayerEvent) {
                                            Date date = new Date();
                                            eventsList.add(dateFormat.format(date) + " player:\n" + event.eventType().name());
                                            recyclerAdapter.notifyData(eventsList);
                                        } else if (receivedEventType == AdEvent.Type.CUEPOINTS_CHANGED || receivedEventType == AdEvent.Type.PAUSED || receivedEventType == AdEvent.Type.RESUMED ||
                                                receivedEventType == AdEvent.Type.STARTED || receivedEventType == AdEvent.Type.COMPLETED||
                                                receivedEventType == AdEvent.Type.ALL_ADS_COMPLETED){

                                        }
                                    }
                                }, PlayerEvent.Type.PLAY, PlayerEvent.Type.PAUSE, PlayerEvent.Type.CAN_PLAY, PlayerEvent.Type.SEEKING, PlayerEvent.Type.SEEKED, PlayerEvent.Type.PLAYING,  PlayerEvent.Type.ENDED, PlayerEvent.Type.TRACKS_AVAILABLE,
                AdEvent.Type.ERROR, AdEvent.Type.LOADED, AdEvent.Type.SKIPPED, AdEvent.Type.TAPPED, AdEvent.Type.CONTENT_PAUSE_REQUESTED, AdEvent.Type.CONTENT_RESUME_REQUESTED, AdEvent.Type.STARTED, AdEvent.Type.LOADED, AdEvent.Type.PAUSED, AdEvent.Type.RESUMED,
                AdEvent.Type.COMPLETED, AdEvent.Type.ALL_ADS_COMPLETED,AdEvent.Type.CUEPOINTS_CHANGED);


        player.addStateChangeListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {

                PlayerEvent.StateChanged stateChanged = (PlayerEvent.StateChanged) event;
                log.d("XXX stateChangeEvent " + event.eventType().name() + " = " + stateChanged.newState);
                Date date = new Date();
                eventsList.add(dateFormat.format(date) + " player:\n" + event.eventType().name() + ":" + stateChanged.newState);
                recyclerAdapter.notifyData(eventsList);
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
                    Date date = new Date();
                    eventsList.add(dateFormat.format(date) + " stats:\n" + reportedEventName);
                    recyclerAdapter.notifyData(eventsList);
                }
            }
        }, KalturaStatsEvent.Type.REPORT_SENT);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                //Cast received event to AnalyticsEvent.BaseAnalyticsReportEvent.
                KavaAnalyticsEvent.KavaAnalyticsReport reportEvent= (KavaAnalyticsEvent.KavaAnalyticsReport) event;
                String reportedEventName = reportEvent.reportedEventName;
                if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                    Date date = new Date();
                    eventsList.add(dateFormat.format(date) + " kava:\n" + reportedEventName);
                    recyclerAdapter.notifyData(eventsList);
                }
            }
        }, KavaAnalyticsEvent.Type.REPORT_SENT);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                YouboraEvent.YouboraReport reportEvent = (YouboraEvent.YouboraReport) event;
                String reportedEventName = reportEvent.reportedEventName;
                if (!PlayerEvent.Type.PLAYHEAD_UPDATED.name().equals(reportedEventName)) {
                    Date date = new Date();
                    eventsList.add(dateFormat.format(date) + " youbora:\n" + reportedEventName);
                    recyclerAdapter.notifyData(eventsList);
                }

            }
        }, YouboraEvent.Type.REPORT_SENT);
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
        this.player = player;
        if (player == null) {
            Log.e("XXX", "Player is null");

            return;
        }
        final ViewGroup container = findViewById(R.id.player_container_layout);
        final PlaybackControlsView playbackControlsView = findViewById(R.id.player_controls);
        container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                container.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                player.setPlayerView(ViewGroup.LayoutParams.MATCH_PARENT, 600);
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

            DisplayMetrics metrics = new DisplayMetrics();
            this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;
            getWindow().setFlags(metrics.widthPixels, metrics.heightPixels);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) player.getView().getLayoutParams();
            if (params != null) {
                params.width = screenWidth;
                params.height = screenHeight;
                //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                player.getView().setLayoutParams(params);
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            //unhide your objects here.
            if(getSupportActionBar()!=null) {
                getSupportActionBar().show();
            }
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) player.getView().getLayoutParams();
            if (params != null) {
                params.width  = params.MATCH_PARENT;
                params.height = params.WRAP_CONTENT;
                params.gravity =  Gravity.START;
                //getWindow().setFlags(params.width, params.height=600);
                player.getView().setLayoutParams(params);
            }
        }
    }

    @Override
    protected void onResume() {
        log.d("Player Activity onResume");
        super.onResume();
        if (player != null) {
            player.onApplicationResumed();
            //if (nowPlaying && AUTO_PLAY_ON_RESUME) {
            //    player.play();
            //}
        }
        //if (controlsView != null) {
        //    controlsView.resume();
        //}
    }

    @Override
    protected void onPause() {
        super.onPause();
        //if (controlsView != null) {
        //    controlsView.release();
        //}
        if (player != null) {
            player.onApplicationPaused();
        }
    }
}
