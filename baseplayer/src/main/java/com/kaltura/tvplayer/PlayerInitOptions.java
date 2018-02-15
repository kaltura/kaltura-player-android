package com.kaltura.tvplayer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PKTrackConfig;
import com.kaltura.tvplayer.utils.GsonReader;


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

        if (playbackJson != null) {
            if (playbackJson.has(AUDIO_LANG)) {
                if (OFF.equals(playbackJson.get(AUDIO_LANG).getAsString())) {
                    audioLanguageMode = PKTrackConfig.Mode.OFF;
                    audioLanguage = "";
                } else if (AUTO.equals(playbackJson.get(AUDIO_LANG).getAsString())) {
                    audioLanguageMode = PKTrackConfig.Mode.AUTO;
                    audioLanguage = "";
                } else {
                    audioLanguageMode = PKTrackConfig.Mode.SELECTION;
                    audioLanguage = playbackJson.get(AUDIO_LANG).getAsString();
                }
            }

            if (playbackJson.has(TEXT_LANG)) {
                if (OFF.equals(playbackJson.get(TEXT_LANG).getAsString())) {
                    textLanguageMode = PKTrackConfig.Mode.OFF;
                    textLanguage = "";
                } else if (AUTO.equals(playbackJson.get(TEXT_LANG).getAsString())) {
                    textLanguageMode = PKTrackConfig.Mode.AUTO;
                    textLanguage = "";
                } else {
                    textLanguageMode = PKTrackConfig.Mode.SELECTION;
                    textLanguage = playbackJson.get(TEXT_LANG).getAsString();
                }
            }
            if (playbackJson.has(AUTOPLAY)) {
                autoplay = playbackJson.get(AUTOPLAY).getAsBoolean();
            }

            if (playbackJson.has(PRELOAD)) {
                String playerConfigPreload = playbackJson.get(PRELOAD).getAsString();
                preload = AUTO.equals(playerConfigPreload);
            }

            if (playbackJson.has(START_TIME)) {
                startTime = playbackJson.get(START_TIME).getAsInt();
            }
        }

        JsonArray streamPriorityJsonArray = null;
        if (playbackJson != null) {
            streamPriorityJsonArray = (reader.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLAYBACK).getAsJsonArray(STREAM_PRIORITY) != null) ?
                    reader.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLAYBACK).getAsJsonArray(STREAM_PRIORITY) : null;
            if (streamPriorityJsonArray != null) {
                Gson gson = new Gson();
                StreamPriority[] streamPriority = gson.fromJson(streamPriorityJsonArray, StreamPriority[].class);
                if (streamPriority != null && streamPriority.length > 0) {
                    setPreferredMediaFormat(streamPriority[0].getFormat());
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

    public PlayerInitOptions setPreferredMediaFormat(String preferredMediaFormat) {
        if (preferredMediaFormat != null) {
            this.preferredMediaFormat = PKMediaFormat.valueOf(preferredMediaFormat);
        } else {
            this.preferredMediaFormat = PKMediaFormat.dash;
        }
        return this;
    }

    private class StreamPriority {
        String engine;
        String format;

        public String getEngine() {
            return engine;
        }

        public String getFormat() {
            return format;
        }
    }
}