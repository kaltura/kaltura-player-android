package com.kaltura.tvplayer.offline.exo;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;

import com.kaltura.android.exoplayer2.offline.Download;
import com.kaltura.android.exoplayer2.offline.DownloadManager;
import com.kaltura.android.exoplayer2.offline.DownloadService;
import com.kaltura.android.exoplayer2.scheduler.PlatformScheduler;
import com.kaltura.android.exoplayer2.util.Util;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.R;

import java.util.List;

public class ExoDownloadService extends DownloadService {

    private static final int JOB_ID = 1;
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static ExoNotificationHelper exoNotificationHelper;
    private static ExoOfflineNotificationHelper customNotification;

    public ExoDownloadService() {
        super(
                FOREGROUND_NOTIFICATION_ID,
                DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
                Consts.EXO_DOWNLOAD_CHANNEL_ID,
                R.string.exo_download_notification_channel_name,
                /* channelDescriptionResourceId= */ 0);
    }

    @Override
    @NonNull
    protected DownloadManager getDownloadManager() {
        DownloadManager downloadManager =  ExoOfflineManager.getInstance(this).downloadManager;
        ExoNotificationHelper exoNotificationHelper = getDownloadNotificationHelper(this);
        downloadManager.addListener(exoNotificationHelper.getDownloadManagerListener(this));
        return downloadManager;
    }

    @Override
    protected PlatformScheduler getScheduler() {
        return Util.SDK_INT >= 21 ? new PlatformScheduler(this, JOB_ID) : null;
    }

    protected static void setForegroundNotification(ExoOfflineNotificationHelper notification) {
        customNotification = notification;
    }

    @Override
    @NonNull
    protected Notification getForegroundNotification(@NonNull List<Download> downloads) {
        if (customNotification != null) {
            return customNotification.buildNotification(this, null, FOREGROUND_NOTIFICATION_ID, downloads);
        }

        return getDownloadNotificationHelper(/* context= */ this)
                .buildProgressNotification(
                        /* context= */ this,
                        R.drawable.ic_cloud_download_black_24dp,
                        /* contentIntent= */ null,
                        /* message= */ null,
                        downloads);
    }

    public static synchronized ExoNotificationHelper getDownloadNotificationHelper(Context context) {
        if (customNotification != null) {
            return customNotification;
        }

        if (exoNotificationHelper == null) {
            exoNotificationHelper = new ExoNotificationHelper(context);
        }
        return exoNotificationHelper;
    }
}