package com.kaltura.tvplayer.offline;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpMediaProvider;
import com.kaltura.tvplayer.*;

import java.io.IOException;

abstract class AbstractOfflineManager extends OfflineManager {
    private String kalturaServerUrl;
    private Integer kalturaPartnerId;

    @Override
    public final void prepareAsset(MediaOptions mediaOptions, SelectionPrefs prefs,
                                   PrepareListener prepareListener) throws IllegalStateException {

        if (kalturaPartnerId == null || kalturaServerUrl == null) {
            throw new IllegalStateException("kalturaPartnerId and/or kalturaServerUrl not set");
        }

        final MediaEntryProvider mediaEntryProvider;
        if (mediaOptions instanceof OVPMediaOptions) {

            final OVPMediaOptions options = (OVPMediaOptions) mediaOptions;
            mediaEntryProvider = new KalturaOvpMediaProvider(kalturaServerUrl, kalturaPartnerId, mediaOptions.ks)
                    .setEntryId(options.entryId)
                    .setUseApiCaptions(options.useApiCaptions);

        } else if (mediaOptions instanceof OTTMediaOptions) {

            final OTTMediaOptions options = (OTTMediaOptions) mediaOptions;
            mediaEntryProvider = new PhoenixMediaProvider(kalturaServerUrl, kalturaPartnerId, mediaOptions.ks)
                    .setAssetId(options.assetId)
                    .setAssetReferenceType(options.assetReferenceType)
                    .setAssetType(options.assetType)
                    .setContextType(options.contextType)
                    .setFileIds(options.fileIds)
                    .setFormats(options.formats)
                    .setProtocol(options.protocol);
        } else {
            throw new IllegalArgumentException("Invalid MediaOptions type");
        }

        mediaEntryProvider.load(response -> {
            if (response.isSuccess()) {
                prepareAsset(response.getResponse(), prefs, prepareListener);
            } else {
                prepareListener.onPrepareFailed(new IOException(response.getError().getMessage()));
            }
        });
    }

    @Override
    public final void sendAssetToPlayer(String assetId, KalturaPlayer player) {
        final PKMediaEntry entry = getLocalPlaybackEntry(assetId);
        player.setMedia(entry);
    }

    @Override
    public void setKalturaServerUrl(String url) {
        this.kalturaServerUrl = url;
    }

    @Override
    public void setKalturaPartnerId(int partnerId) {
        this.kalturaPartnerId = partnerId;
    }
}
