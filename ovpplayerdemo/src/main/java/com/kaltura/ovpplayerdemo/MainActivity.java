package com.kaltura.ovpplayerdemo;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.kalturaplayer.PlayerInitOptions;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.ovpplayer.KalturaOvpPlayer;
import com.kaltura.ovpplayer.OVPMediaOptions;


//class TVItem extends Item {
//
//    final String[] fileIds;
//
//    TVItem(String name, String id, String[] fileIds) {
//        super(name, id);
//
//        this.fileIds = fileIds;
//    }
//}
//
//class TVDemoFactory extends DemoFactory {
//    private static final String serverUrl = "http://api-preprod.ott.kaltura.com/v4_5/api_v3/";
//
//    @Override
//    String serverUrl() {
//        return serverUrl;
//    }
//    
//    @Override
//    int partnerId() {
//        return 198;
//    }
//
//    @Override
//    int uiConfId() {
//        return 41188731;
//    }
//
//    @Override
//    KalturaPlayer player(Context context, JsonObject playerConfig) {
//        return KalturaTvPlayer.create(context, new PlayerInitOptions()
//                .setPlayerConfig(playerConfig)
//                .setAutoPlay(true)
//                .setServerUrl(serverUrl)
//                .setPartnerId(partnerId()));
//    }
//
//    @Override
//    String ks() {
//        return null;
//    }
//
//    @Override
//    MediaOptions mediaOptions(Item item) {
//        return new TVMediaOptions().setAssetId(item.id).setFileIds(((TVItem) item).fileIds);
//    }
//
//    @Override
//    Item[] items() {
//        return new TVItem[] {
//                new TVItem("Something", "259153", new String[]{"804398"})
//        };
//    }
//}

class Item {
    public final String name;
    public final String id;
    
    Item(String name, String id) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " ➜ " + id;
    }
}

public class MainActivity extends BaseDemoActivity<KalturaOvpPlayer> {

    int partnerId = 2215841;
    int uiConfId = 41188731;
    String ks = null;

    Item[] items = {
            new Item("Sintel", "1_w9zx2eti"),
            new Item("Sintel - snippet", "1_9bwuo813"),
    };

    KalturaOvpPlayer player(Context context, JsonObject playerConfig) {
        return KalturaOvpPlayer.create(context,
                new PlayerInitOptions()
                        .setPlayerConfig(playerConfig)
                        .setAutoPlay(true)
                        .setPartnerId(partnerId));
    }

    @Override
    protected Item[] items() {
        return items;
    }

    @Override
    protected void loadPlayerConfig() {
        PlayerConfigManager.initialize(this);
        PlayerConfigManager.retrieve(uiConfId, partnerId, ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(MainActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
                playerConfig = config;
            }
        });
    }

    @Override
    protected void loadItem(Item item) {

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
