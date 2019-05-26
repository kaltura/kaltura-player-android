package com.kaltura.kalturaplayertestapp.models.ima;

import java.util.List;

public class AdsRenderingSettings {

    private Integer bitrate;
    private Integer loadVideoTimeout;
    private List<String> mimeTypes;
    private UiElements uiElements;

    public Integer getBitrate() {
        if (bitrate == null) {
            bitrate = -1;
        }
        return bitrate;
    }


    public Integer getLoadVideoTimeout() {
        if (loadVideoTimeout == null) {
            loadVideoTimeout = 0;
        }
        return loadVideoTimeout;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public UiElements getUiElements() {
        if (uiElements == null) {
            uiElements = new UiElements(true, true);
        }
        return uiElements;
    }
}
