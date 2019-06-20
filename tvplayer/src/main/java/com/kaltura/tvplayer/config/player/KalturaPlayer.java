package com.kaltura.tvplayer.config.player;

public class KalturaPlayer {
    private String domainUrl;
    private String uiConfId;
    private String partnerId;

    public KalturaPlayer(String domainUrl, String uiConfId, String partnerId) {
        this.domainUrl = domainUrl;
        this.uiConfId = uiConfId;
        this.partnerId = partnerId;
    }

    public String getDomainUrl() {
        return domainUrl;
    }

    public String getUiConfId() {
        return uiConfId;
    }

    public String getPartnerId() {
        return partnerId;
    }
}
