package com.kaltura.tvplayer.offline;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.DefaultRenderersFactory;
import com.kaltura.android.exoplayer2.database.DatabaseProvider;
import com.kaltura.android.exoplayer2.database.ExoDatabaseProvider;
import com.kaltura.android.exoplayer2.drm.*;
import com.kaltura.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.kaltura.android.exoplayer2.offline.*;
import com.kaltura.android.exoplayer2.source.MediaSource;
import com.kaltura.android.exoplayer2.source.dash.DashUtil;
import com.kaltura.android.exoplayer2.source.dash.manifest.DashManifest;
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.kaltura.android.exoplayer2.upstream.DataSource;
import com.kaltura.android.exoplayer2.upstream.FileDataSourceFactory;
import com.kaltura.android.exoplayer2.upstream.cache.*;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.*;
import com.kaltura.playkit.player.SourceSelector;
import com.kaltura.tvplayer.OfflineManager;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;


class ExoAssetInfo extends OfflineManager.AssetInfo {

    final PKMediaSource source;
    final PKDrmParams drmData;
    final DownloadHelper downloadHelper;

    ExoAssetInfo(String assetId, OfflineManager.AssetDownloadState state, long estimatedSize, long downloadedSize, PKMediaSource source, PKDrmParams drmData, DownloadHelper downloadHelper) {
        super(assetId, state, estimatedSize, downloadedSize);
        this.source = source;
        this.drmData = drmData;
        this.downloadHelper = downloadHelper;
    }

    @Override
    public void release() {
        if (downloadHelper != null) {
            downloadHelper.release();
        }
    }
}

public class ExoOfflineManager extends AbstractOfflineManager {

    private static final PKLog log = PKLog.get("ExoOfflineManager");
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";

    private static ExoOfflineManager instance;

    private final String userAgent = Util.getUserAgent(appContext, "ExoDownload");

    private final OkHttpDataSourceFactory httpDataSourceFactory = new OkHttpDataSourceFactory(new OkHttpClient(), userAgent);

    private final DatabaseProvider databaseProvider;
    private final File downloadDirectory;
    final Cache downloadCache;
    final DownloadManager downloadManager;


    private ExoOfflineManager(Context context) {
        super(context);
        final File externalFilesDir = context.getExternalFilesDir(null);
        downloadDirectory = externalFilesDir != null ? externalFilesDir : context.getFilesDir();

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

        final String assetId = mediaEntry.getId();
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

                final ExoAssetInfo assetInfo = new ExoAssetInfo(assetId, AssetDownloadState.prepared, -1, 0, source, drmData, helper);
                prepareCallback.onPrepared(assetInfo, null);
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
            throw new IllegalArgumentException("Only WidevineCENC");
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
        return new DefaultTrackSelector.ParametersBuilder().setMaxVideoSizeSd().build();
//        return DefaultTrackSelector.Parameters.DEFAULT;// TODO: 2019-07-31
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
    public PKMediaEntry getLocalPlaybackEntry(String assetId) throws IOException {

        final Download download = downloadManager.getDownloadIndex().getDownload(assetId);

        if (download == null || download.state != Download.STATE_COMPLETED) {
            return null;
        }

        final CacheDataSourceFactory dataSourceFactory = new CacheDataSourceFactory(downloadCache, httpDataSourceFactory);
        final MediaSource mediaSource = DownloadHelper.createMediaSource(download.request, dataSourceFactory);

        final PKMediaSource localMediaSource = localAssetsManager.getLocalMediaSource(assetId, mediaSource);
        return new PKMediaEntry().setId(assetId).setSources(Collections.singletonList(localMediaSource));
    }

    @Override
    public boolean addAsset(AssetInfo assetInfo) {

        if (assetInfo instanceof ExoAssetInfo) {
            return addExoAsset(((ExoAssetInfo) assetInfo));
        }
        return false;
    }

    private boolean addExoAsset(ExoAssetInfo assetInfo) {
        final DownloadHelper helper = assetInfo.downloadHelper;
        final DownloadRequest downloadRequest = helper.getDownloadRequest(assetInfo.id, null);

        DownloadService.sendAddDownload(appContext, ExoDownloadService.class, downloadRequest, false);

        return true;
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
    public boolean registerDrmAsset(AssetInfo assetInfo, DrmRegisterListener listener) {

        if (!(assetInfo instanceof ExoAssetInfo)) {
            return false;
        }

        final ExoAssetInfo exoAssetInfo = (ExoAssetInfo) assetInfo;

        final String sourceUrl = exoAssetInfo.source.getUrl();

        final CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(downloadCache, httpDataSourceFactory);
        final byte[] drmInitData;
        try {
            drmInitData = getDrmInitData(cacheDataSourceFactory, sourceUrl);
        } catch (IOException | InterruptedException e) {
            listener.onRegisterError(exoAssetInfo.id, e);
            return false;
        }

        localAssetsManager.registerDrmAsset(drmInitData, PKMediaFormat.dash.mimeType, exoAssetInfo.id, PKMediaFormat.dash, exoAssetInfo.drmData.getLicenseUri(), new LocalAssetsManager.AssetRegistrationListener() {
            @Override
            public void onRegistered(String localAssetPath) {
                listener.onRegistered(exoAssetInfo.id, null);// TODO: 2019-08-01 get drm info
            }

            @Override
            public void onFailed(String localAssetPath, Exception error) {
                listener.onRegisterError(exoAssetInfo.id, error);
            }
        });

        return false;
    }

    @Override
    public boolean renewDrmAsset(String assetId, PKDrmParams drmParams, DrmRegisterListener listener) {
        return false;
    }

    private byte[] getDrmInitData(CacheDataSourceFactory dataSourceFactory, String contentUri) throws IOException, InterruptedException {
        final CacheDataSource cacheDataSource = dataSourceFactory.createDataSource();
        final DashManifest dashManifest = DashUtil.loadManifest(cacheDataSource, Uri.parse(contentUri));
        final DrmInitData drmInitData = DashUtil.loadDrmInitData(cacheDataSource, dashManifest.getPeriod(0));

        final DrmInitData.SchemeData schemeData = drmInitData.get(C.WIDEVINE_UUID);
        return schemeData != null ? schemeData.data : null;
    }

}
