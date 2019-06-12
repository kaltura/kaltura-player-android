package com.kaltura.tvplayer;

import android.content.Context;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPlugin;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

enum KnownPlugin {

    // Kaltura OVP Analytics
    kava("com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin", KavaAnalyticsPlugin.factory),

    // Kaltura Phoenix MediaHit/MediaMark
    phoenixBookmarks("com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin", KavaAnalyticsPlugin.factory),

    // IMA
    ima("com.kaltura.playkit.plugins.ima.IMAPlugin"),
    imadai("com.kaltura.playkit.plugins.imadai.IMADAIPlugin"),
    // Youbora
    youbora("com.kaltura.playkit.plugins.youbora.YouboraPlugin");


    private static final PKLog log = PKLog.get("KnownPlugin");

    public final String className;
    public final PKPlugin.Factory factory;

    KnownPlugin(String className, PKPlugin.Factory factory) {
        this.className = className;
        this.factory = factory;
    }

    KnownPlugin(String className) {
        this(className, null);
    }

    protected static void registerPluginByName(Context context, String pluginClassName) {
        try {
            Class pluginClass = Class.forName(pluginClassName);
            final Field factoryField = pluginClass.getField("factory");
            if (!Modifier.isStatic(factoryField.getModifiers())) {
                log.e("Plugin factory " + pluginClassName + ".factory is not static");
                return;
            }
            final PKPlugin.Factory factory = (PKPlugin.Factory) factoryField.get(null);
            if (factory == null) {
                log.e("Plugin factory " + pluginClassName + ".factory is null");
                return;
            }
            PlayKitManager.registerPlugins(context, factory);

        } catch (ClassNotFoundException e) {
            // This is ok and very common
            log.v("Plugin class " + pluginClassName + " not found");
        } catch (NoSuchFieldException e) {
            log.e("Plugin factory " + pluginClassName + ".factory not found");
        } catch (IllegalAccessException e) {
            log.e("Plugin factory " + pluginClassName + ".factory is not public");
        } catch (ClassCastException e) {
            log.e("Plugin factory " + pluginClassName + ".factory is not a PKPlugin.Factory");
        } catch (RuntimeException e) {
            log.e("Something bad", e);
        }
    }

    void register(Context context) {
        if (factory != null) {
            PlayKitManager.registerPlugins(context, factory);
        } else {
            registerPluginByName(context, className);
        }
    }

    static void registerAll(Context context) {
        for (KnownPlugin plugin : values()) {
            plugin.register(context);
        }
    }
}
