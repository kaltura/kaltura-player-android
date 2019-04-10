package com.kaltura.playerdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Utils;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.PlayerConfigManager;
import com.kaltura.tvplayer.PlayerInitOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.kaltura.playkit.providers.ott.PhoenixMediaProvider.HttpProtocol.Https;

public class OTTDemoActivity extends BaseDemoActivity {

    private static final PKLog log = PKLog.get("OTTDemoActivity");

    private TVItem currentItem;

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
        if (object.has("protocol")) {
            return new TVItem(object.get("name").getAsString(), object.get("assetId").getAsString(), new String[]{"804398"}, object.get("protocol").getAsString());
        } else {
            return new TVItem(object.get("name").getAsString(), object.get("assetId").getAsString(), new String[]{"804398"});
        }
    }

//    @Override
//    protected void loadPlayerConfig() {
//
//        if (uiConfId == null || uiConfPartnerId == null) {
//            return;
//        }
//        PlayerConfigManager.initialize(this);
//        PlayerConfigManager.retrieve(uiConfId, uiConfPartnerId, ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
//            @Override
//            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
//                Toast.makeText(OTTDemoActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
//                playerConfigUiConfJson = config;
//            }
//        });
//    }

    @Override
    protected void loadItem(DemoItem item) {
        this.currentItem = (TVItem) item;
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void playerActivityLoaded(PlayerActivity playerActivity) {



        PlayerInitOptions updatedInitOptions = new PlayerInitOptions(initOptions.partnerId, initOptions.uiConfId);
        updatedInitOptions.setLicenseRequestAdapter(initOptions.licenseRequestAdapter);
        updatedInitOptions.setContentRequestAdapter(initOptions.contentRequestAdapter);
        updatedInitOptions.setVrPlayerEnabled(initOptions.vrPlayerEnabled);
        updatedInitOptions.setAdAutoPlayOnResume(initOptions.adAutoPlayOnResume);
        updatedInitOptions.setSubtitleStyle(initOptions.setSubtitleStyle);
        updatedInitOptions.setLoadControlBuffers(initOptions.loadControlBuffers);
        updatedInitOptions.setAbrSettings(initOptions.abrSettings);
        updatedInitOptions.setAspectRatioResizeMode(initOptions.aspectRatioResizeMode);
        updatedInitOptions.setPreferredMediaFormat(initOptions.preferredMediaFormat != null ?initOptions.preferredMediaFormat.name() : null);
        updatedInitOptions.setAllowClearLead(initOptions.allowClearLead);
        updatedInitOptions.setAllowCrossProtocolEnabled(initOptions.allowCrossProtocolEnabled);
        updatedInitOptions.setAudioLanguage(initOptions.audioLanguage);
        updatedInitOptions.setTextLanguage(initOptions.textLanguage);
        updatedInitOptions.setSecureSurface(initOptions.secureSurface);
        updatedInitOptions.setKs(initOptions.ks);
        updatedInitOptions.setServerUrl(initOptions.serverUrl);
        updatedInitOptions.setAutoPlay(initOptions.autoplay);
        updatedInitOptions.setReferrer(initOptions.referrer);
        updatedInitOptions.setStartTime(initOptions.startTime);

        KalturaPlayer player = KalturaPlayer.createOTTPlayer(playerActivity, updatedInitOptions);

        OTTMediaOptions ottMediaOptions = new OTTMediaOptions().setAssetId(currentItem.id).setProtocol(currentItem.protocol);
        player.loadMedia(ottMediaOptions, (entry, error) -> log.d("onEntryLoadComplete; " + entry + "; " + error));
        player.setPlayerView(FrameLayout.LayoutParams.WRAP_CONTENT, 600);

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
        final String protocol;

        TVItem(String name, String id, String[] fileIds) {
            super(name, id);
            this.fileIds = fileIds;
            this.protocol = Https;
        }

        TVItem(String name, String id, String[] fileIds, String protocol) {
            super(name, id);
            this.fileIds = fileIds;
            this.protocol = protocol;
        }
    }
}
