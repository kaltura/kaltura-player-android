package com.kaltura.kalturaplayertestapp.converters;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kaltura.playkit.PKMediaFormat;

import java.io.Serializable;
import java.util.List;

public class IMAPluginConfig implements Serializable {
    private static final long serialVersionUID = -3412072109243280357L;

    public static final String AD_TAG_LANGUAGE     = "language";
    public static final String AD_TAG_URL          = "adTagURL";
    public static final String ENABLE_BG_PLAYBACK  = "enableBackgroundPlayback";
    public static final String AUTO_PLAY_AD_BREAK  = "autoPlayAdBreaks";
    public static final String AD_VIDEO_BITRATE    = "videoBitrate";
    public static final String VIDEO_MIME_TYPES    = "videoMimeTypes";
    public static final String AD_TAG_TIMES        = "tagsTimes";
    public static final String AD_ATTRIBUTION_UIELEMENT = "adAttribution";
    public static final String AD_COUNTDOWN_UIELEMENT  = "adCountDown";
    public static final String AD_ENABLE_DEBUG_MODE     = "enableDebugMode";

    private String language = "en";
    private String adTagUrl;
    private boolean enableBackgroundPlayback = true;
    private boolean autoPlayAdBreaks = false;
    private int videoBitrate;
    private boolean adAttribution;
    private boolean adCountDown;
    private boolean enableDebugMode;
    private List<String> videoMimeTypes;
    //private Map<Double,String> tagsTimes; // <AdTime,URL_to_execute>

    public String getLanguage() {
        return language;
    }

    public String getAdTagUrl() {
        return adTagUrl;
    }

    public boolean getDebugMode() {
        return enableDebugMode;
    }
    public boolean isEnableBackgroundPlayback() {
        return enableBackgroundPlayback;
    }

    public boolean isAutoPlayAdBreaks() {
        return autoPlayAdBreaks;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public boolean isAdAttribution() {
        return adAttribution;
    }

    public boolean isAdCountDown() {
        return adCountDown;
    }

    public List<String> getVideoMimeTypes() {
        return videoMimeTypes;
    }

//public Map<Double, String> getTagsTimes() {
    //    return tagsTimes;
    //}

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(AD_TAG_LANGUAGE, language);
        jsonObject.addProperty(AD_TAG_URL, adTagUrl);
        jsonObject.addProperty(ENABLE_BG_PLAYBACK, enableBackgroundPlayback);
        jsonObject.addProperty(AUTO_PLAY_AD_BREAK, autoPlayAdBreaks);
        jsonObject.addProperty(AD_VIDEO_BITRATE, videoBitrate);
        jsonObject.addProperty(AD_ATTRIBUTION_UIELEMENT, adAttribution);
        jsonObject.addProperty(AD_COUNTDOWN_UIELEMENT, adCountDown);
        jsonObject.addProperty(AD_ENABLE_DEBUG_MODE, enableDebugMode);

        Gson gson = new Gson();
        JsonArray jArray = new JsonArray();
        if (videoMimeTypes != null) {
            for (String mimeType : videoMimeTypes) {
                JsonPrimitive element = new JsonPrimitive(mimeType);
                jArray.add(element);
            }
        } else {
            JsonPrimitive element = new JsonPrimitive(PKMediaFormat.mp4.mimeType);
            jArray.add(element);
        }
        jsonObject.add(VIDEO_MIME_TYPES, jArray);

//        String tagsTimesJsonString = gson.toJson(tagsTimes);
//        if (tagsTimesJsonString != null && !"null".equals(tagsTimesJsonString)&& !tagsTimesJsonString.isEmpty()) {
//            JsonParser parser = new JsonParser();
//            JsonObject tagsTimesJsonObject = parser.parse(tagsTimesJsonString).getAsJsonObject();
//            jsonObject.add(AD_TAG_TIMES, tagsTimesJsonObject);
//        } else {
//            jsonObject.add(AD_TAG_TIMES, new JsonObject());
//        }

        return jsonObject;
    }
}
