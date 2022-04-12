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
import com.kaltura.android.exoplayer2.scheduler.Requirements;
import com.kaltura.android.exoplayer2.util.NotificationUtil;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

class ExoNotificationHelper {

    private static final @StringRes int NULL_STRING_ID = 0;
    private final NotificationCompat.Builder notificationBuilder;
    private final int NOTIFICATION_ID = 56324;

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
     * @param notMetRequirements Any requirements for downloads that are not currently met.
     * @return The notification.
     */
    public Notification buildProgressNotification(
            Context context,
            @DrawableRes int smallIcon,
            @Nullable PendingIntent contentIntent,
            @Nullable String message,
            List<Download> downloads,
            @Requirements.RequirementFlags int notMetRequirements) {
        float totalPercentage = 0;
        int downloadTaskCount = 0;
        boolean allDownloadPercentagesUnknown = true;
        boolean haveDownloadedBytes = false;
        boolean haveDownloadingTasks = false;
        boolean haveQueuedTasks = false;
        boolean haveRemovingTasks = false;
        for (int i = 0; i < downloads.size(); i++) {
            Download download = downloads.get(i);
            switch (download.state) {
                case Download.STATE_REMOVING:
                    haveRemovingTasks = true;
                    break;
                case Download.STATE_QUEUED:
                    haveQueuedTasks = true;
                    break;
                case Download.STATE_RESTARTING:
                case Download.STATE_DOWNLOADING:
                    haveDownloadingTasks = true;
                    float downloadPercentage = download.getPercentDownloaded();
                    if (downloadPercentage != C.PERCENTAGE_UNSET) {
                        allDownloadPercentagesUnknown = false;
                        totalPercentage += downloadPercentage;
                    }
                    haveDownloadedBytes |= download.getBytesDownloaded() > 0;
                    downloadTaskCount++;
                    break;
                // Terminal states aren't expected, but if we encounter them we do nothing.
                case Download.STATE_STOPPED:
                case Download.STATE_COMPLETED:
                case Download.STATE_FAILED:
                default:
                    break;
            }
        }

        int titleStringId;
        boolean showProgress = true;
        if (haveDownloadingTasks) {
            titleStringId = R.string.exo_download_downloading;
        } else if (haveQueuedTasks && notMetRequirements != 0) {
            showProgress = false;
            if ((notMetRequirements & Requirements.NETWORK_UNMETERED) != 0) {
                // Note: This assumes that "unmetered" == "WiFi", since it provides a clearer message that's
                // correct in the majority of cases.
                titleStringId = R.string.exo_download_paused_for_wifi;
            } else if ((notMetRequirements & Requirements.NETWORK) != 0) {
                titleStringId = R.string.exo_download_paused_for_network;
            } else {
                titleStringId = R.string.exo_download_paused;
            }
        } else if (haveRemovingTasks) {
            titleStringId = R.string.exo_download_removing;
        } else {
            // There are either no downloads, or all downloads are in terminal states.
            titleStringId = NULL_STRING_ID;
        }

        int maxProgress = 0;
        int currentProgress = 0;
        boolean indeterminateProgress = false;
        if (showProgress) {
            maxProgress = 100;
            if (haveDownloadingTasks) {
                currentProgress = (int) (totalPercentage / downloadTaskCount);
                indeterminateProgress = allDownloadPercentagesUnknown && haveDownloadedBytes;
            } else {
                indeterminateProgress = true;
            }
        }

        return buildNotification(
                context,
                smallIcon,
                contentIntent,
                message,
                titleStringId,
                maxProgress,
                currentProgress,
                indeterminateProgress,
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
                NotificationUtil.setNotification(context, NOTIFICATION_ID, notification);
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
