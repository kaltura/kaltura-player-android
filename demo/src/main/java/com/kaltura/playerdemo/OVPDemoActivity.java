package com.kaltura.playerdemo;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Utils;
import com.kaltura.playkit.utils.GsonReader;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.UIConfManager;
import com.kaltura.tvplayer.PlayerInitOptions;
import com.kaltura.tvplayer.ovp.KalturaOvpPlayer;
import com.kaltura.tvplayer.ovp.OVPMediaOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.URL;

public class OVPDemoActivity extends BaseDemoActivity {

    private static final PKLog log = PKLog.get("OVPDemoActivity");
    
    private DemoItem currentItem;

    @Override
    protected DemoItem[] items() {
        return items;
    }

    @Override
    protected void loadConfigFile() {

        final Uri url = getIntent().getData();
        String jsonString = null;
        
        if (url != null) {
            jsonString = readUrlToString(url);
        } else {
            jsonString = Utils.readAssetToString(this, "ovp/main.json");
        }

        GsonReader reader = GsonReader.withString(jsonString);

        parseCommonOptions(reader);
    }

    private String readUrlToString(Uri url) {
        try {
            return Utils.fullyReadInputStream(new URL(url.toString()).openStream(), 1024*1024).toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    @NonNull
    protected DemoItem parseItem(JsonObject object) {
        return new DemoItem(object.get("name").getAsString(), object.get("entryId").getAsString());
    }

    @Override
    protected void loadPlayerConfig() {
        UIConfManager.initialize(this);
        if (uiConfId == null) {
            return;
        }
        
        UIConfManager.retrieve(uiConfId, uiConfPartnerId, ks, null, new UIConfManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(OVPDemoActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
                uiConf = config;
            }
        });
    }

    @Override
    protected void loadItem(DemoItem item) {
        this.currentItem = item;
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void playerActivityLoaded(PlayerActivity playerActivity) {

        PlayKitManager.registerPlugins(this, DemoPlugin.factory);

        PlayerInitOptions initOptions = new PlayerInitOptions(partnerId, uiConf)
                .setAutoplay(autoplay)
                .setPreload(preload)
                .setServerUrl(serverUrl);

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
