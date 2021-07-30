package com.kaltura.tvplayer.offline;

import com.kaltura.dtg.DownloadRequestParams;
import com.kaltura.playkit.PKRequestParams;

public class OfflineManagerSettings {
    public static final int DEFAULT_HLS_AUDIO_BITRATE_ESTIMATION = 64000;
    public static final int MAX_PARALLEL_DOWNLOADS = 4;
    public static final int MIN_RETRY_COUNT = 5;
    public static final boolean CREATE_NO_MEDIA_FILE = true;
    public static final boolean CROSS_PROTOCOL_ENABLED = true;

    private int maxDownloadRetries = MIN_RETRY_COUNT;
    private int httpTimeoutMillis = 15000;
    private int maxConcurrentDownloads = MAX_PARALLEL_DOWNLOADS;
    private int hlsAudioBitrateEstimation = DEFAULT_HLS_AUDIO_BITRATE_ESTIMATION;
    private long freeDiskSpaceRequiredBytes = 400 * 1024 * 1024; // default 400MB and only relevant for DTG
    private String applicationName = "";
    private boolean createNoMediaFileInDownloadsDir = CREATE_NO_MEDIA_FILE;
    private boolean crossProtocolRedirectEnabled = CROSS_PROTOCOL_ENABLED;
    private DownloadRequestParams.Adapter downloadRequestAdapter;
    private DownloadRequestParams.Adapter chunksUrlAdapter;
    private PKRequestParams.Adapter licenseRequestAdapter;

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

    public int getHlsAudioBitrateEstimation() {
        return hlsAudioBitrateEstimation;
    }

    public OfflineManagerSettings setHlsAudioBitrateEstimation(int hlsAudioBitrateEstimation) {
        this.hlsAudioBitrateEstimation = hlsAudioBitrateEstimation;
        return this;
    }

    public long getFreeDiskSpaceRequiredBytes() {
        return freeDiskSpaceRequiredBytes;
    }

    public OfflineManagerSettings setFreeDiskSpaceRequiredBytes(long freeDiskSpaceRequiredBytes) {
        this.freeDiskSpaceRequiredBytes = freeDiskSpaceRequiredBytes;
        return this;
    }

    public boolean isCrossProtocolRedirectEnabled() {
        return crossProtocolRedirectEnabled;
    }

    public OfflineManagerSettings setCrossProtocolRedirectEnabled(boolean crossProtocolRedirectEnabled) {
        this.crossProtocolRedirectEnabled = crossProtocolRedirectEnabled;
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

    public PKRequestParams.Adapter getLicenseRequestAdapter() {
        return licenseRequestAdapter;
    }

    public void setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter) {
        this.licenseRequestAdapter = licenseRequestAdapter;
    }
}
