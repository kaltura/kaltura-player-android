package com.kaltura.playerdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.KalturaPlayer;
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.Utils;
import com.kaltura.tvplayer.ott.KalturaOTTPlayer;
import com.kaltura.tvplayer.ott.OTTMediaOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class OTTDemoActivity extends BaseDemoActivity {

    private static final PKLog log = PKLog.get("OTTDemoActivity");

    private DemoItem currentItem;

    @Override
    protected DemoItem[] items() {
        return items;
    }

    @Override
    protected void loadConfigFile() {
        final String jsonString = Utils.readAssetToString(this, "ott/main.json");
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

        if (uiConfId == null || uiConfPartnerId == null) {
            return;
        }
        
        PlayerConfigManager.retrieve(uiConfId, uiConfPartnerId, ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(OTTDemoActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
                playerConfig = config;
            }
        });
    }

    @Override
    protected void loadItem(DemoItem item) {
        this.currentItem = item;
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void playerActivityLoaded(PlayerActivity playerActivity) {

        KalturaOTTPlayer player = KalturaOTTPlayer.create(playerActivity, initOptions);

        player.loadMedia(new OTTMediaOptions().setAssetId(currentItem.id), new KalturaPlayer.OnEntryLoadListener() {
            @Override
            public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
                log.d("onEntryLoadComplete; " + entry + "; " + error);
            }
        });

        playerActivity.setPlayer(player);
    }


    @Override
    protected String demoName() {
        return "OTT Player Demo";
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    static class TVItem extends DemoItem {
    
        final String[] fileIds;
    
        TVItem(String name, String id, String[] fileIds) {
            super(name, id);
    
            this.fileIds = fileIds;
        }
    }
}
