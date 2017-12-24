package com.kaltura.playerdemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.ovpplayer.KalturaOvpPlayer;
import com.kaltura.ovpplayer.OVPMediaOptions;
import com.kaltura.playkit.Utils;

public class OVPActivity extends BaseDemoActivity<KalturaOvpPlayer> {

    KalturaOvpPlayer player(Context context, JsonObject playerConfig) {
        return KalturaOvpPlayer.create(context, initOptions);
    }

    @Override
    protected DemoItem[] items() {
        return items;
    }

    @Override
    protected void loadConfigFile() {
        final String jsonString = Utils.readAssetToString(this, "ovp.json");
        final JsonObject json = GsonParser.toJson(jsonString).getAsJsonObject();

        parseCommonOptions(json);
    }

    @Override
    @NonNull
    protected DemoItem parseItem(JsonObject object) {
        return new DemoItem(object.get("name").getAsString(), object.get("entryId").getAsString());
    }

    @Override
    protected void loadPlayerConfig() {
        PlayerConfigManager.initialize(this);
        PlayerConfigManager.retrieve(uiConfId, initOptions.partnerId, ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(OVPActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
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

        OVPMediaOptions mediaOptions = new OVPMediaOptions().setEntryId(item.id);

        player.loadMedia(mediaOptions, this);
    }

    @Override
    protected String demoName() {
        return "OVP Player Demo";
    }
}
