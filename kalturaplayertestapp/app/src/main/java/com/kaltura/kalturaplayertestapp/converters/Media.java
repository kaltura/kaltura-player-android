package com.kaltura.kalturaplayertestapp.converters;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;

import java.util.List;

/**
 * Created by gilad.nadav on 1/24/18.
 */

public class Media {
    public String entryId;   // ovp
    public String ks;        // ovp or ott
    public String assetId;   // ott
    public String format;    // ott
    public Integer fileId;   // ott
    public String assetType; // ott
    public String playbackContextType; // ott
    public String assetReferenceType; // ott
    public String protocol; // ott
    public PKMediaEntry pkMediaEntry; // player without provider
    public String mediaAdTag;
    public List<PKExternalSubtitle> externalSubtitles;

    public Media() {}

    public APIDefines.KalturaAssetType getAssetType() {
        if (assetType == null) {
            return null;
        }

        if (APIDefines.KalturaAssetType.Media.value.equals(assetType.toLowerCase())) {
            return APIDefines.KalturaAssetType.Media;
        } else if (APIDefines.KalturaAssetType.Epg.value.equals(assetType.toLowerCase())) {
            return APIDefines.KalturaAssetType.Epg;
        } else if (APIDefines.KalturaAssetType.Recording.value.equals(assetType.toLowerCase())) {
            return APIDefines.KalturaAssetType.Recording;
        }
        return null;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public APIDefines.PlaybackContextType getPlaybackContextType() {
        if (playbackContextType == null) {
            return null;
        }

        if (APIDefines.PlaybackContextType.Playback.value.toLowerCase().equals(playbackContextType.toLowerCase())) {
            return APIDefines.PlaybackContextType.Playback;
        } else if (APIDefines.PlaybackContextType.StartOver.value.toLowerCase().equals(playbackContextType.toLowerCase())) {
            return APIDefines.PlaybackContextType.StartOver;
        } else if (APIDefines.PlaybackContextType.Trailer.value.toLowerCase().equals(playbackContextType.toLowerCase())) {
            return APIDefines.PlaybackContextType.Trailer;
        } else if (APIDefines.PlaybackContextType.Catchup.value.toLowerCase().equals(playbackContextType.toLowerCase())) {
            return APIDefines.PlaybackContextType.Catchup;
        }
        return null;
    }

    public void setPlaybackContextType(String playbackContextType) {
        this.playbackContextType = playbackContextType;
    }

    public APIDefines.AssetReferenceType getAssetReferenceType() {
        if (assetReferenceType == null) {
            return null;
        }
        if (APIDefines.AssetReferenceType.Media.value.toLowerCase().equals(assetReferenceType.toLowerCase())) {
            return APIDefines.AssetReferenceType.Media;
        } else if (APIDefines.AssetReferenceType.ExternalEpg.value.toLowerCase().equals(assetReferenceType.toLowerCase())) {
            return APIDefines.AssetReferenceType.ExternalEpg;
        } else if (APIDefines.AssetReferenceType.InternalEpg.value.toLowerCase().equals(assetReferenceType.toLowerCase())) {
            return APIDefines.AssetReferenceType.InternalEpg;
        }
        return null;
    }

    public String getProtocol() {
        if (protocol == null) {
            return null;
        }
        if (PhoenixMediaProvider.HttpProtocol.All.toLowerCase().equals(protocol.toLowerCase())) {
            return PhoenixMediaProvider.HttpProtocol.All;
        } else if (PhoenixMediaProvider.HttpProtocol.Http.toLowerCase().equals(protocol.toLowerCase())) {
            return PhoenixMediaProvider.HttpProtocol.Http;
        } else if (PhoenixMediaProvider.HttpProtocol.Https.toLowerCase().equals(protocol.toLowerCase())) {
            return PhoenixMediaProvider.HttpProtocol.Https;
        }
        return null;
    }

    public void setAssetReferenceType(String assetReferenceType) {
        this.assetReferenceType = assetReferenceType;
    }
}

