package com.kaltura.tvplayer.offline.exo;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.playkit.utils.Consts;

import java.util.List;

/**
 * Helper class to create single notification when the download is in progress 
 * when EXO offline download provider is used. 
 * No need to use this class to build notification if app uses DTG offline download provider.
 *
 * App has to use the {@link #getChannelID()} to create NotificationBuilder.
 * In case app uses any other ChannelId then app may see unusual behavior/crashes.
 *
 * The limitation here is that app can create only one notification. That's why in
 * {@link #buildNotification(Context, PendingIntent, int, List)} only one notification is being returned
 * instead of List of notifications for each download.
 * For more reference: https://github.com/google/ExoPlayer/issues/8297#issuecomment-736487275
 *
 * App can either take this single notification and show the progress with the accumulated progress on
 * one notification. OR else app can show one notification using this callback and for other download's
 * notification; app can use offline provider's call back to create other notification.
 * Again to have the channelId, app can use {@link Consts#EXO_DOWNLOAD_CHANNEL_ID}.
 * This channelId is same as{@link #getChannelID()}.
 *
 * To add your custom notification pass your implemented class in
 * {@link com.kaltura.tvplayer.OfflineManager#setForegroundNotification(ExoOfflineNotificationHelper)}
 */

public abstract class ExoOfflineNotificationHelper extends ExoNotificationHelper {

    public ExoOfflineNotificationHelper(Context context, String channelId) {
        super(context);
        if (!TextUtils.equals(channelId, super.getDownloadChannelId())) {
            throw new IllegalArgumentException("ChannelId mismatch. \n " +
                    "Use getChannelId() OR EXO_DOWNLOAD_CHANNEL_ID from Consts to get the correct channelId for Notification.");
        }
    }

    /**
     * A must getter to get the ChannelId to create NotificationBuilder
     * @return ChannelId
     */
    public String getChannelID() {
        return super.getDownloadChannelId();
    }

    /**
     * App can create it's own notification and return the notification
     *
     * @param context Context
     * @param contentIntent contentId what app wants if notification is clicked
     * @param notificationId NotificationId, it is fixed
     * @param downloads List of downloads
     * @return Single Notification
     */
    public abstract Notification buildNotification(Context context,@Nullable PendingIntent contentIntent, int notificationId, @NonNull List<Download> downloads);

    /**
     * A required downloadManager listener {@link DownloadManager.Listener}
     * @param context Context
     * @return listener
     */
    public abstract DownloadManager.Listener getDownloadManagerListener(Context context);

}
