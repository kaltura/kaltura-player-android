package com.kaltura.playerdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.KalturaPlayer;
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.ovpplayer.KalturaOvpPlayer;
import com.kaltura.ovpplayer.OVPMediaOptions;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class OVPDemoActivity extends BaseDemoActivity {

    private static final PKLog log = PKLog.get("OVPDemoActivity");
    
    private DemoItem currentItem;

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
        if (uiConfId == null) {
            return;
        }
        
        if (uiConfPartnerId == null) {
            uiConfPartnerId = partnerId();
        }
        PlayerConfigManager.retrieve(uiConfId, initOptions.partnerId, ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(OVPDemoActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
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

        KalturaOvpPlayer player = KalturaOvpPlayer.create(playerActivity, initOptions);

        player.loadMedia(new OVPMediaOptions().setEntryId(currentItem.id), new KalturaPlayer.OnEntryLoadListener() {
            @Override
            public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
                log.d("onEntryLoadComplete; " + entry + "; " + error);
            }
        });

        playerActivity.setPlayer(player);
    }

    @Override
    protected String demoName() {
        return "OVP Player Demo";
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
}
