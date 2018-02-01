package com.kaltura.kalturaplayertestapp;

import android.content.res.Configuration;
import android.media.MediaCas;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.kalturaplayertestapp.converters.IMAPluginConfig;
import com.kaltura.kalturaplayertestapp.converters.Media;
import com.kaltura.kalturaplayertestapp.converters.PlayerConfig;
import com.kaltura.kalturaplayertestapp.converters.PluginDescriptor;
import com.kaltura.kalturaplayertestapp.converters.YouboraPluginConfig;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.plugins.youbora.YouboraPlugin;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlayerConfigManager;
import com.kaltura.tvplayer.PlayerInitOptions;
import com.kaltura.tvplayer.ott.KalturaOTTPlayer;
import com.kaltura.tvplayer.ott.OTTMediaOptions;
import com.kaltura.tvplayer.ovp.KalturaOvpPlayer;
import com.kaltura.tvplayer.ovp.OVPMediaOptions;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.kaltura.kalturaplayertestapp.Utils.safeString;

//import org.greenrobot.eventbus.EventBus;

public class PlayerActivity extends AppCompatActivity {

    private static final PKLog log = PKLog.get("PlayerActivity");
    public static final String PLAYER_CONFIG_JSON_KEY = "player_config_json_key";
    public static final String PLAYER_CONFIG_TITLE_KEY = "player_config_title_key";

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

        Gson gson = new Gson();
        final PlayerConfig appPlayerInitConfig = gson.fromJson(config.toString(), PlayerConfig.class);

        PluginDescriptor[] pluginDescriptors = gson.fromJson(appPlayerInitConfig.getPluginConfigs(), PluginDescriptor[].class);

        if (pluginDescriptors != null) {
            for (PluginDescriptor pluginDescriptor : pluginDescriptors) {
                switch (pluginDescriptor.getPluginName()) {
                    case "IMAPlugin":
                        IMAPluginConfig imaPluginConfig = gson.fromJson(pluginDescriptor.getParams(), IMAPluginConfig.class);
                        log.d("IMAPlugin");
                        break;
                    case "YouboraPlugin":
                        YouboraPluginConfig youboraPlugin = gson.fromJson(pluginDescriptor.getParams(), YouboraPluginConfig.class);
                        break;
                    default:
                }
            }
        }

        //Build PKPluginConfigs.....


        PlayerConfigManager.retrieve(Integer.valueOf(appPlayerInitConfig.getUiConf().getId()), Integer.valueOf(appPlayerInitConfig.getUiConf().getPartnerId()), appPlayerInitConfig.getKs(), appPlayerInitConfig.getUiConf().getBaseUrl(), new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject studioUiConfJson, ErrorElement error, int freshness) {
                KalturaPlayer player = null;
                appPlayerInitConfig.setPlayerConfig(studioUiConfJson);
                initOptions = new PlayerInitOptions(Integer.valueOf(appPlayerInitConfig.getPartnerId()), Integer.valueOf(appPlayerInitConfig.getUiConf().getId()), appPlayerInitConfig.getPlayerConfig())
                        .setAutoPlay(appPlayerInitConfig.getAutoPlay())
                        .setKs(appPlayerInitConfig.getKs())
                        .setPreload(appPlayerInitConfig.getPreload())
                        .setReferrer(appPlayerInitConfig.getReferrer())
                        .setServerUrl(appPlayerInitConfig.getBaseUrl());
                        //.setPluginConfigs(appPlayerInitConfig.getPluginConfigs());

                mediaList = appPlayerInitConfig.getMediaList();
                //Utils.parseInitOptions(config.getAsJsonObject("initOptions"));

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
        });

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
}
