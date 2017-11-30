package com.kaltura.kalturaplayer;

import com.kaltura.netkit.utils.ErrorElement;

public interface KSResultCallback {
    void complete(String ks, ErrorElement error);
}
