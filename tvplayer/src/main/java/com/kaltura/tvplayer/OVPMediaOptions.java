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

    public OVPMediaOptions(String entryId, String referenceId, String ks, String referrer) {
        ovpMediaAsset = new OVPMediaAsset();
        ovpMediaAsset.setEntryId(entryId);
        ovpMediaAsset.setKs(ks);
        ovpMediaAsset.setReferenceId(referenceId);
        ovpMediaAsset.setReferrer(referrer);
        ovpMediaAsset.setRedirectFromEntryId(true);
    }

    public OVPMediaOptions(String entryId, String referenceId, String ks, String referrer, boolean redirectFromEntryId) {
        ovpMediaAsset = new OVPMediaAsset();
        ovpMediaAsset.setEntryId(entryId);
        ovpMediaAsset.setKs(ks);
        ovpMediaAsset.setReferenceId(referenceId);
        ovpMediaAsset.setReferrer(referrer);
        ovpMediaAsset.setRedirectFromEntryId(redirectFromEntryId);
    }

    public OVPMediaOptions(String entryId, String ks) {
        this(entryId, null, ks, null);
    }

    public OVPMediaOptions(String entryId) {
        this(entryId, null, null, null);
    }

    public OVPMediaAsset getOvpMediaAsset() {
        return ovpMediaAsset;
    }

    public boolean isUseApiCaptions() {
        return useApiCaptions;
    }

    public OVPMediaOptions setReferenceId (String referenceId) {
        if (ovpMediaAsset != null) {
            ovpMediaAsset.setReferenceId(referenceId);
        }
        return this;
    }

    public OVPMediaOptions setUseApiCaptions (boolean useApiCaptions) {
        this.useApiCaptions = useApiCaptions;
        return this;
    }

    @Override
    public MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId) {
        return new KalturaOvpMediaProvider(serverUrl, partnerId, ovpMediaAsset.getKs())
                .setEntryId(ovpMediaAsset.getEntryId())
                .setReferenceId(ovpMediaAsset.getReferenceId())
                .setUseApiCaptions(this.useApiCaptions)
                .setReferrer(ovpMediaAsset.getReferrer())
                .setRedirectFromEntryId(ovpMediaAsset.getRedirectFromEntryId());
    }
}


