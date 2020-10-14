package com.kaltura.tvplayer.offline.exo;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloadService;
import com.kaltura.android.exoplayer2.scheduler.PlatformScheduler;
import com.kaltura.android.exoplayer2.ui.DownloadNotificationHelper;
import com.kaltura.android.exoplayer2.util.NotificationUtil;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.tvplayer.R;

import java.util.List;

public class ExoDownloadService extends DownloadService {

    private static final String CHANNEL_ID = "download_channel";

    private static final int JOB_ID = 1;
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    public ExoDownloadService() {
        super(
                FOREGROUND_NOTIFICATION_ID,
                DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
                CHANNEL_ID,
                R.string.exo_download_notification_channel_name,
                /* channelDescriptionResourceId= */ 0);
    }

    @Override
    @NonNull
    protected DownloadManager getDownloadManager() {
        return ExoOfflineManager.getInstance(this).downloadManager;
    }

    @Override
    protected PlatformScheduler getScheduler() {
        return Util.SDK_INT >= 21 ? new PlatformScheduler(this, JOB_ID) : null;
    }

    @Override
    @NonNull
    protected Notification getForegroundNotification(@NonNull List<Download> downloads) {
        return getDownloadNotificationHelper(/* context= */ this)
                .buildProgressNotification(
                        /* context= */ this,
                        R.drawable.ic_cloud_download_black_24dp,
                        /* contentIntent= */ null,
                        /* message= */ null,
                        downloads);
    }

    public static synchronized DownloadNotificationHelper getDownloadNotificationHelper(
            Context context) {
        return new DownloadNotificationHelper(context, CHANNEL_ID);
    }


    /**
     * Creates and displays notifications for downloads when they complete or fail.
     *
     * <p>This helper will outlive the lifespan of a single instance of {@link ExoDownloadService}.
     * It is static to avoid leaking the first {@link ExoDownloadService} instance.
     */
    private static final class TerminalStateNotificationHelper implements DownloadManager.Listener {

        private final Context context;
        private final DownloadNotificationHelper notificationHelper;

        private int nextNotificationId;

        public TerminalStateNotificationHelper(
                Context context, DownloadNotificationHelper notificationHelper, int firstNotificationId) {
            this.context = context.getApplicationContext();
            this.notificationHelper = notificationHelper;
            nextNotificationId = firstNotificationId;
        }

        @Override
        public void onDownloadChanged(
                DownloadManager downloadManager, Download download, @Nullable Exception finalException) {
            Notification notification;
            if (download.state == Download.STATE_COMPLETED) {
                notification =
                        notificationHelper.buildDownloadCompletedNotification(
                                context,
                                R.drawable.ic_cloud_done_black_24dp,
                                /* contentIntent= */ null,
                                Util.fromUtf8Bytes(download.request.data));
            } else if (download.state == Download.STATE_FAILED) {
                notification =
                        notificationHelper.buildDownloadFailedNotification(
                                context,
                                R.drawable.ic_cloud_done_black_24dp,
                                /* contentIntent= */ null,
                                Util.fromUtf8Bytes(download.request.data));
            } else {
                return;
            }
            NotificationUtil.setNotification(context, nextNotificationId++, notification);
        }
    }
}