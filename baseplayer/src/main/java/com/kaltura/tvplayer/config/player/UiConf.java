package com.kaltura.tvplayer.config.player;

/**
 * Created by gilad.nadav on 1/28/18.
 */

public class UiConf {
    private static final String BASE_URL = "https://cdnapisec.kaltura.com/";
    private Integer id;
    private Integer partnerId;
    private String baseUrl;

    public UiConf(Integer id, Integer partnerId) {
        this.id = id;
        this.partnerId = partnerId;
        this.baseUrl = BASE_URL;
    }

    public UiConf(Integer id, Integer partnerId, String baseUrl) {
        this.id = id;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
