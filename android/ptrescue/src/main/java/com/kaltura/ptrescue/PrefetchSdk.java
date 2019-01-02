package com.kaltura.ptrescue;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.kaltura.playkit.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrefetchSdk {

    private static final String TAG = "PrefetchSdk";
//    private static final String SERVICE_URL = "http://192.168.164.17/ptr.json";
    private static final String SERVICE_URL = "https://***REMOVED***.execute-api.eu-central-1.amazonaws.com/default/getEntriesForPrefetch";
    private static PrefetchSdk shared;
    private final PrefetchDb db;
    private final Handler dbHandler;

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

        // TODO: 02/01/2019 implement
    }

    private PrefetchSdk(Context context) {
        db = Room.databaseBuilder(context.getApplicationContext(), PrefetchDb.class, "history").build();

        HandlerThread dbThread = new HandlerThread("dbThread");
        dbThread.start();
        dbHandler = new Handler(dbThread.getLooper());

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
            entryIds.add(object.getString("entryId"));
//            entryIds.add(object.getString("id"));
        }

        return entryIds;
    }

    public interface OnComplete<T> {
        void accept(T t, Exception e);
    }
}
