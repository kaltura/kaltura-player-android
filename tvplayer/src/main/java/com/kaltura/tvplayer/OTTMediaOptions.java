package com.kaltura.tvplayer;

import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;

public class OTTMediaOptions extends MediaOptions {
    public static final String HTTP = PhoenixMediaProvider.HttpProtocol.Http;;
    public static final String HTTPS = PhoenixMediaProvider.HttpProtocol.Https;

    public String assetId;
    public APIDefines.KalturaAssetType assetType;
    public APIDefines.PlaybackContextType contextType;
    public APIDefines.AssetReferenceType assetReferenceType;
    public String protocol = HTTPS;
    public String[] formats;
    public String[] fileIds;

    @Override
    public MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId, String ks, String referrer) {
        OTTMediaOptions mediaOptions = this;
        final PhoenixMediaProvider provider = new PhoenixMediaProvider(serverUrl, partnerId, ks)
                .setAssetId(mediaOptions.assetId).setReferrer(referrer);

        if (mediaOptions.protocol != null) {
            provider.setProtocol(mediaOptions.protocol);
        }

        if (mediaOptions.fileIds != null) {
            provider.setFileIds(mediaOptions.fileIds);
        }

        if (mediaOptions.contextType != null) {
            provider.setContextType(mediaOptions.contextType);
        }

        if (mediaOptions.assetType != null) {
            provider.setAssetType(mediaOptions.assetType);
        }

        if (mediaOptions.formats != null) {
            provider.setFormats(mediaOptions.formats);
        }

        if (mediaOptions.assetReferenceType != null) {
            provider.setAssetReferenceType(mediaOptions.assetReferenceType);
        }
        return provider;
    }
}
