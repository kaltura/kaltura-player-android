package com.kaltura.kalturaplayertestapp.converters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


import java.util.List;

/**
 * Created by gilad.nadav on 1/24/18.
 */

public class PlayerConfig {
    private String playerType;
    private String baseUrl;
    private String partnerId;
    private String ks;
    private UiConf uiConf;
    private int startPosition;
    private Boolean autoPlay;
    private Boolean preload;
    private String  preferredFormat;
    private String referrer;
    private List<Media> mediaList;
    private TrackSelection trackSelection;
    private JsonArray plugins;
    private JsonObject playerConfig;

    public PlayerConfig() {}

    public String getPlayerType() {
        return playerType;
    }

    public void setPlayerType(String playerType) {
        this.playerType = playerType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getKs() {
        return ks;
    }

    public void setKs(String ks) {
        this.ks = ks;
    }

    public UiConf getUiConf() {
        return uiConf;
    }

    public void setUiConf(UiConf uiConf) {
        this.uiConf = uiConf;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public Boolean getAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(Boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public Boolean getPreload() {
        return preload;
    }

    public void setPreload(Boolean preload) {
        this.preload = preload;
    }

    public String getPreferredFormat() {
        return preferredFormat;
    }

    public void setPreferredFormat(String preferredFormat) {
        this.preferredFormat = preferredFormat;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public List<Media> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<Media> mediaList) {
        this.mediaList = mediaList;
    }

    public JsonArray getPluginConfigs() {
        return plugins;
    }

    public void setPluginConfigs(JsonArray plugins) {
        this.plugins = plugins;
    }

    public TrackSelection getTrackSelection() {
        return trackSelection;
    }

    public void setTrackSelection(TrackSelection trackSelection) {
        this.trackSelection = trackSelection;
    }

    public JsonObject getPlayerConfig() {
        return playerConfig;
    }

    public void setPlayerConfig(JsonObject uiConfPlayerConfig) {
             playerConfig = uiConfPlayerConfig;
    }
}
