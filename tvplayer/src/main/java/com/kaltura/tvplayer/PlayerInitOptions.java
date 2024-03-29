package com.kaltura.tvplayer;

import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKRequestConfig;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.PKSubtitlePreference;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.PKWakeMode;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.AudioCodecSettings;
import com.kaltura.playkit.player.DRMSettings;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.MulticastSettings;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.PKLowLatencyConfig;
import com.kaltura.playkit.player.PKMaxVideoSize;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.player.VideoCodecSettings;
import com.kaltura.playkit.player.vr.VRSettings;
import com.kaltura.tvplayer.config.MediaEntryCacheConfig;
import com.kaltura.tvplayer.config.TVPlayerParams;

public class PlayerInitOptions {

    public final Integer partnerId;
    public String ks;
    public TVPlayerParams tvPlayerParams;
    public PKPluginConfigs pluginConfigs;

    public Boolean autoplay = true;
    public Boolean preload = true;
    public String referrer;
    public Boolean redirectFromEntryId = true;
    public PKTrackConfig.Mode audioLanguageMode;
    public String audioLanguage;
    public PKTrackConfig.Mode textLanguageMode;
    public String textLanguage;
    public PKMediaFormat preferredMediaFormat;
    public Boolean allowCrossProtocolEnabled;
    public Boolean allowClearLead;
    public Boolean enableDecoderFallback;
    public Boolean secureSurface;
    public Boolean adAutoPlayOnResume;
    public Boolean vrPlayerEnabled;
    public Boolean isVideoViewHidden;
    public Boolean forceSinglePlayerEngine;
    public Boolean forceWidevineL3Playback;
    public Boolean allowChunklessPreparation = true;
    public Boolean constrainAudioChannelCountToDeviceCapabilities;
    public DRMSettings drmSettings;
    public SubtitleStyleSettings setSubtitleStyle;
    public PKAspectRatioResizeMode aspectRatioResizeMode;
    public PKRequestParams.Adapter contentRequestAdapter;
    public PKRequestParams.Adapter licenseRequestAdapter;
    public LoadControlBuffers loadControlBuffers;
    public ABRSettings abrSettings;
    public VRSettings vrSettings;
    public PKLowLatencyConfig pkLowLatencyConfig;
    public PKRequestConfig pkRequestConfig;
    public Boolean cea608CaptionsEnabled;
    public Boolean mpgaAudioFormatEnabled;
    public Boolean useTextureView;
    public VideoCodecSettings videoCodecSettings;
    public AudioCodecSettings audioCodecSettings;
    public Boolean isTunneledAudioPlayback;
    public Boolean handleAudioBecomingNoisyEnabled;
    public Boolean handleAudioFocus;
    public PKWakeMode wakeMode;
    public PKSubtitlePreference subtitlePreference;

    public PKMaxVideoSize maxVideoSize;
    public Integer maxVideoBitrate;
    public Integer maxAudioBitrate;
    public Integer maxAudioChannelCount;
    public MulticastSettings multicastSettings;

    public Boolean shutterStaysOnRenderedFirstFrame;

    public MediaEntryCacheConfig mediaEntryCacheConfig;
    public OfflineManager.OfflineProvider offlineProvider = OfflineManager.OfflineProvider.DTG;

    public PlayerInitOptions() {
        partnerId = null;
    }

    public PlayerInitOptions(Integer partnerId) {
        this.partnerId = partnerId;
    }

    public PlayerInitOptions setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public PlayerInitOptions setPluginConfigs(PKPluginConfigs pluginConfigs) {
        if (pluginConfigs != null) {
            this.pluginConfigs = pluginConfigs;
        }
        return this;
    }

    public PlayerInitOptions setAutoPlay(Boolean autoplay) {
        if (autoplay != null) {
            this.autoplay = autoplay;
        }
        return this;
    }

    public PlayerInitOptions setPreload(Boolean preload) {
        if (preload != null) {
            this.preload = preload;
        }
        return this;
    }

    public PlayerInitOptions setReferrer(String referrer) {
        if (referrer != null) {
            this.referrer = referrer;
        }
        return this;
    }

    public PlayerInitOptions setRedirectFromEntryId(Boolean redirectFromEntryId) {
        if (redirectFromEntryId != null) {
            this.redirectFromEntryId = redirectFromEntryId;
        }
        return this;
    }

    public PlayerInitOptions setAudioLanguage(String audioLanguage, PKTrackConfig.Mode audioLanguageMode) {
        if (audioLanguage != null && audioLanguageMode != null) {
            this.audioLanguage = audioLanguage;
            this.audioLanguageMode = audioLanguageMode;
        }
        return this;
    }

    public PlayerInitOptions setTextLanguage(String textLanguage, PKTrackConfig.Mode textLanguageMode) {
        if (textLanguage != null && textLanguageMode != null) {
            this.textLanguage = textLanguage;
            this.textLanguageMode = textLanguageMode;
        }
        return this;
    }

