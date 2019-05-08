package com.kaltura.tvplayer.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kaltura.playkit.PKLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigResolver {
    private static PKLog log = PKLog.get("ConfigResolver");

    public static <T> T resolve(T config, JsonObject defaults) {
        try {
            // We know for sure that the return type is T because of how resolveImp() works.
            //noinspection unchecked
            return (T) resolveImp(config, defaults);
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (SecurityException e) {
            return null;
        }

    }

    private static JsonObject resolveJsonConfig(JsonObject appConfig, JsonObject defaults) {
        if (defaults == null && appConfig != null) {
            return appConfig;
        } else if (defaults != null && appConfig == null) {
            return defaults;
        } else if (defaults == null) {
            return null;
        }

        return resolveJsonConfigImp(appConfig.deepCopy(), defaults);
    }

    private static JsonObject resolveJsonConfigImp(JsonObject appConfig, JsonObject defaults) {

        // If we're here, both defaults and appConfig are not null.

        for (String key: defaults.keySet()) {
            log.d("key = " + key);

            JsonElement sourceValue = defaults.get(key);
            if (sourceValue.isJsonNull()) {
                sourceValue = null;
            }
            if (!appConfig.has(key)) {
                // new value for "key":
                if (sourceValue instanceof JsonArray || sourceValue instanceof JsonPrimitive) {
                    appConfig.add(key, sourceValue);
                } else {
                    appConfig.add(key, null);
                }
            } else {
                // existing value for "key" - recursively deep merge:
                if (sourceValue instanceof JsonObject) {
                    JsonObject valueJson = (JsonObject) sourceValue;
                    resolveJsonConfigImp(appConfig.get(key).getAsJsonObject(), valueJson);
                } else {
                    Object targetValue = appConfig.get(key);
                    boolean isTargetValueIsNull = targetValue == null || appConfig.get(key).isJsonNull();
                    if (sourceValue instanceof JsonArray) {
                        if (!isTargetValueIsNull) {
                            appConfig.add(key, (JsonArray) targetValue);
                        }
                    } else if (sourceValue instanceof JsonPrimitive) {
                        if (appConfig.has(key)) {
                            if (!isTargetValueIsNull) {
                                JsonPrimitive targetPrimitiveVal = (JsonPrimitive) targetValue;
                                if (targetPrimitiveVal.isString()) {
                                    if ("".equals(targetPrimitiveVal.getAsString())) {
                                        appConfig.add(key, sourceValue);
                                    } else {
                                        appConfig.add(key, targetPrimitiveVal);
                                    }
                                } else {
                                    appConfig.add(key, targetPrimitiveVal);
                                }
                            } else {
                                appConfig.add(key, sourceValue);
                            }
                        } else {
                            appConfig.add(key, sourceValue);
                            log.d( "no key " + key + " in appConfig value = " + sourceValue);
                        }
                    }
                }
            }
        }
        return appConfig;
    }

    private static Object resolveImp(Object config, JsonObject uiconf) throws SecurityException, InstantiationException, IllegalAccessException {

        if (config instanceof JsonObject) {
            return resolveJsonConfig((JsonObject) config, uiconf);
        }

        final Class<?> configClass = config.getClass();

        final Object out = configClass.newInstance();

        for (Field field : configClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                // Don't look at static fields
                continue;
            }

            final String name = field.getName();

            field.setAccessible(true);
            final Object value = field.get(config);

            if (value != null) {
                // Just assign
                field.set(out, value);
                continue;
            }

            final JsonElement jsonElement = uiconf.get(name);

            if (jsonElement == null) {
                // Field will remain empty
                continue;
            }

            final Class<?> type = field.getType();

            if (type == String.class) {
                field.set(out, jsonElement.getAsString());
            } else if (type == Integer.class) {
                field.set(out, jsonElement.getAsInt());
            } else if (type == Long.class) {
                field.set(out, jsonElement.getAsLong());
            } else if (type == Byte.class) {
                field.set(out, jsonElement.getAsByte());
            } else if (type == Short.class) {
                field.set(out, jsonElement.getAsShort());
            } else if (type == Boolean.class) {
                field.set(out, jsonElement.getAsBoolean());
            } else if (type == Double.class) {
                field.set(out, jsonElement.getAsDouble());
            } else if (type == Float.class) {
                field.set(out, jsonElement.getAsFloat());
            } else if (type.isAssignableFrom(ArrayList.class)) {
                copyList(field, out, jsonElement.getAsJsonArray());
            } else if (type.isArray()) {
                copyArray(field, type, out, jsonElement.getAsJsonArray());
            } else {
                throw new IllegalArgumentException("Unknown type: " + type);
            }
        }

        return out;
    }

    private static void copyArray(Field field, Class<?> type, Object out, JsonArray jsonArray) throws IllegalAccessException {
        if (type == String[].class) {
            field.set(out, toStringArray(jsonArray));
        } else if (type == int[].class) {
            field.set(out, toIntArray(jsonArray));
        } else if (type == long[].class) {
            field.set(out, toLongArray(jsonArray));
        } else if (type == float[].class) {
            field.set(out, toFloatArray(jsonArray));
        } else if (type == double[].class) {
            field.set(out, toDoubleArray(jsonArray));
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static void copyList(Field field, Object out, JsonArray jsonArray) throws IllegalAccessException {

        final Type typeArgument = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

        if (typeArgument == String.class) {
            field.set(out, Arrays.asList(toStringArray(jsonArray)));
        } else if (typeArgument == Integer.class) {
            field.set(out, toIntList(jsonArray));
        } else if (typeArgument == Long.class) {
            field.set(out, toLongList(jsonArray));
        } else if (typeArgument == Float.class) {
            field.set(out, toFloatList(jsonArray));
        } else if (typeArgument == Double.class) {
            field.set(out, toDoubleList(jsonArray));
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static String[] toStringArray(JsonArray ja) {
        String[] out = new String[ja.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ja.get(i).getAsString();
        }
        return out;
    }

    private static int[] toIntArray(JsonArray jsonArray) {
        int[] out = new int[jsonArray.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = jsonArray.get(i).getAsInt();
        }
        return out;
    }

    private static List<Integer> toIntList(JsonArray jsonArray) {
        List<Integer> out = new ArrayList<>(jsonArray.size());
        for (JsonElement element : jsonArray) {
            out.add(element.getAsInt());
        }
        return out;
    }

    private static long[] toLongArray(JsonArray jsonArray) {
        long[] out = new long[jsonArray.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = jsonArray.get(i).getAsLong();
        }
        return out;
    }

    private static List<Long> toLongList(JsonArray jsonArray) {
        List<Long> out = new ArrayList<>(jsonArray.size());
        for (JsonElement element : jsonArray) {
            out.add(element.getAsLong());
        }
        return out;
    }

    private static float[] toFloatArray(JsonArray jsonArray) {
        float[] out = new float[jsonArray.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = jsonArray.get(i).getAsFloat();
        }
        return out;
    }

    private static List<Float> toFloatList(JsonArray jsonArray) {
        List<Float> out = new ArrayList<>(jsonArray.size());
        for (JsonElement element : jsonArray) {
            out.add(element.getAsFloat());
        }
        return out;
    }

    private static double[] toDoubleArray(JsonArray jsonArray) {
        double[] out = new double[jsonArray.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = jsonArray.get(i).getAsDouble();
        }
        return out;
    }

    private static List<Double> toDoubleList(JsonArray jsonArray) {
        List<Double> out = new ArrayList<>(jsonArray.size());
        for (JsonElement element : jsonArray) {
            out.add(element.getAsDouble());
        }
        return out;
    }
}
