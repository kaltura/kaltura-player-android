package com.kaltura.tvplayer.playlist;

import android.os.Handler;
import android.os.Looper;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.OVPMediaOptions;

import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ProvidersPlaylistController implements PlaylistController {

    private static final PKLog log = PKLog.get("PlaylistController");
    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    private KalturaPlayer kalturaPlayer;
    private PKPlaylist playlist;
    private PlaylistOptions playlistOptions;

    private int currentPlayingIndex = -1;
    private boolean loopEnabled;
    private boolean shuffleEnabled;
    private List<PKPlaylistMedia> shuffledEntries;
    private Map<Integer, PKMediaEntry> loadedMediasMap;

    public ProvidersPlaylistController(KalturaPlayer kalturaPlayer, PKPlaylist playlist) {
        this.kalturaPlayer = kalturaPlayer;
        this.playlist = playlist;
        shuffledEntries = new ArrayList<>();
        loadedMediasMap = new HashMap<>();
    }

    @Override
    public PKPlaylist getPlaylist() {
        return playlist;
    }

    @Override
    public PKPlaylistMedia getCurrentPlaylistMedia() {
        return playlist.getMediaList().get(currentPlayingIndex);
    }

    @Override
    public int getCurrentMediaIndex() {
        return currentPlayingIndex;
    }

    @Override
    public void playItem(int index) {
        if (playlist == null || playlist.getMediaList() == null) {
            return;
        }

        if (loadedMediasMap.containsKey(index)) {
            kalturaPlayer.setMedia(loadedMediasMap.get(index));
            return;
        }

        if (index >= 0 && index < playlist.getMediaList().size()) {
            currentPlayingIndex = index;
            if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ovp) {
                OVPMediaOptions ovpMediaOptions;
                if (playlistOptions instanceof OVPPlaylistOptions) {
                    OVPPlaylistOptions ovpPlaylistOptions = (OVPPlaylistOptions) playlistOptions;
                    ovpMediaOptions = getNextMediaOptions(index, ovpPlaylistOptions);
                    if (ovpMediaOptions.ks == null) {
                        ovpMediaOptions.ks =  ovpPlaylistOptions.ks;
                    }
                    if (ovpMediaOptions.referrer == null) {
                        ovpMediaOptions.referrer =  kalturaPlayer.getInitOptions().referrer;
                    }
                    if (!ovpMediaOptions.useApiCaptions) {
                        ovpMediaOptions.useApiCaptions =  ovpPlaylistOptions.useApiCaptions;
                    }
                } else { // PlaylistId case
                    ovpMediaOptions = new OVPMediaOptions();
                    ovpMediaOptions.entryId = playlist.getMediaList().get(index).getId();
                    ovpMediaOptions.ks = (playlist.getMediaList().get(index).getKs() != null) ? playlist.getMediaList().get(index).getKs() : playlist.getKs();
                    ovpMediaOptions.referrer = kalturaPlayer.getInitOptions().referrer;
                    ovpMediaOptions.useApiCaptions = playlistOptions.useApiCaptions;
                }
                kalturaPlayer.loadMedia(ovpMediaOptions, (entry, loadError) -> {
                    if (loadError != null) {
                        log.e(loadError.getMessage());
                        // send error event
                    } else {
                        loadedMediasMap.put(index, entry);
                        log.d("OVPMedia onEntryLoadComplete  entry = " + entry.getId());
                    }
                });
            } else if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ott) {
                OTTPlaylistOptions ottPlaylistOptions =  (OTTPlaylistOptions) playlistOptions;

                OTTMediaOptions ottMediaOptions = getNextMediaOptions(index, ottPlaylistOptions);
                if (ottMediaOptions.ks == null) {
                    ottMediaOptions.ks =  ottPlaylistOptions.ks;
                }
                if (ottMediaOptions.referrer == null) {
                    ottMediaOptions.referrer =  kalturaPlayer.getInitOptions().referrer;
                }

                kalturaPlayer.loadMedia(ottMediaOptions, (entry, loadError) -> {
                    if (loadError != null) {
                        log.e(loadError.getMessage());
                        // send error event
                    } else {
                        log.d("OVPMedia onEntryLoadComplete  entry = " + entry.getId());
                    }
                });
            } else if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic) {

            }
        }
    }

    private OVPMediaOptions getNextMediaOptions(int index, OVPPlaylistOptions ovpPlaylistOptions) {
        return ovpPlaylistOptions.ovpMediaOptionsList.get(index);
    }

    private OTTMediaOptions getNextMediaOptions(int index, OTTPlaylistOptions ottPlaylistOptions) {
        return ottPlaylistOptions.ottMediaOptionsList.get(index);
    }

    @Override
    public void playNext() {
        log.d("playNext");
        playItem(++currentPlayingIndex);
    }

    @Override
    public void playPrev() {
        log.d("playPrev");
        playItem(--currentPlayingIndex);
    }

    @Override
    public void replay() {
        log.d("replay");
        currentPlayingIndex = 0;
        playItem(currentPlayingIndex);
    }

    @Override
    public boolean isMediaLoaded(int index) {
        log.d("isMediaLoaded index = " + index);
        if (loadedMediasMap == null) {
            return false;
        }
        return loadedMediasMap.get(index) != null ;
    }

    @Override
    public void loop(boolean mode) {
        log.d("loop mode = " + mode);
        loopEnabled = mode;
    }

    @Override
    public void shuffle(boolean mode) {
        log.d("shuffle mode = " + mode);
        shuffleEnabled = mode;
        if (playlist != null && mode) {
            shuffledEntries = new ArrayList<>(playlist.getMediaList());
            Collections.shuffle(shuffledEntries);
        } else {
            shuffledEntries.clear();
        }
    }

    @Override
    public void reset() {
        log.d("reset");
        currentPlayingIndex = -1;
        loopEnabled  = false;
        shuffleEnabled = false;
        loadedMediasMap.clear();
        shuffledEntries.clear();
    }

    @Override
    public void setPlaylistOptions(PlaylistOptions playlistOptions) {
        this.playlistOptions = playlistOptions;
        shuffle(playlistOptions.shuffleEnabled);
        loop(playlistOptions.loopEnabled);
    }
}
