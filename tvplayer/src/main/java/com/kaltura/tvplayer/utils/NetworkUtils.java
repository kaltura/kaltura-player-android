package com.kaltura.tvplayer.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;
import com.kaltura.tvplayer.PlayerConfigManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.kaltura.playkit.Utils.toBase64;

public class NetworkUtils {
    private static final PKLog log = PKLog.get("NetworkUtils");
    private static OkHttpClient client = new OkHttpClient();
    public static final String KALTURA_PLAYER = "com.kaltura.player";
    public static final String UDID = "kaltura-player-android/4.0.0";

    public static void requestOvpConfigByPartnerId(Context context, String baseUrl, int partnerId, PlayerConfigManager.InternalCallback callback) {

        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "partner");
        params.put("action", "getPublicInfo");
        params.put("id", String.valueOf(partnerId));
        params.put("format", "1");

        String configByPartnerIdUrl = buildConfigByPartnerIdUrl(context, baseUrl + OvpConfigs.ApiPrefix, params);
        //log.d("ovp configByPartnerIdUrl = " + configByPartnerIdUrl);
        executeGETRequest(context, "requestOvpConfigByPartnerId", configByPartnerIdUrl, callback);
    }

    public static void requestOttConfigByPartnerId(Context context, String baseUrl, int partnerId, PlayerConfigManager.InternalCallback callback) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "Configurations");
        params.put("action", "serveByDevice");
        params.put("partnerId", String.valueOf(partnerId));
        params.put("applicationName", KALTURA_PLAYER + "." + partnerId);
        params.put("clientVersion", "4");
        params.put("platform", "Android");
        params.put("tag",  "tag");
        params.put("udid", UDID);

        String configByPartnerIdUrl = buildConfigByPartnerIdUrl(context, baseUrl, params);
        //log.d("ott configByPartnerIdUrl = " + configByPartnerIdUrl);
        executeGETRequest(context, "requestOttConfigByPartnerId", configByPartnerIdUrl, callback);
    }

    private static String buildConfigByPartnerIdUrl(Context context, String baseUrl, Map<String, String> params) {

        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
        Set<String> keys = params.keySet();
        if (keys != null) {
            for (String key : keys) {
                builder.appendQueryParameter(key, params.get(key));
            }
        }
        return builder.build().toString();
    }

    public static void sendKavaImpression(Context context) {
        OkHttpClient client = new OkHttpClient();
        String kavaImpressionUrl = buildKavaImpressionUrl(context);
        executeGETRequest(context, "sendKavaImpression", kavaImpressionUrl, null);
    }

    public static String buildKavaImpressionUrl(Context context) {
        Uri.Builder builtUri = Uri.parse(KavaAnalyticsConfig.DEFAULT_BASE_URL).buildUpon();
        builtUri.appendQueryParameter("service", "analytics")
                .appendQueryParameter("action", "trackEvent")
                .appendQueryParameter("eventType", "1")
                .appendQueryParameter("partnerId", String.valueOf(KavaAnalyticsConfig.DEFAULT_KAVA_PARTNER_ID))
                .appendQueryParameter("entryId", KavaAnalyticsConfig.DEFAULT_KAVA_ENTRY_ID)
                .appendQueryParameter("sessionId", generateSessionId())
                .appendQueryParameter("eventIndex", "1")
                .appendQueryParameter("referrer", toBase64(context.getPackageName().getBytes()))
                .appendQueryParameter("deliveryType", "dash")
                .appendQueryParameter("playbackType", "vod")
                .appendQueryParameter("clientVer", PlayKitManager.CLIENT_TAG)
                .appendQueryParameter("position", "0")
                .appendQueryParameter("application", context.getPackageName());
        return builtUri.build().toString();
    }

    public static String generateSessionId() {
        String mediaSessionId = UUID.randomUUID().toString();
        String newSessionId   = UUID.randomUUID().toString();
        newSessionId += ":";
        newSessionId += mediaSessionId;
        return newSessionId;
    }

    private static void executeGETRequest(Context context, String apiName, String configByPartnerIdUrl, PlayerConfigManager.InternalCallback callback) {
        try {
            Request request = new Request.Builder()
                    .url(configByPartnerIdUrl)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                Handler mainHandler = new Handler(context.getMainLooper());
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> {
                        sendError(callback, apiName + " called failed url = " + configByPartnerIdUrl + ", error = " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, final Response response) {

                    if (response == null || !response.isSuccessful()) {
                        mainHandler.post(() -> {
                            sendError(callback, apiName + " call failed url = " + configByPartnerIdUrl);
                        });
                        return;
                    } else {
                        try {
                            ResponseBody responseBody = response.body();
                            if (responseBody != null) {
                                String body = responseBody.string();
                                if (body != null && !body.contains("KalturaAPIException")) {
                                    mainHandler.post(() -> {
                                        if (callback != null) {
                                            callback.finished(body, null);
                                        }
                                    });
                                    return;
                                }
                            }
                        } catch(IOException e){
                            mainHandler.post(() -> {
                                sendError(callback, apiName + " call failed url = " + configByPartnerIdUrl + ", error = " + e.getMessage());
                            });
                            return;
                        }

                        mainHandler.post(() -> {
                            sendError(callback, apiName + " called failed url = " + configByPartnerIdUrl);
                        });
                    }
                }
            });
        } catch (Exception e) {
            sendError(callback, apiName + " call failed url = " + configByPartnerIdUrl + ", error = " + e.getMessage());
        }
    }

    private static void sendError(PlayerConfigManager.InternalCallback callback, String errorMessage) {
        log.e(errorMessage);
        if (callback != null) {
            callback.finished(null, ErrorElement.GeneralError);
        }
    }
}
