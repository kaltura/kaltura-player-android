package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.net.Uri;
import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.database.ExoDatabaseProvider;
import com.kaltura.android.exoplayer2.drm.*;
import com.kaltura.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.offline.*;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.FileDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.cache.*;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.tvplayer.OfflineManager;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class ExoAssetInfo extends OfflineManager.AssetInfo {

    private final PKMediaSource source;
    private final PKDrmParams drmData;

    ExoAssetInfo(PKMediaSource source, PKDrmParams drmData) {
        this.source = source;
        this.drmData = drmData;
    }
}

public class ExoOfflineManager extends AbstractOfflineManager {

    private static final PKLog log = PKLog.get("ExoOfflineManager");
    private static String DOWNLOAD_CONTENT_DIRECTORY = "downloads";

    private static ExoOfflineManager instance;

    private final String userAgent = Util.getUserAgent(appContext, "ExoDownload");

    private final OkHttpDataSourceFactory httpDataSourceFactory = new OkHttpDataSourceFactory(new OkHttpClient(), userAgent);

    private DatabaseProvider databaseProvider;
    private File downloadDirectory;
    private Cache downloadCache;
    DownloadManager downloadManager;

    private Map<String, ExoAssetInfo> newAssets = new HashMap<>();






    private ExoOfflineManager(Context context) {
        super(context);
        downloadDirectory = context.getExternalFilesDir(null);
        if (downloadDirectory == null) {
            downloadDirectory = context.getFilesDir();
        }

        databaseProvider = new ExoDatabaseProvider(context);

        File downloadContentDirectory = new File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY);
        downloadCache = new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), databaseProvider);

        DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(databaseProvider);

        DownloaderConstructorHelper downloaderConstructorHelper = new DownloaderConstructorHelper(downloadCache, httpDataSourceFactory);

        downloadManager = new DownloadManager(
                context,
                downloadIndex,
                new DefaultDownloaderFactory(downloaderConstructorHelper)
        );

        downloadManager.setMaxParallelDownloads(4);

    }

    private CacheDataSourceFactory buildReadOnlyCacheDataSource(DataSource.Factory upstreamFactory, Cache cache) {

        return new CacheDataSourceFactory(
                cache,
                upstreamFactory,
                new FileDataSourceFactory(),
                null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null
        );
    }


    @Override
    public void prepareAsset(PKMediaEntry mediaEntry, SelectionPrefs prefs, PrepareCallback prepareCallback) {

        SourceSelector selector = new SourceSelector(mediaEntry, null);

        final PKMediaSource source = selector.getSelectedSource();
        final PKDrmParams drmData = selector.getSelectedDrmParams();
        final PKMediaFormat mediaFormat = source.getMediaFormat();
        final String url = source.getUrl();

        if (mediaFormat != PKMediaFormat.dash) {
            throw new IllegalArgumentException("Only DASH, for now");
        }

        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = buildDrmSessionManager(drmData);
        final DownloadHelper downloadHelper = DownloadHelper.forDash(Uri.parse(url), httpDataSourceFactory,
                new DefaultRenderersFactory(appContext), drmSessionManager, buildExoParameters(prefs));

        downloadHelper.prepare(new DownloadHelper.Callback() {
            @Override
            public void onPrepared(DownloadHelper helper) {
                final DownloadRequest downloadRequest = helper.getDownloadRequest(null);

                DownloadService.sendAddDownload(appContext, ExoDownloadService.class, downloadRequest, false);

                helper.release();

                prepareCallback.onPrepared(new ExoAssetInfo(source, drmData), null, -1);
            }

            @Override
            public void onPrepareError(DownloadHelper helper, IOException e) {
                if (e != null) {
                    log.e("onPrepareError", e);
                }
                helper.release();
            }
        });
    }

    @Nullable
    private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(PKDrmParams drmData) {
        if (drmData == null) {
            return null;
        }

        if (drmData.getScheme() != PKDrmParams.Scheme.WidevineCENC) {
            throw new IllegalArgumentException("Only Widevine CENC");
        }

        String licenseUrl = drmData.getLicenseUri();
        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
        try {
            final HttpMediaDrmCallback mediaDrmCallback = new HttpMediaDrmCallback(licenseUrl, httpDataSourceFactory);
            drmSessionManager = DefaultDrmSessionManager.newWidevineInstance(mediaDrmCallback, null);
        } catch (UnsupportedDrmException e) {
            e.printStackTrace();
        }
        return drmSessionManager;
    }

    private DefaultTrackSelector.Parameters buildExoParameters(SelectionPrefs prefs) {
        return DefaultTrackSelector.Parameters.DEFAULT;// TODO: 2019-07-31
    }

    public static ExoOfflineManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ExoOfflineManager.class) {
                if (instance == null) {
                    instance = new ExoOfflineManager(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void pauseDownloads() {
        // TODO: 29/01/2018 DTG stop
    }

    @Override
    public void resumeDownloads() {
        // TODO: 29/01/2018 DTG resume
    }

    @Override
    public AssetInfo getAssetInfo(String assetId) {
        return null;
    }

    @Override
    public List<AssetInfo> getAssetsInState(AssetDownloadState state) {
        // TODO: 15/02/2018 DTG get downloads
        return null;
    }

    @Override
    public PKMediaEntry getLocalPlaybackEntry(String assetId) {
        // TODO: 28/01/2018 DTG local file and LocalAssetManager local source
        return null;
    }

    @Override
    public boolean addAsset(AssetInfo assetInfo) {
        return false;
    }

    @Override
    public boolean startAssetDownload(String assetId) {
        // TODO: 29/01/2018 DTG start item. IF ANOTHER ITEM IS IN PROGRESS, DON'T START yet.
        return false;
    }

    @Override
    public boolean pauseAssetDownload(String assetId) {
        // TODO: 29/01/2018 DTG pause item.
        return false;
    }

    @Override
    public boolean removeAsset(String assetId) {
        // TODO: 29/01/2018 DTG remove item. LAM unregister.
        return false;
    }

    @Override
    public DrmInfo getDrmStatus(String assetId) {
        // TODO: 29/01/2018 LAM check status.
        return null;
    }

    @Override
    public boolean registerDrmAsset(String assetId, DrmRegisterListener listener) {
        return false;
    }

    @Override
    public boolean registerDrmAsset(String assetId, PKDrmParams drmParams, DrmRegisterListener listener) {
        return false;
    }
}
