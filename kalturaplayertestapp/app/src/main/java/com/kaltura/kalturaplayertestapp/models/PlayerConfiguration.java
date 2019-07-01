package com.kaltura.kalturaplayertestapp.models;

/**
 * Created by gilad.nadav on 16/01/2018.
 */

public class PlayerConfiguration {
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

    private String mediaProvider;
    private String ottBaseUrl;
    private int ottPartnerId;
    private String ovpBaseUrl;
    private int ovpPartnerId;
    private int uiConfId;
    private String ottAssetId;
    private String ottFileId;
    private String ovpEntryId;
    private boolean autoPlay;
    private boolean preload;
    private float startPosition;
    //private PluginConfig pluginConfig;


    public PlayerConfiguration() {
    }

    public PlayerConfiguration(String mediaProvider, String ottBaseUrl, int ottPartnerId, String ovpBaseUrl, int ovpPartnerId, int uiConfId, String ottAssetId, String ottFileId, String ovpEntryId, boolean autoPlay, boolean preload, float startPosition) {
        this.mediaProvider = mediaProvider;
        this.ottBaseUrl = ottBaseUrl;
        this.ottPartnerId = ottPartnerId;
        this.ovpBaseUrl = ovpBaseUrl;
        this.ovpPartnerId = ovpPartnerId;
        this.uiConfId = uiConfId;
        this.ottAssetId = ottAssetId;
        this.ottFileId = ottFileId;
        this.ovpEntryId = ovpEntryId;
        this.autoPlay = autoPlay;
        this.preload = preload;
        this.startPosition = startPosition;
    }

    public String getMediaProvider() {
        return mediaProvider;
    }

    public void setMediaProvider(String mediaProvider) {
        this.mediaProvider = mediaProvider;
    }

    public String getOttBaseUrl() {
        return ottBaseUrl;
    }

    public void setOttBaseUrl(String ottBaseUrl) {
        this.ottBaseUrl = ottBaseUrl;
    }

    public int getOttPartnerId() {
        return ottPartnerId;
    }

    public void setOttPartnerId(int ottPartnerId) {
        this.ottPartnerId = ottPartnerId;
    }

    public String getOvpBaseUrl() {
        return ovpBaseUrl;
    }

    public void setOvpBaseUrl(String ovpBaseUrl) {
        this.ovpBaseUrl = ovpBaseUrl;
    }

    public int getOvpPartnerId() {
        return ovpPartnerId;
    }

    public void setOvpPartnerId(int ovpPartnerId) {
        this.ovpPartnerId = ovpPartnerId;
    }

    public int getUiConfId() {
        return uiConfId;
    }

    public void setUiConfId(int uiConfId) {
        this.uiConfId = uiConfId;
    }

    public String getOttAssetId() {
        return ottAssetId;
    }

    public void setOttAssetId(String ottAssetId) {
        this.ottAssetId = ottAssetId;
    }

    public String getOttFileId() {
        return ottFileId;
    }

    public void setOttFileId(String ottFileId) {
        this.ottFileId = ottFileId;
    }

    public String getOvpEntryId() {
        return ovpEntryId;
    }

    public void setOvpEntryId(String ovpEntryId) {
        this.ovpEntryId = ovpEntryId;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public boolean isPreload() {
        return preload;
    }

    public void setPreload(boolean preload) {
        this.preload = preload;
    }

    public float getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(float startPosition) {
        this.startPosition = startPosition;
    }
}
