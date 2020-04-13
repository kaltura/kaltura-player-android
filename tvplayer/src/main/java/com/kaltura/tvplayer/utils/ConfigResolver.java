package com.kaltura.tvplayer.utils;
import android.os.Bundle;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.playkit.PKLog;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ConfigResolver {
    private static PKLog log = PKLog.get("ConfigResolver");
    public static <T> T resolve(T config, TokenResolver tokenResolver) {
        if (config instanceof JsonObject) {
            //noinspection unchecked -- the compiler doesn't know that T==JsonObject
            return (T) resolveJsonObject((JsonObject) config, tokenResolver);
        }
        if (config instanceof Bundle) {
            //noinspection unchecked -- the compiler doesn't know that T==Bundle
            return (T) resolveBundle((Bundle)config, tokenResolver);
        }
        try {
            // We know for sure that the return type is T because of how resolveImp() works.
            //noinspection unchecked
            return (T) resolveObject(config, tokenResolver);
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }
    private static JsonObject resolveJsonObject(JsonObject config, TokenResolver tokenResolver) {
        // Resolve by converting to string and back
        final String resolved = resolveString(config.toString(), tokenResolver);
        return (JsonObject) new JsonParser().parse(resolved);
    }
    private static Bundle resolveBundle(Bundle config, TokenResolver tokenResolver) {
        Bundle resolvedBundle = new Bundle();
        resolvedBundle.putAll(config); // add all values and then update the relevant ones only
        for (String key : config.keySet()) {
            Object value = config.get(key);
            if (value instanceof String) {
                final String resolved = resolveString((String) value, tokenResolver);
                resolvedBundle.putString(key, resolved);
            } else if (value instanceof Bundle) {
                Bundle resolvedInnerBundle = resolveBundle((Bundle) value, tokenResolver);
                resolvedBundle.putBundle(key, resolvedInnerBundle);
            } 
        }
        return resolvedBundle;
    }

    private static Object resolveObject(Object config, TokenResolver tokenResolver) throws SecurityException, InstantiationException, IllegalAccessException {
        final Class<?> configClass = config.getClass();
        Object out = configClass.newInstance();
        for (Field field : configClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                // Don't look at static fields
                continue;
            }
            field.setAccessible(true);
            final Object value = field.get(config);
            if (value instanceof String) {
                field.set(out, resolveString((String) value, tokenResolver));
            } else if (value != null) {
                field.set(out, value);
            }
            // TODO: 2019-06-06 Recurse into Lists, arrays, Maps?
        }
        return out;
    }
    private static String resolveString(String string, TokenResolver tokenResolver) {
        return tokenResolver.resolve(string);
    }
}
