package com.kaltura.tvplayer;

import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpMediaProvider;

public class OVPMediaOptions extends MediaOptions {
    public String entryId;
    public String widgetId;
    public boolean useApiCaptions;

    public OVPMediaOptions(String entryId) {
        this.entryId = entryId;
    }
    public OVPMediaOptions(String entryId, String widgetId) {
        this.entryId = entryId;
        this.widgetId = widgetId;
    }

    public OVPMediaOptions() {}

    @Override
    public MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId, String ks, String referrer) {
        return new KalturaOvpMediaProvider(serverUrl, partnerId, ks)
                .setEntryId(this.entryId).setUseApiCaptions(this.useApiCaptions).setWidgetId(widgetId).setReferrer(referrer);
    }
}

