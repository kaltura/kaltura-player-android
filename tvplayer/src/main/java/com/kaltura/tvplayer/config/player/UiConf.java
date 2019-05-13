package com.kaltura.tvplayer.config.player;

public class UiConf {
    private static final String BASE_URL = "https://cdnapisec.kaltura.com/";
    public Integer uiConfId;
    public Integer partnerId;
    public String baseUrl;

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

    public Integer getPartnerId() {
        return partnerId;
    }

    public Integer getUiConfId() {
        return uiConfId;
    }
}
