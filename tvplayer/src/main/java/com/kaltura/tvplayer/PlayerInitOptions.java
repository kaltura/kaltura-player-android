package com.kaltura.tvplayer;

import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.tvplayer.config.player.UiConf;

public class PlayerInitOptions {
    public static final String PLAYER = "player";
    public static final String AUDIO_LANG = "audioLanguage";
    public static final String TEXT_LANG = "textLanguage";
    public static final String PLAYBACK = "playback";
    public static final String OFF = "off";
    public static final String AUTOPLAY = "autoplay";
    public static final String PRELOAD = "preload";
    //public static final String START_TIME = "startTime";
    public static final String CONFIG = "config";
    public static final String PLUGINS = "plugins";
    public static final String AUTO = "auto";
    public static final String OPTIONS = "options";
    public static final String UICONF_ID ="uiConfId";
    public static final String PARTNER_ID = "partnerId";
    public static final String REFERRER = "referrer";
    public static final String KS = "ks";
    public static final String SERVER_URL = "serverUrl";
    public static final String ALLOW_CROSS_PROTOCOL_ENABLED = "allowCrossProtocolEnabled";
    public static final String STREAM_PRIORITY = "streamPriority";

    public final Integer partnerId;
    public final Integer uiConfPartnerId;

    public String ks;
    public Integer uiConfId;
    public PKPluginConfigs pluginConfigs;

    public Boolean autoplay = true;
    public Boolean preload = true;
    //public int startTime;
    public String serverUrl;
    public String referrer;
    public PKTrackConfig.Mode audioLanguageMode;
    public String audioLanguage;
    public PKTrackConfig.Mode textLanguageMode;
    public String textLanguage;
    public PKMediaFormat preferredMediaFormat;
    public Boolean allowCrossProtocolEnabled;
    public Boolean allowClearLead;
    public Boolean secureSurface;
    public Boolean adAutoPlayOnResume;
    public Boolean vrPlayerEnabled;
    public Boolean isVideoViewHidden;
    public SubtitleStyleSettings setSubtitleStyle;
    public PKAspectRatioResizeMode aspectRatioResizeMode;
    public PKRequestParams.Adapter contentRequestAdapter;
    public PKRequestParams.Adapter licenseRequestAdapter;
    public LoadControlBuffers loadControlBuffers;
    public ABRSettings abrSettings;

    public PlayerInitOptions() {
        partnerId = null;
        uiConfPartnerId = null;
    }

