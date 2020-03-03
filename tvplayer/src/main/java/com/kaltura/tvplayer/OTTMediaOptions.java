package com.kaltura.tvplayer;

import com.kaltura.playkit.providers.BaseMediaAsset;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.ott.OTTMediaAsset;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;

import java.util.List;

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

        final PhoenixMediaProvider provider = new PhoenixMediaProvider(serverUrl, partnerId, ottMediaAsset.getKs())
                .setAssetId(ottMediaAsset.getAssetId());

        if (ottMediaAsset.getProtocol() != null) {
            provider.setProtocol(ottMediaAsset.getProtocol());
        }

        if (ottMediaAsset.getMediaFileIds() != null) {
            List<String> fileIds = ottMediaAsset.getMediaFileIds();
            provider.setFileIds((String[])fileIds.toArray(new String[0]));
        }

        if (ottMediaAsset.getContextType() != null) {
            provider.setContextType(ottMediaAsset.getContextType());
        }

        if (ottMediaAsset.getAssetType() != null) {
            provider.setAssetType(ottMediaAsset.getAssetType());
        }

        if (ottMediaAsset.getUrlType() != null) {
            provider.setPKUrlType(ottMediaAsset.getUrlType());
        }

        if (ottMediaAsset.getFormats() != null) {
            List<String> formats = ottMediaAsset.getFormats();
            provider.setFormats((String[]) formats.toArray(new String[0]));
        }

        if (ottMediaAsset.getAssetReferenceType() != null) {
            provider.setAssetReferenceType(ottMediaAsset.getAssetReferenceType());
        }

        if (ottMediaAsset.getReferrer() != null) {
            provider.setReferrer(ottMediaAsset.getReferrer());
        }

        return provider;
    }
}
