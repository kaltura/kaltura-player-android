package com.kaltura.tvplayer;

import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.providers.ovp.OVPMediaAsset;

public class OVPMediaOptions extends MediaOptions {

    private OVPMediaAsset ovpMediaAsset;
    private boolean useApiCaptions;

    public OVPMediaOptions(OVPMediaAsset ovpMediaAsset) {
        this.ovpMediaAsset = ovpMediaAsset;
    }

    public OVPMediaOptions(String entryId, String ks, String referrer) {
        this.ovpMediaAsset = new OVPMediaAsset();
        this.ovpMediaAsset.setEntryId(entryId);
        this.ovpMediaAsset.setReferrer(referrer);
    }

    public OVPMediaOptions(String entryId, String ks) {
        this(entryId,ks, null);
    }

    public OVPMediaOptions(String entryId) {
        this(entryId, null, null);
    }

    public OVPMediaAsset getOvpMediaAsset() {
        return ovpMediaAsset;
    }

    public boolean isUseApiCaptions() {
        return useApiCaptions;
    }

    public OVPMediaOptions setUseApiCaptions (boolean useApiCaptions) {
        this.useApiCaptions = useApiCaptions;
        return this;
    }

    @Override
    public MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId) {
        return new KalturaOvpMediaProvider(serverUrl, partnerId, ovpMediaAsset.getKs())
                .setEntryId(ovpMediaAsset.getEntryId()).setUseApiCaptions(this.useApiCaptions).setReferrer(ovpMediaAsset.getReferrer());
    }
}

