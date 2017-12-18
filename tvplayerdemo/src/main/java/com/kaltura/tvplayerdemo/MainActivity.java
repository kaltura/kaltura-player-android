package com.kaltura.tvplayerdemo;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.baseplayerdemo.BaseDemoActivity;
import com.kaltura.baseplayerdemo.DemoItem;
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.kalturaplayer.PlayerInitOptions;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.tvplayer.KalturaTvPlayer;
import com.kaltura.tvplayer.TVMediaOptions;


class TVItem extends DemoItem {

    final String[] fileIds;

    TVItem(String name, String id, String[] fileIds) {
        super(name, id);

        this.fileIds = fileIds;
    }
}

public class MainActivity extends BaseDemoActivity<KalturaTvPlayer> {

    String serverUrl = "http://api-preprod.ott.kaltura.com/v4_5/api_v3/";
    int partnerId = 198;
    int ovpPartnerId = 2215841;     // for player config
    int uiConfId = 41188731;
    String ks = null;

    DemoItem[] items = {
            new TVItem("Something", "259153", new String[]{"804398"})
    };

    KalturaTvPlayer player(Context context, JsonObject playerConfig) {
        return KalturaTvPlayer.create(context, new PlayerInitOptions()
                .setPlayerConfig(playerConfig)
                .setAutoPlay(true)
                .setServerUrl(serverUrl)
                .setPartnerId(partnerId));
    }

    @Override
    protected DemoItem[] items() {
        return items;
    }

    @Override
    protected int partnerId() {
        return partnerId;
    }

    @Override
    protected void loadPlayerConfig() {
        PlayerConfigManager.initialize(this);
        PlayerConfigManager.retrieve(uiConfId, ovpPartnerId, ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(MainActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
                playerConfig = config;
            }
        });
    }

    @Override
    protected void loadItem(DemoItem item) {

        if (player == null) {
            player = player(this, playerConfig);
            playerContainer.removeAllViews();
            playerContainer.addView(player.getView());
        }
        
        player.stop();

        TVMediaOptions mediaOptions = new TVMediaOptions()
                .setAssetId(item.id)
                .setFileIds(((TVItem) item).fileIds);

        player.loadMedia(mediaOptions, this);
    }
}
