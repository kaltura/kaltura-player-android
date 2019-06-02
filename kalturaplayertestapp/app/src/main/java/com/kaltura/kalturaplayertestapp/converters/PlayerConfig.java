package com.kaltura.kalturaplayertestapp.converters;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.player.ABRSettings;
import com.kaltura.playkit.player.LoadControlBuffers;
import com.kaltura.playkit.player.PKAspectRatioResizeMode;
import com.kaltura.playkit.player.SubtitleStyleSettings;
import com.kaltura.playkit.player.vr.VRSettings;


import java.util.List;

/**
 * Created by gilad.nadav on 1/24/18.
 */

public class PlayerConfig {
    public String playerType;
    public String baseUrl;
    public String partnerId;
    public String ks;
    public UiConf uiConf;
    public int startPosition;
    public Boolean autoPlay;
    public Boolean preload;
    public Boolean allowCrossProtocolEnabled;
    public String  preferredFormat;
    public Boolean allowClearLead;
    public Boolean secureSurface;
    public Boolean adAutoPlayOnResume;
    public Boolean vrPlayerEnabled;
    public VRSettings vrSettings;
    public Boolean isVideoViewHidden;
    public SubtitleStyleSettings setSubtitleStyle;
    public PKAspectRatioResizeMode aspectRatioResizeMode;
    public PKRequestParams.Adapter contentRequestAdapter;
    public PKRequestParams.Adapter licenseRequestAdapter;
    public LoadControlBuffers loadControlBuffers;
    public ABRSettings abrSettings;
    public String referrer;
    public List<Media> mediaList;
    public TrackSelection trackSelection;
    public JsonArray plugins;
    public JsonObject playerConfig;

    public PlayerConfig() {}
}
