package com.kaltura.kalturaplayertestapp.models.ima;

import java.util.List;

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
