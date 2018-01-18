package com.kaltura.playerdemo;

import android.content.Context;
import android.view.View;

import com.google.gson.JsonObject;
import com.kaltura.playkit.MessageBus;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.TokenResolver;
import com.kaltura.playkit.utils.GsonReader;

public class DemoPlugin extends PKPlugin {
    
    private static final PKLog log = PKLog.get("DemoPlugin");
    private TokenResolver resolver;

    private String string;
    private Integer i1;
    private Integer i2;
    private Integer i3;
    private View nonPrimitive;
    
    public static Factory factory = new Factory() {
        @Override
        public String getName() {
            return "Demo";
        }

        @Override
        public String getVersion() {
            return "1.0.0";
        }

        @Override
        public PKPlugin newInstance() {
            return new DemoPlugin();
        }

        @Override
        public void warmUp(Context context) {}

        @Override
        public Object mergeConfig(Object original, JsonObject additions) {
            Config o = original != null ? (Config) original : new Config(null);
            Config merged = new Config(o.nonPrimitive);

            GsonReader reader = new GsonReader(additions);

            merged.i1 = o.i1 != null ? o.i1 : reader.getInteger("i1");

            merged.i2 = o.i2 != null ? o.i2 : reader.getInteger("i2");

            merged.i3 = o.i3 != null ? o.i3 : reader.getInteger("i3");

            merged.string = o.string != null ? o.string : reader.getString("string");

            return merged;
        }
    };

    @Override
    protected void onLoad(Context context, Player player, Object config, MessageBus messageBus, TokenResolver tokenResolver) {
        this.resolver = tokenResolver;
        Config cfg = null;
        if (config instanceof Config) {
            cfg = (Config) config;
        } else if (config instanceof JsonObject) {
            cfg = (Config) factory.mergeConfig(new Config(null), (JsonObject) config);
        }
        
        if (cfg != null) {
            this.i1 = cfg.i1;
            this.i2 = cfg.i2;
            this.i3 = cfg.i3;
            this.nonPrimitive = cfg.nonPrimitive;
            this.string = cfg.string;
        }
        
        
        log.d("onLoad. Original string: " + string);
        log.d("onLoad. Resolved string: " + resolver.resolve(string));
    }

    @Override
    protected void onUpdateMedia(PKMediaConfig mediaConfig) {
        log.d("onUpdateMedia. Resolved string: " + resolver.resolve(string));
    }

    @Override
    protected void onUpdateConfig(Object config) {
        log.d("onUpdateConfig. Resolved string: " + resolver.resolve(string));
    }

    @Override
    protected void onApplicationPaused() {

    }

    @Override
    protected void onApplicationResumed() {

    }

    @Override
    protected void onDestroy() {

    }
    
    public static class Config {
        public String string;
        public Integer i1;
        public Integer i2;
        public Integer i3;
        public View nonPrimitive;

        public Config(View nonPrimitive) {
            this.nonPrimitive = nonPrimitive;
        }
    }
}
