package com.kaltura.playerdemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.playkit.Utils;
import com.kaltura.tvplayer.KalturaTvPlayer;
import com.kaltura.tvplayer.TVMediaOptions;


class TVItem extends DemoItem {

    final String[] fileIds;

    TVItem(String name, String id, String[] fileIds) {
        super(name, id);

        this.fileIds = fileIds;
    }
}

public class OTTActivity extends BaseDemoActivity<KalturaTvPlayer> {

//    String serverUrl;
//    int partnerId = 198;
    int ovpPartnerId = 2215841;     // for player config
//    int uiConfId = 41188731;
//    String ks = null;
    
    KalturaTvPlayer player(Context context, JsonObject playerConfig) {
        return KalturaTvPlayer.create(context, initOptions);
    }

    @Override
    protected DemoItem[] items() {
        return items;
    }

    @Override
    protected void loadConfigFile() {
        final String jsonString = Utils.readAssetToString(this, "ott.json");
        final JsonObject json = GsonParser.toJson(jsonString).getAsJsonObject();

        parseCommonOptions(json);
    }

    @NonNull
    @Override
    protected DemoItem parseItem(JsonObject object) {
        return new TVItem(object.get("name").getAsString(), object.get("assetId").getAsString(), new String[]{"804398"});
    }

    @Override
    protected void loadPlayerConfig() {
        PlayerConfigManager.initialize(this);
        PlayerConfigManager.retrieve(uiConfId, ovpPartnerId, ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(OTTActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
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

    @Override
    protected String demoName() {
        return "OTT Player Demo";
    }
}