    /**
     * This method is deprecated.
     * Please use {@link com.kaltura.playkit.PKRequestConfig} in order to set crossProtocolRedirect
     */
    @Deprecated
    public PlayerInitOptions setAllowCrossProtocolEnabled(Boolean allowCrossProtocolEnabled) {
        if (allowCrossProtocolEnabled != null) {
            this.allowCrossProtocolEnabled = allowCrossProtocolEnabled;
        }
        return this;
    }

    public PlayerInitOptions setPreferredMediaFormat(PKMediaFormat preferredMediaFormat) {
        if (preferredMediaFormat != null) {
            this.preferredMediaFormat = preferredMediaFormat;
        }
        return this;
    }

    public PlayerInitOptions setAllowClearLead(Boolean allowClearLead) {
        if (allowClearLead != null) {
            this.allowClearLead = allowClearLead;
        }
        return this;
    }

    public PlayerInitOptions setEnableDecoderFallback(Boolean enableDecoderFallback) {
        if (enableDecoderFallback != null) {
            this.enableDecoderFallback = enableDecoderFallback;
        }
        return this;
    }

    public PlayerInitOptions setSecureSurface(Boolean secureSurface) {
        if (secureSurface != null) {
            this.secureSurface = secureSurface;
        }
        return this;
    }

    public PlayerInitOptions setAdAutoPlayOnResume(Boolean adAutoPlayOnResume) {
        if (adAutoPlayOnResume != null) {
            this.adAutoPlayOnResume = adAutoPlayOnResume;
        }
        return this;
    }

    public PlayerInitOptions setVrPlayerEnabled(Boolean vrPlayerEnabled) {
        if (vrPlayerEnabled != null) {
            this.vrPlayerEnabled = vrPlayerEnabled;
        }
        return this;
    }

    public PlayerInitOptions setVRSettings(VRSettings vrSettings) {
        if (vrSettings != null) {
            this.vrSettings = vrSettings;
        }
        return this;
    }

