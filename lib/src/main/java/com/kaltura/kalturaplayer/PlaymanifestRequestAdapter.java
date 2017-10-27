/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.kalturaplayer;

import android.net.Uri;

import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;
import static com.kaltura.playkit.Utils.toBase64;

public class PlaymanifestRequestAdapter implements PKRequestParams.Adapter {

    private final String referrer;
    private String playSessionId;

    public static PlaymanifestRequestAdapter install(Player player, String referrer) {
        PlaymanifestRequestAdapter decorator = new PlaymanifestRequestAdapter(player, referrer);
        player.getSettings().setContentRequestAdapter(decorator);
        
        return decorator;
    }

    private PlaymanifestRequestAdapter(Player player, String referrer) {
        this.referrer = referrer;
        updateParams(player);
    }

    @Override
    public PKRequestParams adapt(PKRequestParams requestParams) {
        Uri url = requestParams.url;

        if (url.getPath().contains("/playManifest/")) {
            Uri alt = url.buildUpon()
                    .appendQueryParameter("clientTag", CLIENT_TAG)
                    .appendQueryParameter("referrer", toBase64(referrer.getBytes()))
                    .appendQueryParameter("playSessionId", playSessionId)
                    .build();

            String lastPathSegment = requestParams.url.getLastPathSegment();
            if (lastPathSegment.endsWith(".wvm")) {
                // in old android device it will not play wvc if url is not ended in wvm
                alt = alt.buildUpon().appendQueryParameter("name", lastPathSegment).build();
            }
            return new PKRequestParams(alt, requestParams.headers);
        }

        return requestParams;
    }

    @Override
    public void updateParams(Player player) {
        this.playSessionId = player.getSessionId();
    }
}
