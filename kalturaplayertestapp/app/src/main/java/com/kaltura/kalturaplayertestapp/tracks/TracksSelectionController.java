package com.kaltura.kalturaplayertestapp.tracks;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.player.AudioTrack;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.playkit.player.TextTrack;
import com.kaltura.playkit.player.VideoTrack;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.kalturaplayertestapp.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_AUDIO;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_TEXT;
import static com.kaltura.playkit.utils.Consts.TRACK_TYPE_VIDEO;

public class TracksSelectionController {
    private static final PKLog log = PKLog.get("TracksSelectionController");


    private Context context;
    private KalturaPlayer player;
    private PKTracks tracks;

    private int lastVideoTrackSelectionIndex = 0;
    private int lastAudioTrackSelectionIndex = 0;
    private int lastTextTrackSelectionIndex  = 0;

    public TracksSelectionController(Context context, KalturaPlayer player, PKTracks tracks) {
        this.context = context;
        this.player = player;
        this.tracks = tracks;
        lastVideoTrackSelectionIndex = tracks.getDefaultVideoTrackIndex();
        lastAudioTrackSelectionIndex = tracks.getDefaultAudioTrackIndex();
        lastTextTrackSelectionIndex = tracks.getDefaultTextTrackIndex();
    }

    private RecyclerView buildTracksSelectionView() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.tracks_selection_recycle_view, null);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        return recyclerView;
    }

    public void showTracksSelectionDialog(final int trackType) {
        List<TrackItem> trackItems = createTrackItems(trackType);
        if (trackItems.size() <= 1) {
            return;
        }
        int lastTrackSelection;
        switch (trackType) {
            case TRACK_TYPE_VIDEO:
                lastTrackSelection = lastVideoTrackSelectionIndex;
                break;
            case TRACK_TYPE_AUDIO:
                lastTrackSelection = lastAudioTrackSelectionIndex;
                break;
            case TRACK_TYPE_TEXT:
                lastTrackSelection = lastTextTrackSelectionIndex;
                break;
            default:
                return;
        }

        RecyclerView recyclerView = buildTracksSelectionView();
        final TracksAdapter adapter = new TracksAdapter(trackItems, lastTrackSelection);
        recyclerView.setAdapter(adapter);
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);

        builder.setTitle(getDialogTitle(trackType));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onTrackSelected(trackType, adapter.getTrackItemId(), adapter.getLastTrackSelection());
                dialogInterface.dismiss();
            }
        });
        builder.setView(recyclerView);

        android.support.v7.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public PKTracks getTracks() {
        return tracks;
    }

    private  void onTrackSelected(int trackType, String uniqueId, int lastTrackSelected) {
        if (uniqueId == null) {
            return;
        }

        switch (trackType) {
            case TRACK_TYPE_VIDEO:
                lastVideoTrackSelectionIndex = lastTrackSelected;
                break;
            case TRACK_TYPE_AUDIO:
                lastAudioTrackSelectionIndex = lastTrackSelected;
                break;
            case TRACK_TYPE_TEXT:
                lastTextTrackSelectionIndex = lastTrackSelected;
                break;
            default:
                return;
        }
        player.changeTrack(uniqueId);
    }

    private String getDialogTitle(int trackType) {

        switch (trackType){
            case Consts.TRACK_TYPE_VIDEO:
                return "Video";
            case Consts.TRACK_TYPE_AUDIO:
                return "Audio";
            case Consts.TRACK_TYPE_TEXT:
                return "Text";
            default:
                return "";
        }
    }

    public List<TrackItem> createTrackItems(int eventType) {

        List<TrackItem> trackItems = new ArrayList<>();
        if (tracks == null) {
            return trackItems;
        }
        TrackItem trackItem;
        switch (eventType) {
            case TRACK_TYPE_VIDEO:
                List<VideoTrack> videoTracksInfo = tracks.getVideoTracks();
                for (int i = 0; i < videoTracksInfo.size(); i++) {
                    VideoTrack trackInfo = videoTracksInfo.get(i);
                    if (trackInfo.isAdaptive()) {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), "Auto");
                    } else {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), buildBitrateString(trackInfo.getBitrate()));
                    }
                    trackItems.add(trackItem);
                }
                break;
            case TRACK_TYPE_AUDIO:
                List<AudioTrack> audioTracksInfo = tracks.getAudioTracks();
                if (audioTracksInfo != null && audioTracksInfo.size() > 1) {
                    for (int i = 0; i < audioTracksInfo.size(); i++) {
                        AudioTrack trackInfo = audioTracksInfo.get(i);
                        String lang = trackInfo.getLabel();
                        if (lang == null) {
                            lang = getFriendlyLanguageLabel(trackInfo.getLanguage());
                        }
                        if (trackInfo.isAdaptive()) {
                            trackItem = new TrackItem(trackInfo.getUniqueId(), buildLanguageString(lang) + " " + "Auto");
                        } else {
                            trackItem = new TrackItem(trackInfo.getUniqueId(), buildLanguageString(lang) + " " + buildBitrateString(trackInfo.getBitrate()));
                        }
                        trackItems.add(trackItem);
                    }
                }
                break;
            case TRACK_TYPE_TEXT:
                List<TextTrack> textTracksInfo = tracks.getTextTracks();
                for (int i = 0; i < textTracksInfo.size(); i++) {
                    TextTrack trackInfo = textTracksInfo.get(i);
                    if (trackInfo.isAdaptive()) {
                        trackItem = new TrackItem(trackInfo.getUniqueId(), "Auto");
                    } else {
                        String lang = trackInfo.getLabel();
                        if (lang == null) {
                            lang = getFriendlyLanguageLabel(trackInfo.getLanguage());
                        }
                        trackItem = new TrackItem(trackInfo.getUniqueId(), buildLanguageString(lang));
                    }
                    trackItems.add(trackItem);
                }
                break;
        }
        return trackItems;
    }

    @NonNull
    private String getFriendlyLanguageLabel(String languageCode) {
        if (languageCode == null) {
            return "";
        }

        String lang;
        lang = new Locale(languageCode).getDisplayLanguage();
        lang = lang.substring(0, 1).toUpperCase() + lang.substring(1);
        return (lang != null) ? lang : languageCode;
    }

    private static String buildBitrateString(long bitrate) {
        return bitrate == Consts.NO_VALUE ? ""
                : String.format("%.2fMbit", bitrate / 1000000f);
    }

    private static String buildLanguageString(String language) {
        return TextUtils.isEmpty(language) || "und".equals(language) ? ""
                : language;
    }

    public void setTrackLastSelectionIndex(int trackType , int trackIndex) {
        switch (trackType) {
            case TRACK_TYPE_VIDEO:
                lastVideoTrackSelectionIndex = trackIndex;
                break;
            case TRACK_TYPE_AUDIO:
                lastAudioTrackSelectionIndex = trackIndex;
                break;
            case TRACK_TYPE_TEXT:
                lastTextTrackSelectionIndex = trackIndex;
                break;
            default:
                return;
        }
    }
}
