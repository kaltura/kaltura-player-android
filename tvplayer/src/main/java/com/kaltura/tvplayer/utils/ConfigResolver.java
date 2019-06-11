package com.kaltura.tvplayer.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.playkit.PKLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ConfigResolver {
    private static PKLog log = PKLog.get("ConfigResolver");

    private final TokenResolver tokenResolver;

    public ConfigResolver(TokenResolver tokenResolver) {
        this.tokenResolver = tokenResolver;
    }

    public <T> T resolve(T config) {

        if (config instanceof JsonObject) {
            //noinspection unchecked -- the compiler doesn't know that T==JsonObject
            return (T) resolveJsonObject(((JsonObject) config).deepCopy());
        }

        try {
            // We know for sure that the return type is T because of how resolveImp() works.
            //noinspection unchecked
            return (T) resolveObject(config);
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

    private JsonObject resolveJsonObject(JsonObject config) {

        // Resolve by converting to string and back
        final String resolved = tokenResolver.resolve(config.toString());
        return (JsonObject) new JsonParser().parse(resolved);
    }

    private Object resolveObject(Object config) throws SecurityException, InstantiationException, IllegalAccessException {

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
                field.set(out, resolveString((String) value));
            } else if (value != null) {
                field.set(out, value);
            }

            // TODO: 2019-06-06 Recurse into Lists, arrays, Maps?
        }

        return out;
    }

    private String resolveString(String string) {
        return tokenResolver.resolve(string);
    }
}
