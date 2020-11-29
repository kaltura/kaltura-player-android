package com.kaltura.tvplayer;

import com.kaltura.playkit.PKMediaEntry;

public interface PKMediaEntryInterceptor {
    void apply(PKMediaEntry mediaEntry, OnMediaInterceptorListener listener);
}
