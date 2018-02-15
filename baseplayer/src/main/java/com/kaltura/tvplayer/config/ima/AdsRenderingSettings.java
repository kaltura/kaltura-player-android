package com.kaltura.tvplayer.config.ima;

import java.util.List;

/**
 * Created by gilad.nadav on 2/8/18.
 */

public class AdsRenderingSettings {

    private Integer bitrate;
    private Integer loadVideoTimeout;
    private List<String> mimeTypes;
    private UiElements uiElements;

    public Integer getBitrate() {
        return bitrate;
    }


    public Integer getLoadVideoTimeout() {
        return loadVideoTimeout;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public UiElements getUiElements() {
        return uiElements;
    }
}
