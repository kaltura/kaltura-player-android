package com.kaltura.tvplayer.config.player;

public class UiConf {
    private static final String BASE_URL = "https://cdnapisec.kaltura.com/";
    private Integer uiConfId;
    private Integer partnerId;
    private String baseUrl;

    public UiConf(Integer uiConfId, Integer partnerId) {
        this.uiConfId = uiConfId;
        this.partnerId = partnerId;
        this.baseUrl = BASE_URL;
    }

    public UiConf(Integer uiconfId, Integer partnerId, String baseUrl) {
        this.uiConfId = uiconfId;
        this.partnerId = partnerId;
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Integer getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Integer partnerId) {
        this.partnerId = partnerId;
    }

    public Integer getUiConfId() {
        return uiConfId;
    }

    public void setUiConfId(Integer uiConfId) {
        this.uiConfId = uiConfId;
    }
}
