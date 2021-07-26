package com.kaltura.tvplayer.offline.exo;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.util.NotificationUtil;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

class ExoNotificationHelper {

    private static final @StringRes int NULL_STRING_ID = 0;
    private final NotificationCompat.Builder notificationBuilder;

    public ExoNotificationHelper(Context context) {
        notificationBuilder = new NotificationCompat.Builder(context.getApplicationContext(), getDownloadChannelId());
    }

    public String getDownloadChannelId() {
        return Consts.EXO_DOWNLOAD_CHANNEL_ID;
    }

    /**
     * Returns a progress notification for the given downloads.
     *
     * @param context A context.
     * @param smallIcon A small icon for the notification.
     * @param contentIntent An optional content intent to send when the notification is clicked.
     * @param message An optional message to display on the notification.
     * @param downloads The downloads.
     * @return The notification.
     */
    protected Notification buildProgressNotification(
            Context context,
            @DrawableRes int smallIcon,
            @Nullable PendingIntent contentIntent,
            @Nullable String message,
            List<Download> downloads) {
        float totalPercentage = 0;
        int downloadTaskCount = 0;
        boolean allDownloadPercentagesUnknown = true;
        boolean haveDownloadedBytes = false;
        boolean haveDownloadTasks = false;
        boolean haveRemoveTasks = false;
        for (int i = 0; i < downloads.size(); i++) {
            Download download = downloads.get(i);
            if (download.state == Download.STATE_REMOVING) {
                haveRemoveTasks = true;
                continue;
            }
            if (download.state != Download.STATE_RESTARTING
                    && download.state != Download.STATE_DOWNLOADING) {
                continue;
            }
            haveDownloadTasks = true;
            float downloadPercentage = download.getPercentDownloaded();
            if (downloadPercentage != C.PERCENTAGE_UNSET) {
                allDownloadPercentagesUnknown = false;
                totalPercentage += downloadPercentage;
            }
            haveDownloadedBytes |= download.getBytesDownloaded() > 0;
            downloadTaskCount++;
        }

        int titleStringId =
                haveDownloadTasks
                        ? R.string.exo_download_downloading
                        : (haveRemoveTasks ? R.string.exo_download_removing : NULL_STRING_ID);
        int progress = 0;
        boolean indeterminate = true;
        if (haveDownloadTasks) {
            progress = (int) (totalPercentage / downloadTaskCount);
            indeterminate = allDownloadPercentagesUnknown && haveDownloadedBytes;
        }
        return buildNotification(
                context,
                smallIcon,
                contentIntent,
                message,
                titleStringId,
                /* maxProgress= */ 100,
                progress,
                indeterminate,
                /* ongoing= */ true,
                /* showWhen= */ false);
    }

    protected DownloadManager.Listener getDownloadManagerListener(Context context) {
        return new DownloadManager.Listener() {
            @Override
            public void onDownloadChanged(@NotNull DownloadManager downloadManager, @NonNull Download download, @Nullable Exception finalException) {
                Notification notification;
                if (download.state == Download.STATE_COMPLETED || (download.state == Download.STATE_STOPPED && download.state == StopReason.prefetchDone.toExoCode())) {
                    notification =
                            buildDownloadCompletedNotification(
                                    context,
                                    R.drawable.ic_cloud_done_black_24dp,
                                    null, 
                                    null);
                } else if (download.state == Download.STATE_FAILED) {
                    notification =
                            buildDownloadFailedNotification(
                                    context,
                                    R.drawable.ic_cloud_done_black_24dp,
                                    null,
                                    download.failureReason == 0 ?
                                            context.getString(R.string.download_failure_none) :
                                            context.getString(R.string.download_failure_unknown));
                } else {
                    return;
                }
                NotificationUtil.setNotification(context,  56324, notification);
            }
        };
    }

    /**
     * Returns a notification for a completed download.
     *
     * @param context A context.
     * @param smallIcon A small icon for the notifications.
     * @param contentIntent An optional content intent to send when the notification is clicked.
     * @param message An optional message to display on the notification.
     * @return The notification.
     */
    public Notification buildDownloadCompletedNotification(
            Context context,
            @DrawableRes int smallIcon,
            @Nullable PendingIntent contentIntent,
            @Nullable String message) {
        int titleStringId = R.string.exo_download_completed;
        return buildEndStateNotification(context, smallIcon, contentIntent, message, titleStringId);
    }

    /**
     * Returns a notification for a failed download.
     *
     * @param context A context.
     * @param smallIcon A small icon for the notifications.
     * @param contentIntent An optional content intent to send when the notification is clicked.
     * @param message An optional message to display on the notification.
     * @return The notification.
     */
    public Notification buildDownloadFailedNotification(
            Context context,
            @DrawableRes int smallIcon,
            @Nullable PendingIntent contentIntent,
            @Nullable String message) {
        @StringRes int titleStringId = R.string.exo_download_failed;
        return buildEndStateNotification(context, smallIcon, contentIntent, message, titleStringId);
    }

    private Notification buildEndStateNotification(
            Context context,
            @DrawableRes int smallIcon,
            @Nullable PendingIntent contentIntent,
            @Nullable String message,
            @StringRes int titleStringId) {
        return buildNotification(
                context,
                smallIcon,
                contentIntent,
                message,
                titleStringId,
                0,
                0,
                false,
                false,
                true);
    }

    private Notification buildNotification(
            Context context,
            @DrawableRes int smallIcon,
            @Nullable PendingIntent contentIntent,
            @Nullable String message,
            @StringRes int titleStringId,
            int maxProgress,
            int currentProgress,
            boolean indeterminateProgress,
            boolean ongoing,
            boolean showWhen) {
        notificationBuilder.setSmallIcon(smallIcon);
        notificationBuilder.setContentTitle(
                titleStringId == NULL_STRING_ID ? null : context.getResources().getString(titleStringId));
        notificationBuilder.setContentIntent(contentIntent);
        notificationBuilder.setStyle(
                message == null ? null : new NotificationCompat.BigTextStyle().bigText(message));
        notificationBuilder.setProgress(maxProgress, currentProgress, indeterminateProgress);
        notificationBuilder.setOngoing(ongoing);
        notificationBuilder.setShowWhen(showWhen);
        return notificationBuilder.build();
    }
}
