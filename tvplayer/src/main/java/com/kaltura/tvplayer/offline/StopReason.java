package com.kaltura.tvplayer.offline;

import com.kaltura.android.exoplayer2.offline.Download;

enum StopReason {
    none,       // 0 = Download.STOP_REASON_NONE
    unknown,    // 10
    pause;      // 11

    static StopReason fromExoReason(int reason) {
        switch (reason) {
            case Download.STOP_REASON_NONE:
                return unknown;
            case 11:
                return pause;
            default:
                return unknown;
        }
    }

    int toExoCode() {
        switch (this) {
            case none: return Download.STOP_REASON_NONE;
            case pause: return 11;
            default: return 10;
        }
    }
}
