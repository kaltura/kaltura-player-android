package com.kaltura.ptrescue;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrefetchSdk {

    private static final String TAG = "PrefetchSdk";
//    private static final String SERVICE_URL = "http://192.168.164.17/ptr.json";
    private static final String SERVICE_URL = "https://***REMOVED***.execute-api.eu-central-1.amazonaws.com/default/getEntriesForPrefetch";
    public static final String URI_STRING = "https://cdnapisec.kaltura.com/p/***REMOVED***/sp/***REMOVED***00/playManifest/entryId/1_aworxd15/format/applehttp/protocol/https/a.m3u8";
    private static PrefetchSdk shared;
    private final PrefetchDb db;
    private final Handler dbHandler;
    private final String userAgent;
    private final Context context;
    private DownloadManager downloadManager;
    private DownloadTracker downloadTracker;
    private File downloadDirectory;
    private Cache downloadCache;

    private static final String DOWNLOAD_ACTION_FILE = "actions";
    private static final String DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions";
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private static final int MAX_SIMULTANEOUS_DOWNLOADS = 2;


    /** Returns a {@link DataSource.Factory}. */
    public DataSource.Factory buildDataSourceFactory() {
        DefaultDataSourceFactory upstreamFactory =
                new DefaultDataSourceFactory(context, buildHttpDataSourceFactory());
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache());
    }

    /** Returns a {@link HttpDataSource.Factory}. */
    public HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(userAgent);
    }

    public DownloadManager getDownloadManager() {
        initDownloadManager();
        return downloadManager;
    }

    public DownloadTracker getDownloadTracker() {
        initDownloadManager();
        return downloadTracker;
    }

    private synchronized void initDownloadManager() {
        if (downloadManager == null) {
            DownloaderConstructorHelper downloaderConstructorHelper =
                    new DownloaderConstructorHelper(getDownloadCache(), buildHttpDataSourceFactory());
            downloadManager =
                    new DownloadManager(
                            downloaderConstructorHelper,
                            MAX_SIMULTANEOUS_DOWNLOADS,
                            DownloadManager.DEFAULT_MIN_RETRY_COUNT,
                            new File(getDownloadDirectory(), DOWNLOAD_ACTION_FILE));
            downloadTracker =
                    new DownloadTracker(
                            context,
                            buildDataSourceFactory(),
                            new File(getDownloadDirectory(), DOWNLOAD_TRACKER_ACTION_FILE));
            downloadManager.addListener(downloadTracker);
        }
    }

    private synchronized Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache = new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor());
        }
        return downloadCache;
    }

    private File getDownloadDirectory() {
        if (downloadDirectory == null) {
            downloadDirectory = context.getExternalFilesDir(null);
            if (downloadDirectory == null) {
                downloadDirectory = context.getFilesDir();
            }
        }
        return downloadDirectory;
    }

    private static CacheDataSourceFactory buildReadOnlyCacheDataSource(
            DefaultDataSourceFactory upstreamFactory, Cache cache) {
        return new CacheDataSourceFactory(
                cache,
                upstreamFactory,
                new FileDataSourceFactory(),
                /* cacheWriteDataSinkFactory= */ null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                /* eventListener= */ null);
    }
    public void reportWatchedEntry(@NonNull String entryId) {
        final long now = System.currentTimeMillis();
        dbHandler.post(() -> db.dao().insert(new WatchedEntry(entryId, now)));
    }

    public void prefetchNow(OnComplete<List<String>> onComplete) {
        dbHandler.post(() -> {
            try {
                final List<String> strings = prefetchNow();
                onComplete.accept(strings, null);
            } catch (JSONException | IOException e) {
                onComplete.accept(null, e);
            }
        });
    }

    private List<String> prefetchNow() throws JSONException, IOException {
        final List<String> strings = submit();

        prefetchEntries(strings);

        db.dao().clearHistory();

        return strings;
    }

    private void prefetchEntries(List<String> strings) {

        Log.d(TAG, "prefetching entries: " + strings);

        downloadTracker.startDownload("1_aworxd15", Uri.parse(URI_STRING));
    }

    private PrefetchSdk(Context context) {
        this.context = context.getApplicationContext();
        db = Room.databaseBuilder(this.context, PrefetchDb.class, "history").build();

        HandlerThread dbThread = new HandlerThread("dbThread");
        dbThread.start();
        dbHandler = new Handler(dbThread.getLooper());

        userAgent = Util.getUserAgent(this.context, "PrimeTimeDemo");

        initDownloadManager();


    }

    public static PrefetchSdk shared(Context context) {
        if (shared == null) {
            synchronized (PrefetchSdk.class) {
                if (shared == null) {
                    shared = new PrefetchSdk(context);
                }
            }
        }
        return shared;
    }

    private List<String> submit() throws JSONException, IOException {
        final List<WatchedEntry> all = db.dao().getAllWatched();

        JSONArray entries = new JSONArray();
        for (WatchedEntry watchedEntry : all) {
            entries.put(watchedEntry.entryId);
        }

        JSONObject request = new JSONObject()
                .put("entries", entries);

        final byte[] bytes = Utils.executePost(SERVICE_URL, request.toString().getBytes(), null);

        JSONObject response = new JSONObject(new String(bytes));
        List<String> entryIds = new ArrayList<>();
        JSONArray array = response.getJSONArray("entries");
        for (int i = 0, length = array.length(); i < length; i++) {
            final JSONObject object = array.getJSONObject(i);
            entryIds.add(object.getString("id"));
        }

        return entryIds;
    }

    public void install(Player player) {
        player.getSettings()
                .setOfflineDataSourceFactory(buildDataSourceFactory())
                .setOfflineStreamKeys(downloadTracker.getOfflineStreamKeys(Uri.parse(URI_STRING)));

    }

    public interface OnComplete<T> {
        void accept(T t, Exception e);
    }
}
