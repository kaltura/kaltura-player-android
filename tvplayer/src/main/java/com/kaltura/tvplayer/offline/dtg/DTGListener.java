package com.kaltura.tvplayer.offline.dtg;

import com.kaltura.dtg.DownloadItem;
import com.kaltura.dtg.DownloadStateListener;

abstract class DTGListener implements DownloadStateListener {

    @Override
    public void onDownloadComplete(DownloadItem item) {

    }

    @Override
    public void onProgressChange(DownloadItem item, long downloadedBytes) {

    }

    @Override
    public void onDownloadStart(DownloadItem item) {

    }

    @Override
    public void onDownloadPause(DownloadItem item) {

    }

    @Override
    public void onDownloadFailure(DownloadItem item, Exception error) {

    }

    @Override
    public void onDownloadMetadata(DownloadItem item, Exception error) {

    }

    @Override
    public void onTracksAvailable(DownloadItem item, DownloadItem.TrackSelector trackSelector) {

    }
}
