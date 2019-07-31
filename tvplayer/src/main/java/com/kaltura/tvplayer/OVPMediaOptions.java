package com.kaltura.tvplayer;

import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpMediaProvider;

public class OVPMediaOptions extends MediaOptions {
    public String entryId;
    public boolean useApiCaptions;

    @Override
    public MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId, String ks, String referrer) {
        return new KalturaOvpMediaProvider(serverUrl, partnerId, ks)
                .setEntryId(this.entryId).setUseApiCaptions(this.useApiCaptions).setReferrer(referrer);
    }
}

