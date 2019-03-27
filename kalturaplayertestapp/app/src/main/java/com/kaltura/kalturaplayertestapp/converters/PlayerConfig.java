package com.kaltura.kalturaplayertestapp.converters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.SubtitleStyleSettings;


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
    private Boolean allowCrossProtocolEnabled;
    private String  preferredFormat;
    private Boolean allowClearLead;
    private Boolean secureSurface;
    private Boolean adAutoPlayOnResume;
    private Boolean vrPlayerEnabled;
    private SubtitleStyleSettings setSubtitleStyle;
    private PKAspectRatioResizeMode aspectRatioResizeMode;
    private PKRequestParams.Adapter contentRequestAdapter;
    private PKRequestParams.Adapter licenseRequestAdapter;
    private LoadControlBuffers loadControlBuffers;
    private ABRSettings abrSettings;
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

    public Boolean getAllowCrossProtocolEnabled() {
        return allowCrossProtocolEnabled;
    }

    public void setAllowCrossProtocolEnabled(Boolean allowCrossProtocolEnabled) {
        this.allowCrossProtocolEnabled = allowCrossProtocolEnabled;
    }

    public Boolean getAllowClearLead() {
        return allowClearLead;
    }

    public void setAllowClearLead(Boolean allowClearLead) {
        this.allowClearLead = allowClearLead;
    }

    public Boolean getSecureSurface() {
        return secureSurface;
    }

    public void setSecureSurface(Boolean setSecureSurface) {
        this.secureSurface = setSecureSurface;
    }

    public Boolean getAdAutoPlayOnResume() {
        return adAutoPlayOnResume;
    }

    public void setAdAutoPlayOnResume(Boolean adAutoPlayOnResume) {
        this.adAutoPlayOnResume = adAutoPlayOnResume;
    }

    public Boolean getVrPlayerEnabled() {
        return vrPlayerEnabled;
    }

    public void setVrPlayerEnabled(Boolean vrPlayerEnabled) {
        this.vrPlayerEnabled = vrPlayerEnabled;
    }

    public SubtitleStyleSettings getSetSubtitleStyle() {
        return setSubtitleStyle;
    }

    public void setSetSubtitleStyle(SubtitleStyleSettings setSubtitleStyle) {
        this.setSubtitleStyle = setSubtitleStyle;
    }

    public PKAspectRatioResizeMode getAspectRatioResizeMode() {
        return aspectRatioResizeMode;
    }

    public void setAspectRatioResizeMode(PKAspectRatioResizeMode aspectRatioResizeMode) {
        this.aspectRatioResizeMode = aspectRatioResizeMode;
    }

    public PKRequestParams.Adapter getContentRequestAdapter() {
        return contentRequestAdapter;
    }

    public void setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter) {
        this.contentRequestAdapter = contentRequestAdapter;
    }

    public PKRequestParams.Adapter getLicenseRequestAdapter() {
        return licenseRequestAdapter;
    }

    public void setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        this.licenseRequestAdapter = licenseRequestAdapter;
    }

    public LoadControlBuffers getLoadControlBuffers() {
        return loadControlBuffers;
    }

    public void setLoadControlBuffers(LoadControlBuffers loadControlBuffers) {
        this.loadControlBuffers = loadControlBuffers;
    }

    public ABRSettings getAbrSettings() {
        return abrSettings;
    }

    public void setAbrSettings(ABRSettings abrSettings) {
        this.abrSettings = abrSettings;
    }
}
