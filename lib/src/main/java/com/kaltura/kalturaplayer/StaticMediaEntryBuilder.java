package com.kaltura.kalturaplayer;

import android.net.Uri;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;

import java.util.Collections;

public class StaticMediaEntryBuilder {
    public static MediaEntryProvider provider(int partnerId, String ks, String serverUrl, String entryId, PKMediaFormat format) {
        final PKMediaEntry entry = buildEntry(serverUrl, partnerId, entryId, ks, format);
        return new MediaEntryProvider() {
            @Override
            public void load(OnMediaLoadCompletion completion) {
                completion.onComplete(new ResultElement<PKMediaEntry>() {
                    @Override
                    public PKMediaEntry getResponse() {
                        return entry;
                    }

                    @Override
                    public boolean isSuccess() {
                        return true;
                    }

                    @Override
                    public ErrorElement getError() {
                        return null;
                    }
                });
            }

            @Override
            public void cancel() {

            }
        };
    }

    public static PKMediaEntry buildEntry(String serverUrl, int partnerId, String entryId, String ks, PKMediaFormat format) {
        
        String formatName;
        switch (format) {
            case hls:
                formatName = "applehttp";
                break;
            case dash:
                formatName = "mpegdash";
                break;
            case mp4:
            case mp3:
            case wvm:
                formatName = "url";
                break;
            default:
                return null;
        }

        Uri base = Uri.parse(serverUrl);
        Uri.Builder builder = base.buildUpon();
        builder.appendPath("p").appendPath("" + partnerId).appendPath("playManifest")
                .appendPath("entryId").appendPath(entryId)
                .appendPath("format").appendPath(formatName)
                .appendPath("protocol").appendPath(base.getScheme());


        if (ks != null) {
            builder.appendPath("ks").appendPath(ks);
        }

        builder.appendPath("a." + format.pathExt);

        return new PKMediaEntry().setId(entryId).setSources(Collections.singletonList(
                new PKMediaSource().setId(entryId).setMediaFormat(format)
                        .setUrl(builder.toString())));
    }
}
