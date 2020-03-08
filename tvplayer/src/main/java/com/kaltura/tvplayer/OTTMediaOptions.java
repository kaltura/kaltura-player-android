package com.kaltura.tvplayer;

import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.ott.OTTMediaAsset;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;

public class OTTMediaOptions extends MediaOptions {

    private OTTMediaAsset ottMediaAsset;

    public OTTMediaOptions(OTTMediaAsset ottMediaAsset) {
        this.ottMediaAsset = ottMediaAsset;
    }

    public OTTMediaAsset getOttMediaAsset() {
        return ottMediaAsset;
    }

    @Override
    public MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId) {
        return new PhoenixMediaProvider(serverUrl, partnerId, ottMediaAsset);
    }
}
