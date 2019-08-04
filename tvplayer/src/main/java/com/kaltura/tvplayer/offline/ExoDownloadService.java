package com.kaltura.tvplayer.offline;

import android.app.Notification;
import androidx.annotation.Nullable;
import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloadService;
import com.kaltura.android.exoplayer2.scheduler.PlatformScheduler;
import com.kaltura.android.exoplayer2.scheduler.Scheduler;
import com.kaltura.android.exoplayer2.ui.DownloadNotificationHelper;
import com.kaltura.android.exoplayer2.util.NotificationUtil;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.tvplayer.R;

import java.util.List;

public class ExoDownloadService extends DownloadService {


    private static final String CHANNEL_ID = "download_channel";
    private static final int JOB_ID = 1;
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    private static int nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1;

    private DownloadNotificationHelper notificationHelper;

    public ExoDownloadService() {
        super(FOREGROUND_NOTIFICATION_ID,
                DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
                CHANNEL_ID,
                R.string.download_notification_channel);

        nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1;

    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new DownloadNotificationHelper(this, CHANNEL_ID);
    }

    @Override
    protected DownloadManager getDownloadManager() {
        return ExoOfflineManager.getInstance(this).downloadManager;
    }

    @Nullable
    @Override
    protected Scheduler getScheduler() {
        return new WorkManagerScheduler("ExoDownloadService");
    }

    @Override
    protected Notification getForegroundNotification(List<Download> downloads) {
        return notificationHelper.buildProgressNotification(
                R.drawable.ic_cloud_download_black_24dp, /* contentIntent= */ null, /* message= */ null, downloads);
    }

    @Override
    protected void onDownloadChanged(Download download) {
        Notification notification;
        if (download.state == Download.STATE_COMPLETED) {
            notification =
                    notificationHelper.buildDownloadCompletedNotification(
                            R.drawable.ic_cloud_done_black_24dp,
                            /* contentIntent= */ null,
                            Util.fromUtf8Bytes(download.request.data));
        } else if (download.state == Download.STATE_FAILED) {
            notification =
                    notificationHelper.buildDownloadFailedNotification(
                            R.drawable.ic_cloud_done_black_24dp,
                            /* contentIntent= */ null,
                            Util.fromUtf8Bytes(download.request.data));
        } else {
            return;
        }
        NotificationUtil.setNotification(this, nextNotificationId++, notification);
    }

}
