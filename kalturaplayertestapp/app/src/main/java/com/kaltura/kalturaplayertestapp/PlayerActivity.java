package com.kaltura.kalturaplayertestapp;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.plugins.ima.IMAPlugin;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaStatsPlugin;
import com.kaltura.playkit.plugins.youbora.YouboraPlugin;
import com.kaltura.playkit.plugins.youbora.pluginconfig.YouboraConfig;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlayerConfigManager;
import com.kaltura.tvplayer.PlayerInitOptions;
import com.kaltura.tvplayer.ott.KalturaOTTPlayer;
import com.kaltura.tvplayer.ott.OTTMediaOptions;
import com.kaltura.tvplayer.ovp.KalturaOvpPlayer;
import com.kaltura.tvplayer.ovp.OVPMediaOptions;

import java.util.List;
import java.util.Set;

import static com.kaltura.kalturaplayertestapp.Utils.safeString;

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

    Integer uiConfId;
    String ks;
    List<Media> mediaList;
    Integer uiConfPartnerId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


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
    }

    private PKPluginConfigs convertPluginsJsonArrayToPKPlugins(JsonArray pluginConfigs) {
        //JsonObject uiConfPluginsConfig = TvPlayerUtils.getPluginsConfig(uiConfPlayerConfig);
        //UiConfYouboraConfig uiConfYouboraConfig = TvPlayerUtils.getUiConfYouboraConfig(uiConfPluginsConfig);
        //UiConfFormatIMAConfig uiConfIMAConfig= TvPlayerUtils.getUiConfIMAConfig(uiConfPluginsConfig);
        //KavaAnalyticsConfig kavaAnalyticsConfig = TvPlayerUtils.getUiConfKavaConfig(partnerId, uiConfPluginsConfig);

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

                // Now it's safe to look at `supportedDrmSchemes`
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
//            ViewGroup container = findViewById(R.id.player_container);
//            container.removeAllViews();

            player.stop();
            player.destroy();
            player = null;
        }
    }

    public void setPlayer(KalturaPlayer player) {
        this.player = player;
        if (player == null) {
            Log.e("XXX", "Player is null");

            return;
        }
        ViewGroup container = findViewById(R.id.player_container);
        container.addView(player.getView());
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
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) player.getView().getLayoutParams();
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
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) player.getView().getLayoutParams();
            if (params != null) {
                params.width = params.MATCH_PARENT;
                params.height = 600;
                //getWindow().setFlags(params.width, params.height=600);
                player.getView().setLayoutParams(params);
            }
        }
    }

    @Override
    protected void onResume() {
        log.d("Ad Event onResume");
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