    public PlayerInitOptions setPKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig) {
        if (pkLowLatencyConfig != null) {
            this.pkLowLatencyConfig = pkLowLatencyConfig;
        }
        return this;
    }

    public PlayerInitOptions setPKRequestConfig(PKRequestConfig pkRequestConfig) {
        if (pkRequestConfig != null) {
            this.pkRequestConfig = pkRequestConfig;
        }
        return this;
    }

    public PlayerInitOptions setIsVideoViewHidden(Boolean isVideoViewHidden) {
        if (isVideoViewHidden != null) {
            this.isVideoViewHidden = isVideoViewHidden;
        }
        return this;
    }

    public PlayerInitOptions forceSinglePlayerEngine(Boolean forceSinglePlayerEngine) {
        if (forceSinglePlayerEngine != null) {
            this.forceSinglePlayerEngine = forceSinglePlayerEngine;
        }
        return this;
    }

    public PlayerInitOptions allowChunklessPreparation(Boolean allowChunklessPreparation) {
        if (allowChunklessPreparation != null) {
            this.allowChunklessPreparation = allowChunklessPreparation;
        }
        return this;
    }

    public PlayerInitOptions constrainAudioChannelCountToDeviceCapabilities(Boolean enabled) {
        if (constrainAudioChannelCountToDeviceCapabilities != null) {
            this.constrainAudioChannelCountToDeviceCapabilities = enabled;
        }
        return this;
    }

    public PlayerInitOptions forceWidevineL3Playback(Boolean forceWidevineL3Playback) {
        if (forceWidevineL3Playback != null) {
            this.forceWidevineL3Playback = forceWidevineL3Playback;
        }
        return this;
    }

    public PlayerInitOptions setDrmSettings(DRMSettings drmSettings) {
        if (drmSettings != null) {
            this.drmSettings = drmSettings;
        }
        return this;
    }

    public PlayerInitOptions setSubtitleStyle(SubtitleStyleSettings setSubtitleStyle) {
        if (setSubtitleStyle != null) {
            this.setSubtitleStyle = setSubtitleStyle;
        }
        return this;
    }

    public PlayerInitOptions setAspectRatioResizeMode(PKAspectRatioResizeMode aspectRatioResizeMode) {
        if (aspectRatioResizeMode != null) {
            this.aspectRatioResizeMode = aspectRatioResizeMode;
        }
        return this;
    }

    public PlayerInitOptions setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter) {
        if (contentRequestAdapter != null) {
            this.contentRequestAdapter = contentRequestAdapter;
        }
        return this;
    }

    public PlayerInitOptions setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        if (contentRequestAdapter != null) {
            this.licenseRequestAdapter = licenseRequestAdapter;
        }
        return this;
    }

    public PlayerInitOptions setLoadControlBuffers(LoadControlBuffers loadControlBuffers) {
        if (loadControlBuffers != null) {
            this.loadControlBuffers = loadControlBuffers;
        }
        return this;
    }

    public PlayerInitOptions setAbrSettings(ABRSettings abrSettings) {
        if (abrSettings != null) {
            this.abrSettings = abrSettings;
        }
        return this;
    }

    public PlayerInitOptions setTVPlayerParams(TVPlayerParams tvPlayerParams) {
        if (tvPlayerParams != null) {
            this.tvPlayerParams = tvPlayerParams;
        }
        return this;
    }

    public PlayerInitOptions setCea608CaptionsEnabled(Boolean cea608CaptionsEnabled) {
        if (cea608CaptionsEnabled != null) {
            this.cea608CaptionsEnabled = cea608CaptionsEnabled;
        }
        return this;
    }

    public PlayerInitOptions setMpgaAudioFormatEnabled(Boolean mpgaAudioFormatEnabled) {
        if (mpgaAudioFormatEnabled != null) {
            this.mpgaAudioFormatEnabled = mpgaAudioFormatEnabled;
        }
        return this;
    }

    public PlayerInitOptions useTextureView(Boolean useTextureView) {
        if (useTextureView != null) {
            this.useTextureView = useTextureView;
        }
        return this;
    }

    public PlayerInitOptions setVideoCodecSettings(VideoCodecSettings videoCodecSettings) {
        if (videoCodecSettings != null) {
            this.videoCodecSettings = videoCodecSettings;
        }
        return this;
    }

    public PlayerInitOptions setAudioCodecSettings(AudioCodecSettings audioCodecSettings) {
        if (audioCodecSettings != null) {
            this.audioCodecSettings = audioCodecSettings;
        }
        return this;
    }

    public PlayerInitOptions setTunneledAudioPlayback(Boolean isTunneledAudioPlayback) {
        if (isTunneledAudioPlayback != null) {
            this.isTunneledAudioPlayback = isTunneledAudioPlayback;
        }
        return this;
    }

    public PlayerInitOptions setHandleAudioBecomingNoisy(Boolean handleAudioBecomingNoisyEnabled) {
        if (handleAudioBecomingNoisyEnabled != null) {
            this.handleAudioBecomingNoisyEnabled = handleAudioBecomingNoisyEnabled;
        }
        return this;
    }

    public PlayerInitOptions setHandleAudioFocus(Boolean handleAudioFocus) {
        if (handleAudioFocus != null) {
            this.handleAudioFocus = handleAudioFocus;
        }
        return this;
    }

    public PlayerInitOptions setWakeMode(PKWakeMode wakeMode) {
        if (wakeMode != null) {
            this.wakeMode = wakeMode;
        }
        return this;
    }

    public PlayerInitOptions setSubtitlePreference(PKSubtitlePreference subtitlePreference) {
        if (subtitlePreference != null) {
            this.subtitlePreference = subtitlePreference;
        }
        return this;
    }

    /**
     * Please Use {@link #setAbrSettings(com.kaltura.playkit.player.ABRSettings)}
     * to achieve this functionality.
     */
    @Deprecated
    public PlayerInitOptions setMaxVideoSize(PKMaxVideoSize maxVideoSize) {
        if (maxVideoSize != null) {
            this.maxVideoSize = maxVideoSize;
        }
        return this;
    }

    /**
     * Please Use {@link #setAbrSettings(com.kaltura.playkit.player.ABRSettings)}
     * to achieve this functionality.
     */
    @Deprecated
    public PlayerInitOptions setMaxVideoBitrate(Integer maxVideoBitrate) {
        if (maxVideoBitrate != null) {
            this.maxVideoBitrate = maxVideoBitrate;
        }
        return this;
    }

    public PlayerInitOptions setMaxAudioBitrate(Integer maxAudioBitrate) {
        if (maxAudioBitrate != null) {
            this.maxAudioBitrate = maxAudioBitrate;
        }
        return this;
    }

    public PlayerInitOptions setMaxAudioChannelCount(Integer maxAudioChannelCount) {
        if (maxAudioChannelCount != null) {
            this.maxAudioChannelCount = maxAudioChannelCount;
        }
        return this;
    }

    public PlayerInitOptions setMulticastSettings(MulticastSettings multicastSettings) {
        if (multicastSettings != null) {
            this.multicastSettings = multicastSettings;
        }
        return this;
    }

    public PlayerInitOptions setMediaEntryCacheConfig(MediaEntryCacheConfig mediaEntryCacheConfig) {
        if (mediaEntryCacheConfig != null) {
            this.mediaEntryCacheConfig = mediaEntryCacheConfig;
        }
        return this;
    }

    public PlayerInitOptions setOfflineProvider(OfflineManager.OfflineProvider offlineProvider) {
        if (offlineProvider != null) {
            this.offlineProvider = offlineProvider;
        }
        return this;
    }
}
