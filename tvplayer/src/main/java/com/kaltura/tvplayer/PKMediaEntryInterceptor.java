package com.kaltura.tvplayer;

import com.kaltura.playkit.PKMediaEntry;

/**
 * Created by alex_lytvynenko on 09.11.2020.
 */
public interface PKMediaEntryInterceptor {
    void apply(PKMediaEntry mediaEntry, OnMediaInterceptorListener listener);
}
