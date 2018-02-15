package com.kaltura.tvplayer.config.youbora;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class UiConfYouboraConfig {

    private String accountCode;

    private String username;

    private Boolean haltOnError;

    private Boolean enableAnalytics;

    private Boolean enableSmartAds;

    private Media media;

    private Ads ads;

    private Properties properties;

    private ExtraParams extraParams;

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getHaltOnError() {
        return haltOnError;
    }

    public void setHaltOnError(Boolean haltOnError) {
        this.haltOnError = haltOnError;
    }

    public Boolean getEnableAnalytics() {
        return enableAnalytics;
    }

    public void setEnableAnalytics(Boolean enableAnalytics) {
        this.enableAnalytics = enableAnalytics;
    }

    public Boolean getEnableSmartAds() {
        return enableSmartAds;
    }

    public void setEnableSmartAds(Boolean enableSmartAds) {
        this.enableSmartAds = enableSmartAds;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public Ads getAds() {
        return ads;
    }

    public void setAds(Ads ads) {
        this.ads = ads;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public ExtraParams getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(ExtraParams extraParams) {
        this.extraParams = extraParams;
    }

    public JsonObject toJson() {
        JsonPrimitive accountCode = new JsonPrimitive(getAccountCode() != null ? getAccountCode() : "");
        JsonPrimitive username = new JsonPrimitive(getUsername() != null ? getUsername() : "");
        JsonPrimitive haltOnError = new JsonPrimitive(getHaltOnError() != null ? getHaltOnError() : Boolean.TRUE);
        JsonPrimitive enableAnalytics = new JsonPrimitive(getEnableAnalytics() != null ? getEnableAnalytics() : Boolean.TRUE);
        JsonPrimitive enableSmartAds = new JsonPrimitive(getEnableSmartAds() != null ? getEnableSmartAds() : Boolean.FALSE);

        JsonObject mediaEntry = getMediaJsonObject();
        JsonObject adsEntry = new JsonObject();
        adsEntry.addProperty("campaign", (getAds() != null && getAds().getCampaign() != null) ? getAds().getCampaign() : "");
        JsonObject propertiesEntry = getPropertiesJsonObject();
        JsonObject extraParamEntry = getExtraParamJsonObject();
        JsonObject youboraConfig = getYouboraConfigJsonObject(accountCode, username, haltOnError, enableAnalytics, enableSmartAds, mediaEntry, adsEntry, propertiesEntry, extraParamEntry);
        return youboraConfig;
    }

    @NonNull
    private JsonObject getMediaJsonObject() {
        JsonObject mediaEntry = new JsonObject();
        if (getMedia() == null) {
            return mediaEntry;
        }

        Media media = getMedia();
        mediaEntry.addProperty("isLive", media.getIsLive() != null ? media.getIsLive() : Boolean.FALSE);
        mediaEntry.addProperty("title",  media.getTitle() != null ? media.getTitle() : "");
        if (media.getDuration() != null) {
            mediaEntry.addProperty("duration", media.getDuration());
        }
        return mediaEntry;
    }

    @NonNull
    private JsonObject getYouboraConfigJsonObject(JsonPrimitive accountCode, JsonPrimitive username, JsonPrimitive haltOnError, JsonPrimitive enableAnalytics, JsonPrimitive enableSmartAds, JsonObject mediaEntry, JsonObject adsEntry, JsonObject propertiesEntry, JsonObject extraParamEntry) {
        JsonObject youboraConfig = new JsonObject();
        youboraConfig.add("accountCode", accountCode);
        youboraConfig.add("username", username);
        youboraConfig.add("haltOnError", haltOnError);
        youboraConfig.add("enableAnalytics", enableAnalytics);
        youboraConfig.add("enableSmartAds", enableSmartAds);
        youboraConfig.add("media", mediaEntry);
        youboraConfig.add("ads", adsEntry);
        youboraConfig.add("properties", propertiesEntry);
        youboraConfig.add("extraParams", extraParamEntry);
        return youboraConfig;
    }

    @NonNull
    private JsonObject getPropertiesJsonObject() {
        JsonObject propertiesEntry = new JsonObject();
        if (getProperties() == null) {
            return propertiesEntry;
        }

        Properties prop = getProperties();
        propertiesEntry.addProperty("genre", (prop.getGenre() != null) ? prop.getGenre() : "");
        propertiesEntry.addProperty("type", (prop.getType() != null) ? prop.getType() : "");
        propertiesEntry.addProperty("transaction_type", (prop.getTransactionType() != null) ? prop.getTransactionType() : "");
        propertiesEntry.addProperty("year", (prop.getYear() != null) ? prop.getYear() : "");
        propertiesEntry.addProperty("cast", (prop.getCast() != null) ? prop.getCast() : "");
        propertiesEntry.addProperty("director", (prop.getDirector() != null) ? prop.getDirector() : "");
        propertiesEntry.addProperty("owner", (prop.getOwner() != null) ? prop.getOwner() : "");
        propertiesEntry.addProperty("parental", (prop.getParental() != null) ? prop.getParental() : "");
        propertiesEntry.addProperty("price", (prop.getPrice() != null) ? prop.getPrice() : "");
        propertiesEntry.addProperty("rating", (prop.getRating() != null) ? prop.getRating() : "");
        propertiesEntry.addProperty("audioType", (prop.getAudioType() != null) ? prop.getAudioType() : "");
        propertiesEntry.addProperty("audioChannels", (prop.getAudioChannels() != null) ? prop.getAudioChannels() : "");
        propertiesEntry.addProperty("device", (prop.getDevice() != null) ? prop.getDevice() : "");
        propertiesEntry.addProperty("quality", (prop.getQuality() != null) ? prop.getQuality() : "");
        return propertiesEntry;
    }

    @NonNull
    private JsonObject getExtraParamJsonObject() {
        JsonObject extraParamEntry = new JsonObject();
        if (getExtraParams() == null) {
            return extraParamEntry;
        }
        ExtraParams extraParams = getExtraParams();
        if (extraParams.getParam1() != null) {
            extraParamEntry.addProperty("param1", extraParams.getParam1());
        }
        if (extraParams.getParam2() != null) {
            extraParamEntry.addProperty("param2", extraParams.getParam2());
        }
        if (extraParams.getParam3() != null) {
            extraParamEntry.addProperty("param3", extraParams.getParam3());
        }
        if (extraParams.getParam4() != null) {
            extraParamEntry.addProperty("param4", extraParams.getParam4());
        }
        if (extraParams.getParam5() != null) {
            extraParamEntry.addProperty("param5", extraParams.getParam5());
        }
        if (extraParams.getParam6() != null) {
            extraParamEntry.addProperty("param6", extraParams.getParam6());
        }
        if (extraParams.getParam7() != null) {
            extraParamEntry.addProperty("param7", extraParams.getParam7());
        }
        if (extraParams.getParam8() != null) {
            extraParamEntry.addProperty("param8", extraParams.getParam8());
        }
        if (extraParams.getParam9() != null) {
            extraParamEntry.addProperty("param9", extraParams.getParam9());
        }
        if (extraParams.getParam10() != null) {
            extraParamEntry.addProperty("param10", extraParams.getParam10());
        }
        return extraParamEntry;
    }

    public void merge(UiConfYouboraConfig youboraConfigUiConf) {
        if (youboraConfigUiConf == null) {
            return;
        }

        if (TextUtils.isEmpty(accountCode)) {
            accountCode = youboraConfigUiConf.getAccountCode();
        }
        if (TextUtils.isEmpty(username)) {
            username =  youboraConfigUiConf.getUsername();
        }

        if (haltOnError == null) {
            haltOnError = youboraConfigUiConf.getHaltOnError();
        }

        if (enableAnalytics == null) {
            enableAnalytics = youboraConfigUiConf.getEnableAnalytics();
        }

        if (enableSmartAds == null) {
            enableSmartAds = youboraConfigUiConf.getEnableSmartAds();
        }
        if (media != null) {
            if (youboraConfigUiConf.getMedia() != null) {
                if (media.getIsLive() == null) {
                    media.setIsLive(youboraConfigUiConf.getMedia().getIsLive());
                }
                if (media.getTitle() == null) {
                    media.setTitle(youboraConfigUiConf.getMedia().getTitle());
                }
                if (media.getDuration() == null) {
                    media.setDuration(youboraConfigUiConf.getMedia().getDuration());
                }
            }
        } else {
            media = youboraConfigUiConf.getMedia();
        }

        if (ads != null) {
            if (ads.getCampaign() == null) {
                if (youboraConfigUiConf.getAds() != null) {
                    ads.setCampaign(youboraConfigUiConf.getAds().getCampaign());
                }
            }
        } else {
            ads = youboraConfigUiConf.getAds();
        }

        if (properties != null) {
            if (youboraConfigUiConf.getProperties() != null) {
                Properties propUiConf = youboraConfigUiConf.getProperties();
                if (TextUtils.isEmpty((properties.getGenre()))) {
                    properties.setGenre(propUiConf.getGenre());
                }
                if (TextUtils.isEmpty((properties.getType()))) {
                    properties.setType(propUiConf.getType());
                }
                if (TextUtils.isEmpty((properties.getTransactionType()))) {
                    properties.setTransactionType(propUiConf.getTransactionType());
                }
                if (TextUtils.isEmpty((properties.getAudioChannels()))) {
                    properties.setAudioChannels(propUiConf.getAudioChannels());
                }
                if (TextUtils.isEmpty((properties.getAudioType()))) {
                    properties.setAudioType(propUiConf.getAudioType());
                }
                if (TextUtils.isEmpty((properties.getCast()))) {
                    properties.setCast(propUiConf.getCast());
                }
                if (TextUtils.isEmpty((properties.getDevice()))) {
                    properties.setDevice(propUiConf.getDevice());
                }
                if (TextUtils.isEmpty((properties.getDirector()))) {
                    properties.setDirector(propUiConf.getDirector());
                }
                if (TextUtils.isEmpty((properties.getOwner()))) {
                    properties.setOwner(propUiConf.getOwner());
                }
                if (TextUtils.isEmpty((properties.getParental()))) {
                    properties.setParental(propUiConf.getParental());
                }
                if (TextUtils.isEmpty((properties.getYear()))) {
                    properties.setYear(propUiConf.getYear());
                }
                if (TextUtils.isEmpty((properties.getQuality()))) {
                    properties.setQuality(propUiConf.getQuality());
                }
                if (TextUtils.isEmpty((properties.getRating()))) {
                    properties.setRating(propUiConf.getRating());
                }

            }
        } else {
            properties = youboraConfigUiConf.getProperties();
        }

        if (extraParams != null) {
            if (youboraConfigUiConf.getExtraParams() != null) {
                ExtraParams extraParamsUiConf = youboraConfigUiConf.getExtraParams();
                if (TextUtils.isEmpty((extraParams.getParam1()))) {
                    extraParams.setParam1(extraParamsUiConf.getParam1());
                }
                if (TextUtils.isEmpty((extraParams.getParam1()))) {
                    extraParams.setParam1(extraParamsUiConf.getParam1());
                }
                if (TextUtils.isEmpty((extraParams.getParam2()))) {
                    extraParams.setParam2(extraParamsUiConf.getParam2());
                }
                if (TextUtils.isEmpty((extraParams.getParam3()))) {
                    extraParams.setParam3(extraParamsUiConf.getParam3());
                }
                if (TextUtils.isEmpty((extraParams.getParam4()))) {
                    extraParams.setParam4(extraParamsUiConf.getParam4());
                }
                if (TextUtils.isEmpty((extraParams.getParam5()))) {
                    extraParams.setParam5(extraParamsUiConf.getParam5());
                }
                if (TextUtils.isEmpty((extraParams.getParam6()))) {
                    extraParams.setParam6(extraParamsUiConf.getParam6());
                }
                if (TextUtils.isEmpty((extraParams.getParam7()))) {
                    extraParams.setParam7(extraParamsUiConf.getParam7());
                }
                if (TextUtils.isEmpty((extraParams.getParam8()))) {
                    extraParams.setParam8(extraParamsUiConf.getParam8());
                }
                if (TextUtils.isEmpty((extraParams.getParam1()))) {
                    extraParams.setParam9(extraParamsUiConf.getParam9());
                }
                if (TextUtils.isEmpty((extraParams.getParam10()))) {
                    extraParams.setParam10(extraParamsUiConf.getParam10());
                }
            }

        } else {
            extraParams = youboraConfigUiConf.getExtraParams();
        }

    }

}