package com.kaltura.tvplayer.offline;

import com.kaltura.dtg.DownloadRequestParams;

public class OfflineManagerSettings {

    private int maxDownloadRetries = 5;
    private int httpTimeoutMillis = 15000;
    private int maxConcurrentDownloads = 4;
    private int defaultHlsAudioBitrateEstimation = 64000;
    private long freeDiskSpaceRequiredBytes = 400 * 1024 * 1024; // default 400MB
    private String applicationName = "";
    private boolean createNoMediaFileInDownloadsDir = true;
    private boolean crossProtocolRedirectEnabled = true;
    private DownloadRequestParams.Adapter downloadRequestAdapter;
    private DownloadRequestParams.Adapter chunksUrlAdapter;

    public int getMaxDownloadRetries() {
        return maxDownloadRetries;
    }

    public OfflineManagerSettings setMaxDownloadRetries(int maxDownloadRetries) {
        this.maxDownloadRetries = maxDownloadRetries;
        return this;
    }

    public int getHttpTimeoutMillis() {
        return httpTimeoutMillis;
    }

    public OfflineManagerSettings setHttpTimeoutMillis(int httpTimeoutMillis) {
        this.httpTimeoutMillis = httpTimeoutMillis;
        return this;
    }

    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    public OfflineManagerSettings setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
        return this;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public OfflineManagerSettings setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public boolean isCreateNoMediaFileInDownloadsDir() {
        return createNoMediaFileInDownloadsDir;
    }

    public OfflineManagerSettings setCreateNoMediaFileInDownloadsDir(boolean createNoMediaFileInDownloadsDir) {
        this.createNoMediaFileInDownloadsDir = createNoMediaFileInDownloadsDir;
        return this;
    }

    public int getDefaultHlsAudioBitrateEstimation() {
        return defaultHlsAudioBitrateEstimation;
    }

    public OfflineManagerSettings setDefaultHlsAudioBitrateEstimation(int defaultHlsAudioBitrateEstimation) {
        this.defaultHlsAudioBitrateEstimation = defaultHlsAudioBitrateEstimation;
        return this;
    }

    public long getFreeDiskSpaceRequiredBytes() {
        return freeDiskSpaceRequiredBytes;
    }

    public OfflineManagerSettings setFreeDiskSpaceRequiredBytes(long freeDiskSpaceRequiredBytes) {
        this.freeDiskSpaceRequiredBytes = freeDiskSpaceRequiredBytes;
        return this;
    }

    public DownloadRequestParams.Adapter getDownloadRequestAdapter() {
        return downloadRequestAdapter;
    }

    public OfflineManagerSettings setDownloadRequestAdapter(DownloadRequestParams.Adapter downloadRequestAdapter) {
        this.downloadRequestAdapter = downloadRequestAdapter;
        return this;
    }

    public DownloadRequestParams.Adapter getChunksUrlAdapter() {
        return chunksUrlAdapter;
    }

    public OfflineManagerSettings setChunksUrlAdapter(DownloadRequestParams.Adapter chunksUrlAdapter) {
        this.chunksUrlAdapter = chunksUrlAdapter;
        return this;
    }

    public boolean isCrossProtocolRedirectEnabled() {
        return crossProtocolRedirectEnabled;
    }

    public OfflineManagerSettings setCrossProtocolRedirectEnabled(boolean crossProtocolRedirectEnabled) {
        this.crossProtocolRedirectEnabled = crossProtocolRedirectEnabled;
        return this;
    }
}
