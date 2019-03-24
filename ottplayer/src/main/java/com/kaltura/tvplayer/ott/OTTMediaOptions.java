package com.kaltura.tvplayer.ott;

import android.support.annotation.NonNull;

import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;
import com.kaltura.tvplayer.MediaOptions;

public class OTTMediaOptions extends MediaOptions {
    String assetId;
    APIDefines.KalturaAssetType assetType;
    APIDefines.PlaybackContextType contextType;
    APIDefines.AssetReferenceType assetReferenceType;
    String protocol;
    String[] formats;
    String[] fileIds;

    public OTTMediaOptions setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public OTTMediaOptions setAssetType(APIDefines.KalturaAssetType assetType) {
        this.assetType = assetType;
        return this;
    }

    public OTTMediaOptions setContextType(APIDefines.PlaybackContextType contextType) {
        this.contextType = contextType;
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public OTTMediaOptions setProtocol(@NonNull @PhoenixMediaProvider.HttpProtocol String protocol) {
        this.protocol = protocol;
        return this;
    }

    public OTTMediaOptions setFormats(String[] formats) {
        this.formats = formats;
        return this;
    }

    public OTTMediaOptions setFileIds(String[] fileIds) {
        this.fileIds = fileIds;
        return this;
    }

    public APIDefines.AssetReferenceType getAssetReferenceType() {
        return assetReferenceType;
    }

    public OTTMediaOptions setAssetReferenceType(APIDefines.AssetReferenceType assetReferenceType) {
        this.assetReferenceType = assetReferenceType;
        return this;
    }

    @Override
    public OTTMediaOptions setKS(String ks) {
        super.setKS(ks);
        return this;
    }

    @Override
    public OTTMediaOptions setStartPosition(double startPosition) {
        super.setStartPosition(startPosition);
        return this;
    }

    @Override
    public OTTMediaOptions setPreferredMediaFormat(String preferredMediaFormat) {
        super.setPreferredMediaFormat(preferredMediaFormat);
        return this;
    }
}
