package com.kaltura.tvplayer;

import android.support.annotation.NonNull;

import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;

public class OTTMediaOptions extends MediaOptions {
    String assetId;

    // TODO: create local enums for assetType and contextType

    APIDefines.KalturaAssetType assetType;
    APIDefines.PlaybackContextType contextType;

    // TODO: 2019-05-07 Remove referenceType, only use the default
    APIDefines.AssetReferenceType assetReferenceType;

    String protocol;// TODO: 2019-05-07 default to "https"
    String[] formats;
    String[] fileIds;



    public OTTMediaOptions setAssetId(String assetId) {
        // why only if not null?
        if (assetId != null) {
            this.assetId = assetId;
        }
        return this;
    }

    public OTTMediaOptions setAssetType(APIDefines.KalturaAssetType assetType) {
        if (assetType != null) {
            this.assetType = assetType;
        }
        return this;
    }

    public OTTMediaOptions setContextType(APIDefines.PlaybackContextType contextType) {
        if (contextType != null) {
            this.contextType = contextType;
        }
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    // TODO: 2019-05-07 remove the annotation
    public OTTMediaOptions setProtocol(@NonNull @PhoenixMediaProvider.HttpProtocol String protocol) {
        if (protocol != null) {
            this.protocol = protocol;
        }
        return this;
    }

    public OTTMediaOptions setFormats(String[] formats) {
        if (formats != null) {
            this.formats = formats;
        }
        return this;
    }

    public OTTMediaOptions setFileIds(String[] fileIds) {
        if (fileIds != null) {
            this.fileIds = fileIds;
        }
        return this;
    }

    public OTTMediaOptions setAssetReferenceType(APIDefines.AssetReferenceType assetReferenceType) {
        if (assetReferenceType != null) {
            this.assetReferenceType = assetReferenceType;
        }
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
}
