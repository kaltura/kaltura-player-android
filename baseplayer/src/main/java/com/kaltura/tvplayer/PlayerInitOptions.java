package com.kaltura.tvplayer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.tvplayer.config.player.StreamType;
import com.kaltura.tvplayer.config.player.UiConfPlayer;
import com.kaltura.tvplayer.utils.GsonReader;

import java.util.List;


public class PlayerInitOptions {
    public static final String PLAYER = "player";
    public static final String AUDIO_LANG = "audioLanguage";
    public static final String TEXT_LANG = "textLanguage";
    public static final String PLAYBACK = "playback";
    public static final String OFF = "off";
    public static final String AUTOPLAY = "autoplay";
    public static final String PRELOAD = "preload";
    public static final String START_TIME = "startTime";
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

    public final int partnerId;
    public final JsonObject uiConf;

    public String ks;
    public Integer uiConfId;
    public PKPluginConfigs pluginConfigs;

    public Boolean autoplay = true;
    public Boolean preload = true;
    public int startTime;
    public String serverUrl;
    public String referrer;
    public PKTrackConfig.Mode audioLanguageMode;
    public String audioLanguage;
    public PKTrackConfig.Mode textLanguageMode;
    public String textLanguage;
    public PKMediaFormat preferredMediaFormat;
    public boolean allowCrossProtocolEnabled;

    public PlayerInitOptions(int partnerId, int uiConfId, JsonObject uiConf) {
        this.partnerId = partnerId;
        this.uiConfId  = uiConfId;
        // Fields not found in the UIConf: partnerId*, ks*, serverUrl, referrer

        if (uiConf == null) {
            this.uiConf = null;
            return;
        }
        this.uiConf = uiConf;
        fillUiConfPlaybackData(uiConf);
    }

    private void fillUiConfPlaybackData(JsonObject uiConf) {

        GsonReader reader = GsonReader.withObject(uiConf);
        JsonObject playbackJson = (reader != null && reader.getObject(CONFIG) != null && reader.getObject(CONFIG).getAsJsonObject(PLAYER) != null) ? reader.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLAYBACK) : null;
        if (playbackJson != null && playbackJson.keySet().size() > 0) {
            Gson gson = new Gson();
            UiConfPlayer uiconfPlayer = gson.fromJson(playbackJson, UiConfPlayer.class);
            String audioLang = uiconfPlayer.getAudioLanguage();
            if (audioLang != null) {
                if (AUTO.equals(audioLang)) { // maybe "" is also considered as AUTO????
                    audioLanguageMode = PKTrackConfig.Mode.AUTO;
                    audioLanguage = "";
                } else if (!"".equals(audioLang)){
                    audioLanguageMode = PKTrackConfig.Mode.SELECTION;
                    audioLanguage = audioLang;
                }
            }

            String textLang = uiconfPlayer.getTextLanguage();
            if (textLang != null) {
                if (OFF.equals(textLang)) {
                    textLanguageMode = PKTrackConfig.Mode.OFF;
                    textLanguage = "";
                } else if (AUTO.equals(textLang)) {
                    textLanguageMode = PKTrackConfig.Mode.AUTO;
                    textLanguage = "";
                } else {
                    textLanguageMode = PKTrackConfig.Mode.SELECTION;
                    textLanguage = textLang;
                }
            }
            if (uiconfPlayer.getAutoplay() != null) {
                autoplay = uiconfPlayer.getAutoplay();
            }

            if (uiconfPlayer.getPreload() != null) {
                String playerConfigPreload = uiconfPlayer.getPreload();
                preload = AUTO.equals(playerConfigPreload);
            }

            if (uiconfPlayer.getStartTime() != null) {
                startTime = uiconfPlayer.getStartTime();
            }

            if (uiconfPlayer.getStreamPriority() != null) {
                List<StreamType> streamTypeList = uiconfPlayer.getStreamPriority();
                if (streamTypeList != null && streamTypeList.size() > 0) {
                    setPreferredMediaFormat(streamTypeList.get(0).getFormat());
                }
            }
        }
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

    public PlayerInitOptions setStartTime(int startTime) {
        this.startTime = startTime;
        return this;
    }

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
            this.preferredMediaFormat = PKMediaFormat.valueOf(preferredMediaFormat);
        } else {
            this.preferredMediaFormat = PKMediaFormat.dash;
        }
        return this;
    }
}