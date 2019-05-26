
package com.kaltura.kalturaplayertestapp.models.ima;

import com.google.ads.interactivemedia.v3.api.StreamRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kaltura.playkit.ads.AdTagType;

public class UiConfFormatIMADAIConfig {
    public static final int DEFAULT_AD_LOAD_TIMEOUT = 5;
    public static final int DEFAULT_CUE_POINTS_CHANGED_DELAY = 2000;
    public static final int DEFAULT_AD_LOAD_COUNT_DOWN_TICK = 250;


    public static final String AD_ASSET_TITLE         = "assetTitle";
    public static final String AD_ASSET_KEY           = "assetKey";
    public static final String AD_API_KEY             = "apiKey";
    public static final String AD_CONTENT_SOURCE_ID   = "contentSourceId";
    public static final String AD_VIDEOE_ID           = "videoId";
    public static final String AD_STREAM_FORMAT       = "streamFormat";
    public static final String AD_LICENSE_URL       = "licenseUrl";

    public static final String AD_TAG_LANGUAGE        = "language";
    public static final String AD_VIDEO_BITRATE       = "videoBitrate";
    public static final String AD_VIDEO_MIME_TYPES      = "videoMimeTypes";
    public static final String AD_ATTRIBUTION_UIELEMENT = "adAttribution";
    public static final String AD_COUNTDOWN_UIELEMENT   = "adCountDown";
    public static final String AD_LOAD_TIMEOUT          = "adLoadTimeOut";
    public static final String AD_ENABLE_DEBUG_MODE     = "enableDebugMode";

    private String assetTitle;
    private String assetKey;  // null for VOD
    private String apiKey;    // seems to be always null in demos
    private String contentSourceId; // null for Live
    private String videoId;         // null for Live
    private StreamRequest.StreamFormat streamFormat;
    private String licenseUrl;

    private AdsRenderingSettings adsRenderingSettings;
    private SdkSettings sdkSettings;

    public String getAssetTitle() {
        return assetTitle;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getContentSourceId() {
        return contentSourceId;
    }

    public String getVideoId() {
        return videoId;
    }

    public StreamRequest.StreamFormat getStreamFormat() {
        return streamFormat;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public AdsRenderingSettings getAdsRenderingSettings() {
        if (adsRenderingSettings == null) {
            adsRenderingSettings = new AdsRenderingSettings();
        }
        return adsRenderingSettings;
    }

    public SdkSettings getSdkSettings() {
        if (sdkSettings == null) {
            sdkSettings = new SdkSettings();
        }
        return sdkSettings;
    }

    public JsonObject toJson() { // to Json will return format like IMADAIConfig
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty(AD_ASSET_TITLE, assetTitle);
        jsonObject.addProperty(AD_ASSET_KEY, assetKey);
        jsonObject.addProperty(AD_API_KEY, apiKey);
        jsonObject.addProperty(AD_CONTENT_SOURCE_ID, contentSourceId);
        jsonObject.addProperty(AD_VIDEOE_ID, videoId);
        jsonObject.addProperty(AD_STREAM_FORMAT, streamFormat.name());
        jsonObject.addProperty(AD_LICENSE_URL, licenseUrl);

        jsonObject.addProperty(AD_TAG_LANGUAGE, getSdkSettings().getLanguage());
        jsonObject.addProperty(AD_VIDEO_BITRATE, getAdsRenderingSettings().getBitrate());
        jsonObject.addProperty(AD_ATTRIBUTION_UIELEMENT, getAdsRenderingSettings().getUiElements().isAdAttribution());
        jsonObject.addProperty(AD_COUNTDOWN_UIELEMENT, getAdsRenderingSettings().getUiElements().isAdCountDown());
        jsonObject.addProperty(AD_LOAD_TIMEOUT, getAdsRenderingSettings().getLoadVideoTimeout());
        jsonObject.addProperty(AD_ENABLE_DEBUG_MODE, getSdkSettings().isDebugMode());

        Gson gson = new Gson();
        JsonArray jArray = new JsonArray();
        if (adsRenderingSettings.getMimeTypes() != null) {
            for (String mimeType : adsRenderingSettings.getMimeTypes()) {
                JsonPrimitive element = new JsonPrimitive(mimeType);
                jArray.add(element);
            }
        }
        jsonObject.add(AD_VIDEO_MIME_TYPES, jArray);
        return jsonObject;
    }
}