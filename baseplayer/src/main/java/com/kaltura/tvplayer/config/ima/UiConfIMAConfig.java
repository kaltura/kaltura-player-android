/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.tvplayer.config.ima;


import android.text.TextUtils;

import com.kaltura.playkit.utils.Consts;

/**
 * Created by gilad.nadav on 17/11/2016.
 */

public class UiConfIMAConfig {

    private String adTagUrl;
    private AdsRenderingSettings adsRenderingSettings;
    private SdkSettings sdkSettings;

    public String getAdTagUrl() {
        return adTagUrl;
    }

    public AdsRenderingSettings getAdsRenderingSettings() {
        return adsRenderingSettings;
    }

    public SdkSettings getSdkSettings() {
        return sdkSettings;
    }

//    public void merge(UiConfIMAConfig imaConfigUiConf) {
//        if (imaConfigUiConf == null) {
//            return;
//        }
//
//        if (TextUtils.isEmpty(adTagURL)) {
//            this.adTagURL = imaConfigUiConf.getAdTagUrl();
//        }
//
//        if (imaConfigUiConf.getAdsRenderingSettings() != null) {
//            if (videoBitrate == -1 &&
//                    imaConfigUiConf.getAdsRenderingSettings().getBitrate() != null &&
//                    imaConfigUiConf.getAdsRenderingSettings().getBitrate() != -1 ) {
//                videoBitrate = imaConfigUiConf.getAdsRenderingSettings().getBitrate();
//            }
//
//            if (imaConfigUiConf.getAdsRenderingSettings().getUiElements() != null) {
//                if (imaConfigUiConf.getAdsRenderingSettings().getUiElements().isAdAttribution() == false) {
//                    adAttribution = false;
//                }
//                if (imaConfigUiConf.getAdsRenderingSettings().getUiElements().isAdCountDown() == false) {
//                    adCountDown = false;
//                }
//            }
//            if (imaConfigUiConf.getAdsRenderingSettings().getLoadVideoTimeout() != null &&
//                    imaConfigUiConf.getAdsRenderingSettings().getLoadVideoTimeout() != DEFAULT_AD_LOAD_TIMEOUT * Consts.MILLISECONDS_MULTIPLIER) {
//                adLoadTimeOut = imaConfigUiConf.getAdsRenderingSettings().getLoadVideoTimeout();
//            }
//        }
//
//        if (imaConfigUiConf.getSdkSettings() != null) {
//            if (imaConfigUiConf.getSdkSettings().isDebugMode() != false) {
//                enableDebugMode = false;
//            }
//
//            if (imaConfigUiConf.getSdkSettings().getLanguage() != null &&
//                    !language.equals(imaConfigUiConf.getSdkSettings().getLanguage())) {
//                language = imaConfigUiConf.getSdkSettings().getLanguage();
//            }
//        }
//
//        if (imaConfigUiConf.getAdsRenderingSettings().getMimeTypes() != null &&
//                imaConfigUiConf.getAdsRenderingSettings().getMimeTypes().size() > 0 &&
//                !imaConfigUiConf.getAdsRenderingSettings().getMimeTypes().get(0).equals(videoMimeTypes.get(0))) {
//            videoMimeTypes.set(0, imaConfigUiConf.getAdsRenderingSettings().getMimeTypes().get(0));
//        }
//        //  this.adTagType = AdTagType.VAST;
//    }
}