    public PlayerInitOptions(Integer partnerId, UiConf uiConf) {
        this.partnerId = partnerId;
        this.uiConfPartnerId = uiConf.partnerId;
        this.uiConfId  = uiConf.uiConfId;
    }



//    @Deprecated
//    public PlayerInitOptions(int partnerId, int uiConfId, JsonObject uiConfJsonObjet) {
//        this.partnerId = partnerId;
//        this.uiConfId  = uiConfId;
//        // Fields not found in the UIConf: partnerId*, ks*, serverUrl, referrer
//
//        if (uiConfJsonObjet == null) {
//            this.uiConfJsonObjet = null;
//            return;
//        }
//        this.uiConfJsonObjet = uiConfJsonObjet;
//        fillUiConfPlaybackData(uiConfJsonObjet);
//    }
//
//    private void fillUiConfPlaybackData(JsonObject uiConfJson) {
//
//        GsonReader reader = GsonReader.withObject(uiConfJson);
//        JsonObject playbackJson = (reader != null && reader.getObject(CONFIG) != null && reader.getObject(CONFIG).getAsJsonObject(PLAYER) != null) ? reader.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLAYBACK) : null;
//        if (playbackJson != null && playbackJson.keySet().size() > 0) {
//            Gson gson = new Gson();
//            UiConfPlayer uiconfPlayer = gson.fromJson(playbackJson, UiConfPlayer.class);
//            String audioLang = uiconfPlayer.getAudioLanguage();
//            if (audioLang != null) {
//                if (AUTO.equals(audioLang)) { // maybe "" is also considered as AUTO????
//                    audioLanguageMode = PKTrackConfig.Mode.AUTO;
//                    audioLanguage = "";
//                } else if (!"".equals(audioLang)){
//                    audioLanguageMode = PKTrackConfig.Mode.SELECTION;
//                    audioLanguage = audioLang;
//                }
//            }
//
//            String textLang = uiconfPlayer.getTextLanguage();
//            if (textLang != null) {
//                if (OFF.equals(textLang)) {
//                    textLanguageMode = PKTrackConfig.Mode.OFF;
//                    textLanguage = "";
//                } else if (AUTO.equals(textLang)) {
//                    textLanguageMode = PKTrackConfig.Mode.AUTO;
//                    textLanguage = "";
//                } else {
//                    textLanguageMode = PKTrackConfig.Mode.SELECTION;
//                    textLanguage = textLang;
//                }
//            }
//            if (uiconfPlayer.getAutoplay() != null) {
//                autoplay = uiconfPlayer.getAutoplay();
//            }
//
//            if (uiconfPlayer.getPreload() != null) {
//                String playerConfigPreload = uiconfPlayer.getPreload();
//                preload = AUTO.equals(playerConfigPreload);
//            }
//
//            if (uiconfPlayer.getStartTime() != null) {
//                startTime = uiconfPlayer.getStartTime();
//            }
//
//            if (uiconfPlayer.getStreamPriority() != null) {
//                List<StreamType> streamTypeList = uiconfPlayer.getStreamPriority();
//                if (streamTypeList != null && streamTypeList.size() > 0) {
//                    setPreferredMediaFormat(streamTypeList.get(0).getFormat());
//                }
//            }
//        }
//    }

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


    public PlayerInitOptions setUiConfId(Integer uiConfId) {
        if (uiConfId != null) {
            this.uiConfId = uiConfId;
        }
        return this;
    }

    public PlayerInitOptions setAutoPlay(Boolean autoplay) {
        if (autoplay != null) {
            this.autoplay = autoplay;
        }
        return this;
    }

//    public PlayerInitOptions setStartTime(int startTime) {
//        this.startTime = startTime;
//        return this;
//    }

    public PlayerInitOptions setPreload(Boolean preload) {
        if (preload != null) {
            this.preload = preload;
        }
        return this;
    }

    public PlayerInitOptions setServerUrl(String serverUrl) {
        if (serverUrl != null) {
            this.serverUrl = serverUrl;
        }
        return this;
    }

    public PlayerInitOptions setReferrer(String referrer) {
        if (referrer != null) {
            this.referrer = referrer;
        }
        return this;
    }

    public PlayerInitOptions setAudioLanguage(String audioLanguage) {
        if (audioLanguage != null) {
            this.audioLanguage = audioLanguage;
        }
        return this;
    }

    public PlayerInitOptions setTextLanguage(String textLanguage) {
        if (textLanguage != null) {
            this.textLanguage = textLanguage;
        }
        return this;
    }

    public PlayerInitOptions setAllowCrossProtocolEnabled(Boolean allowCrossProtocolEnabled) {
        if (allowCrossProtocolEnabled != null) {
            this.allowCrossProtocolEnabled = allowCrossProtocolEnabled;
        }
        return this;
    }

    public PlayerInitOptions setPreferredMediaFormat(String preferredMediaFormat) {
        if (preferredMediaFormat != null) {
            if (preferredMediaFormat.equals("progressive")) {
                preferredMediaFormat = "mp4";
            }
            this.preferredMediaFormat = PKMediaFormat.valueOf(preferredMediaFormat);
        } else {
            this.preferredMediaFormat = PKMediaFormat.dash;
        }
        return this;
    }

    public PlayerInitOptions setAllowClearLead(Boolean allowClearLead) {
        if (allowClearLead != null) {
            this.allowClearLead = allowClearLead;
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

    public PlayerInitOptions setIsVideoViewHidden(Boolean isVideoViewHidden) {
        if (isVideoViewHidden != null) {
            this.isVideoViewHidden = isVideoViewHidden;
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
}