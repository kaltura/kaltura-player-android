package com.kaltura.playerdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Utils;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.PlayerConfigManager;
import com.kaltura.tvplayer.PlayerInitOptions;
import com.kaltura.tvplayer.TVPlayerType;
import com.kaltura.tvplayer.config.PhoenixConfigurationsResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.kaltura.playkit.providers.ott.PhoenixMediaProvider.HttpProtocol.Https;

public class OTTDemoActivity extends BaseDemoActivity {

    private static final PKLog log = PKLog.get("OTTDemoActivity");
    private Gson gson = new Gson();
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

    @Override
    protected void loadItem(DemoItem item) {
        this.currentItem = (TVItem) item;
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void playerActivityLoaded(PlayerActivity playerActivity) {

        PlayerConfigManager.retrieve(this, TVPlayerType.ott, initOptions.partnerId, initOptions.serverUrl, (partnerId, config, error, freshness) -> {
            PlayerInitOptions updatedInitOptions = new PlayerInitOptions(initOptions.partnerId);

            PhoenixConfigurationsResponse phoenixConfigurationsResponse = gson.fromJson(config, PhoenixConfigurationsResponse.class);
            if (phoenixConfigurationsResponse != null) {
                updatedInitOptions.setTVPlayerParams(phoenixConfigurationsResponse.params);
            }

            updatedInitOptions.setLicenseRequestAdapter(initOptions.licenseRequestAdapter);
            updatedInitOptions.setContentRequestAdapter(initOptions.contentRequestAdapter);
            updatedInitOptions.setVrPlayerEnabled(initOptions.vrPlayerEnabled);
            updatedInitOptions.setVRSettings(initOptions.vrSettings);
            updatedInitOptions.setAdAutoPlayOnResume(initOptions.adAutoPlayOnResume);
            updatedInitOptions.setSubtitleStyle(initOptions.setSubtitleStyle);
            updatedInitOptions.setLoadControlBuffers(initOptions.loadControlBuffers);
            updatedInitOptions.setAbrSettings(initOptions.abrSettings);
            updatedInitOptions.setAspectRatioResizeMode(initOptions.aspectRatioResizeMode);
            updatedInitOptions.setPreferredMediaFormat(initOptions.preferredMediaFormat != null ? initOptions.preferredMediaFormat.name() : null);
            updatedInitOptions.setAllowClearLead(initOptions.allowClearLead);
            updatedInitOptions.setAllowCrossProtocolEnabled(initOptions.allowCrossProtocolEnabled);
            updatedInitOptions.setSecureSurface(initOptions.secureSurface);
            updatedInitOptions.setKs(initOptions.ks);
            updatedInitOptions.setServerUrl(initOptions.serverUrl);
            updatedInitOptions.setAutoPlay(initOptions.autoplay);
            updatedInitOptions.setReferrer(initOptions.referrer);
            updatedInitOptions.forceSinglePlayerEngine(initOptions.forceSinglePlayerEngine);
            if (initOptions.audioLanguage != null && initOptions.audioLanguageMode != null) {
                updatedInitOptions.setAudioLanguage(initOptions.audioLanguage, initOptions.audioLanguageMode);
            }
            if (initOptions.textLanguage != null && initOptions.textLanguageMode != null) {
                updatedInitOptions.setTextLanguage(initOptions.textLanguage, initOptions.textLanguageMode);
            }

            KalturaPlayer player = KalturaPlayer.createOTTPlayer(playerActivity, updatedInitOptions);

            OTTMediaOptions ottMediaOptions = new OTTMediaOptions();
            ottMediaOptions.assetId = currentItem.id;
            ottMediaOptions.protocol = currentItem.protocol;
            player.loadMedia(ottMediaOptions, (entry, loadError) -> log.d("onEntryLoadComplete; " + entry + "; " + loadError));
            player.setPlayerView(FrameLayout.LayoutParams.WRAP_CONTENT, 600);

            playerActivity.setPlayer(player);
        });
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
