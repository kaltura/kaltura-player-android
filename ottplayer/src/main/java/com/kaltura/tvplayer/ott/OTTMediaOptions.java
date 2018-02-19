package com.kaltura.tvplayer.ott;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.api.ovp.SimpleOvpSessionProvider;
import com.kaltura.playkit.api.phoenix.APIDefines;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ott.PhoenixMediaProvider;
import com.kaltura.tvplayer.MediaOptions;

public class OTTMediaOptions extends MediaOptions {
    String assetId;
    APIDefines.KalturaAssetType assetType;
    APIDefines.PlaybackContextType contextType;
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

    public OTTMediaOptions setFormats(String[] formats) {
        this.formats = formats;
        return this;
    }

    public OTTMediaOptions setFileIds(String[] fileIds) {
        this.fileIds = fileIds;
        return this;
    }

    @Override
    public void loadMedia(String serverUrl, int partnerId, final OnEntryLoadListener listener) {
        final PhoenixMediaProvider provider = new PhoenixMediaProvider()
                .setAssetId(assetId)
                .setSessionProvider(new SimpleOvpSessionProvider(serverUrl, partnerId, ks));

        if (fileIds != null) {
            provider.setFileIds(fileIds);
        }

        if (contextType != null) {
            provider.setContextType(contextType);
        }

        if (assetType != null) {
            provider.setAssetType(assetType);
        }

        if (formats != null) {
            provider.setFormats(formats);
        }

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                listener.onEntryLoadComplete(response.getResponse(), response.getError());
            }
        });
    }
}
