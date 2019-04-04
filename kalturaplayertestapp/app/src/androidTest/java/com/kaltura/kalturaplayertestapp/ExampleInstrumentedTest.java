package com.kaltura.kalturaplayertestapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.plugins.ima.IMAConfig;
import com.kaltura.playkit.plugins.youbora.pluginconfig.YouboraConfig;
import com.kaltura.tvplayer.PlayerConfigManager;
import com.kaltura.tvplayer.utils.ConfigResolver;
import com.kaltura.tvplayer.utils.TokenResolver;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final PKLog log = PKLog.get("Test");

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.kaltura.kalturaplayertestapp", appContext.getPackageName());
    }

    public void configLoader() throws InterruptedException {
        Context appContext = InstrumentationRegistry.getTargetContext();
        PlayerConfigManager.initialize(appContext);
        CountDownLatch latch = new CountDownLatch(1);

        PlayerConfigManager.retrieve(43907621, 2215841, null, null, (id, uiConf, error, freshness) -> {
            log.d(uiConf.toString());
            latch.countDown();

        });
        latch.await(5, TimeUnit.SECONDS);
    }

    private View fakeView(Context context, String s) {
        return new View(context) {
            @Override
            public String toString() {
                return s;
            }
        };
    }

    @Test
    public void configResolver() {
        Context appContext = InstrumentationRegistry.getTargetContext();

        final ConfigResolver configResolver = new ConfigResolver(string -> string.replace("{{entryId}}", "FOO_BAR"));


        // This is what comes from PlayerConfigManager.retrieve():
        String uiConfString = "{\"disableUserCache\":true,\"player\":{\"plugins\":{" +
                "\"youbora\":{\"options\":{\"accountCode\":\"123456\"}}," +

                "\"ima\":{\"adTagUrl\":\"http://foo.bar/baz?id={{entryId}}\"," +
                "\"adsRenderingSettings\":{\"loadVideoTimeout\":5000},\"companions\":{" +
                "\"ads\":{\"Comp_300x250\":{\"width\":300,\"height\":250},\"Comp_728x90\":{" +
                "\"width\":728,\"height\":90}},\"sizeCriteria\":\"SELECT_EXACT_MATCH\"}}," +
                "\"kava\":{}" +
                "}," +

                "\"playback\":{\"textLanguage\":\"auto\",\"preload\":\"auto\",\"autoplay\":true,\"muted\":false}}," +
                "\"provider\":{\"env\":{}}," +
                "\"cast\":{\"receiverApplicationId\":\"112233\",\"advertising\":{}}}";

        final JsonObject uiConf = GsonParser.toJson(uiConfString).getAsJsonObject();
        final JsonObject pluginsJson = uiConf.get("player").getAsJsonObject().get("plugins").getAsJsonObject();
        final JsonObject imaJson = pluginsJson.get("ima").getAsJsonObject();
        final JsonObject kavaJson = pluginsJson.get("kava").getAsJsonObject();
        final JsonObject youboraJson = pluginsJson.get("youbora").getAsJsonObject();

        // Verify the parsed JSON before resolving
        assertEquals("http://foo.bar/baz?id={{entryId}}", imaJson.get("adTagUrl").getAsString());
        assertEquals("123456", youboraJson.get("options").getAsJsonObject().get("accountCode").getAsString());

        // IMA
        IMAConfig imaConfig = new IMAConfig();
        imaConfig.addControlsOverlay(new View(appContext));
        imaConfig.addControlsOverlay(new View(appContext));
        imaConfig.addControlsOverlay(new View(appContext));

        final IMAConfig resolvedImaConfig = configResolver.resolve(imaConfig, imaJson);

        assertEquals("http://foo.bar/baz?id=FOO_BAR", resolvedImaConfig.getAdTagUrl());
        assertSame(imaConfig.getControlsOverlayList(), resolvedImaConfig.getControlsOverlayList());


        // Youbora
        YouboraConfig youboraConfig = new YouboraConfig();
        youboraConfig.setUsername("tester1");

        final YouboraConfig resolvedYouboraConfig = configResolver.resolve(youboraConfig, youboraJson.get("options").getAsJsonObject());
        assertEquals(youboraConfig.getUsername(), resolvedYouboraConfig.getUsername());
        assertEquals("123456", resolvedYouboraConfig.getAccountCode());
    }
}
