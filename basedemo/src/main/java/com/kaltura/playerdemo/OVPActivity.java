package com.kaltura.playerdemo;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.kalturaplayer.PlayerInitOptions;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.ovpplayer.KalturaOvpPlayer;
import com.kaltura.ovpplayer.OVPMediaOptions;

public class OVPActivity extends BaseDemoActivity<KalturaOvpPlayer> {

    int partnerId = 2215841;
    int uiConfId = 41188731;
    String ks = null;

    DemoItem[] items = {
            new DemoItem("Sintel", "1_w9zx2eti"),
            new DemoItem("Sintel - snippet", "1_9bwuo813"),
    };

    KalturaOvpPlayer player(Context context, JsonObject playerConfig) {
        return KalturaOvpPlayer.create(context,
                new PlayerInitOptions()
                        .setPlayerConfig(playerConfig)
                        .setAutoPlay(true)
                        .setPartnerId(partnerId));
    }

    @Override
    protected DemoItem[] items() {
        return items;
    }

    @Override
    protected void loadPlayerConfig() {
        PlayerConfigManager.initialize(this);
        PlayerConfigManager.retrieve(uiConfId, partnerId, ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(OVPActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
                playerConfig = config;
            }
        });
    }

    @Override
    protected int partnerId() {
        return partnerId;
    }

    @Override
    protected void loadItem(DemoItem item) {

        if (player == null) {
            player = player(this, playerConfig);
            playerContainer.removeAllViews();
            playerContainer.addView(player.getView());
        }
        
        player.stop();

        OVPMediaOptions mediaOptions = new OVPMediaOptions().setEntryId(item.id);

        player.loadMedia(mediaOptions, this);
    }
}
