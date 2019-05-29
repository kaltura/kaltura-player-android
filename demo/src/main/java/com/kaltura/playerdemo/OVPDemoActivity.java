package com.kaltura.playerdemo;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.JsonObject;

import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Utils;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.OVPMediaOptions;
import com.kaltura.tvplayer.PlayerInitOptions;
import com.kaltura.tvplayer.config.player.UiConf;

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

        final JsonObject json = GsonParser.toJson(jsonString).getAsJsonObject();

        parseCommonOptions(json);
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

//    @Override
//    protected void loadPlayerConfig() {
//
//        if (uiConfId == null) {
//            return;
//        }
//
//        if (uiConfPartnerId == null) {
//            uiConfPartnerId = partnerId();
//        }
//        PlayerConfigManager.initialize(this);
//        PlayerConfigManager.retrieve(uiConfId, uiConfPartnerId, ks, uiConfServerUrl, new PlayerConfigManager.OnPlayerConfigLoaded() {
//            @Override
//            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
//                Toast.makeText(OVPDemoActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
//                playerConfigUiConfJson = config;
//            }
//        });
//    }

    @Override
    protected void loadItem(DemoItem item) {
        this.currentItem = item;
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void playerActivityLoaded(PlayerActivity playerActivity) {
        PlayerInitOptions updatedInitOptions = new PlayerInitOptions(initOptions.partnerId, new UiConf(Integer.valueOf(initOptions.uiConfId), Integer.valueOf(initOptions.uiConfPartnerId)));
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
        updatedInitOptions.setSecureSurface(initOptions.secureSurface);
        updatedInitOptions.setKs(initOptions.ks);
        updatedInitOptions.setServerUrl(initOptions.serverUrl);
        updatedInitOptions.setAutoPlay(initOptions.autoplay);
        updatedInitOptions.setReferrer(initOptions.referrer);
        if (initOptions.audioLanguage != null && initOptions.audioLanguageMode != null) {
            updatedInitOptions.setAudioLanguage(initOptions.audioLanguage, initOptions.audioLanguageMode);
        }
        if (initOptions.textLanguage != null && initOptions.textLanguageMode != null) {
            updatedInitOptions.setTextLanguage(initOptions.textLanguage, initOptions.textLanguageMode);
        }

        KalturaPlayer player = KalturaPlayer.createOVPPlayer(playerActivity, updatedInitOptions);
       OVPMediaOptions ovpMediaOptions = new OVPMediaOptions();
        ovpMediaOptions.entryId = currentItem.id;
        player.loadMedia(ovpMediaOptions, (entry, error) -> log.d("onEntryLoadComplete; " + entry + "; " + error));
        player.setPlayerView(FrameLayout.LayoutParams.WRAP_CONTENT, 600);

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
