package com.kaltura.tvplayer.utils;

import android.content.Context;
import android.net.Uri;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.kaltura.playkit.Utils.toBase64;

public class NetworkUtils {
    private static final PKLog log = PKLog.get("NetworkUtils");

    public static void sendKavaImpression(Context context) {
        OkHttpClient client = new OkHttpClient();
        String kavaImpressionUrl = buildKavaImpressionUrl(context);
        try {
            Request request = new Request.Builder()
                    .url(kavaImpressionUrl)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.e("KavaImpression called failed url=" + kavaImpressionUrl + ", error=" + e.getMessage());
                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        log.e("KavaImpression called failed url=" + kavaImpressionUrl);
                    }
                }
            });
        } catch (Exception e) {
            log.e("KavaImpression called failed url=" + kavaImpressionUrl + ", error=" + e.getMessage());
        }
    }

    public static String buildKavaImpressionUrl(Context context) {
        Uri builtUri = Uri.parse(KavaAnalyticsConfig.DEFAULT_BASE_URL).buildUpon()
                .appendQueryParameter("service", "analytics")
                .appendQueryParameter("action", "trackEvent")
                .appendQueryParameter("eventType", "1")
                .appendQueryParameter("partnerId", String.valueOf(KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID))
                .appendQueryParameter("entryId", KavaAnalyticsConfig.DEFAULT_KAVA_ENTRY_ID)
                .appendQueryParameter("sessionId", generateSessionId())
                .appendQueryParameter("eventIndex", "1")
                .appendQueryParameter("referrer", toBase64(context.getPackageName().getBytes()))
                .appendQueryParameter("deliveryType", "gilad")
                .appendQueryParameter("playbackType", "vod")
                .appendQueryParameter("clientVer", PlayKitManager.CLIENT_TAG)
                .appendQueryParameter("position", "0")
                .appendQueryParameter("application", context.getPackageName())
                .build();
        return builtUri.toString();
    }

    public static String generateSessionId() {
        String mediaSessionId = UUID.randomUUID().toString();
        String newSessionId   = UUID.randomUUID().toString();
        newSessionId += ":";
        newSessionId += mediaSessionId;
        return newSessionId;
    }

}
