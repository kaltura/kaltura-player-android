package com.kaltura.tvplayer.playlist;

import android.os.Handler;
import android.os.Looper;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.player.PKTracks;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.OVPMediaOptions;
import com.kaltura.tvplayer.PKBasicPlaylist;

import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PKPlaylistController implements PlaylistController {

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

    public PKPlaylistController(KalturaPlayer kalturaPlayer, PKPlaylist playlist) {
        this.kalturaPlayer = kalturaPlayer;
        subscribeToPlayerEvents();
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

        if (playlist instanceof PKBasicPlaylist && ((PKBasicPlaylist) playlist).getPlaylistMediaEntryList() == null) {
            return;
        } else if (!(playlist instanceof PKBasicPlaylist) && (playlist == null ||  playlist.getMediaList() == null)) {
            return;
        }

        if (loadedMediasMap.containsKey(index)) {
            kalturaPlayer.setMedia(loadedMediasMap.get(index));
            return;
        }

        if (isValidPlaylistIndex(index)) {
            currentPlayingIndex = index;
            if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ovp) {
                playItemOVP(index);
            } else if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ott) {
                playItemOTT(index);
            } else if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic) {
                playItemBasic(index);
            }
        }
    }

    private void playItemOVP(int index) {
        OVPMediaOptions ovpMediaOptions;
        if (playlistOptions instanceof OVPPlaylistOptions) {
            OVPPlaylistOptions ovpPlaylistOptions = (OVPPlaylistOptions) playlistOptions;
            ovpMediaOptions = getNextMediaOptions(index, ovpPlaylistOptions);
            if (ovpMediaOptions == null) {
                return; // error cannot play any next item
            }
            if (ovpMediaOptions.ks == null) {
                ovpMediaOptions.ks = ovpPlaylistOptions.ks;
            }
            if (ovpMediaOptions.referrer == null) {
                ovpMediaOptions.referrer = kalturaPlayer.getInitOptions().referrer;
            }
            if (!ovpMediaOptions.useApiCaptions) {
                ovpMediaOptions.useApiCaptions = ovpPlaylistOptions.useApiCaptions;
            }
        } else { // PlaylistId case
            OVPPlaylistIdOptions ovpPlaylistIdOptions = (OVPPlaylistIdOptions) playlistOptions;
            ovpMediaOptions = new OVPMediaOptions();
            ovpMediaOptions.entryId = playlist.getMediaList().get(index).getId();
            ovpMediaOptions.ks = (playlist.getMediaList().get(index).getKs() != null) ? playlist.getMediaList().get(index).getKs() : playlist.getKs();
            ovpMediaOptions.referrer = kalturaPlayer.getInitOptions().referrer;
            ovpMediaOptions.useApiCaptions = ovpPlaylistIdOptions.useApiCaptions;
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
    }

    private void playItemOTT(int index) {
        OTTPlaylistOptions ottPlaylistOptions =  (OTTPlaylistOptions) playlistOptions;
        OTTMediaOptions ottMediaOptions = getNextMediaOptions(index, ottPlaylistOptions);
        if (ottMediaOptions == null) {
            return; // error cannot play any next item
        }
        if (ottMediaOptions.ks == null) {
            ottMediaOptions.ks =  ottPlaylistOptions.ks;
        }
        if (ottMediaOptions.referrer == null) {
            ottMediaOptions.referrer = kalturaPlayer.getInitOptions().referrer;
        }

        kalturaPlayer.loadMedia(ottMediaOptions, (entry, loadError) -> {
            if (loadError != null) {
                log.e(loadError.getMessage());
                // send error event
            } else {
                log.d("OVPMedia onEntryLoadComplete  entry = " + entry.getId());
            }
        });
    }

    private void playItemBasic(int index) {
        BasicPlaylistOptions basicPlaylistOptions = (BasicPlaylistOptions) playlistOptions;
        PKMediaEntry pkMediaEntry = getNextMediaOptions(index, basicPlaylistOptions);
        if (pkMediaEntry == null) {
            return; // error cannot play any next item
        }
        loadedMediasMap.put(index, pkMediaEntry);
        kalturaPlayer.setMedia(pkMediaEntry, 0L);
    }

    private boolean isValidPlaylistIndex(int index) {
        boolean isValidIndex;
        if (playlist instanceof PKBasicPlaylist) {
            isValidIndex = index >= 0 && index < ((PKBasicPlaylist) playlist).getPlaylistMediaEntryList().size();
        } else {
            isValidIndex = index >= 0 && index < playlist.getMediaList().size();
        }
        return isValidIndex;
    }

    private OVPMediaOptions getNextMediaOptions(int index, OVPPlaylistOptions ovpPlaylistOptions) {
        while (true) {
            List<OVPMediaOptions> ovpMediaOptionsList = ovpPlaylistOptions.ovpMediaOptionsList;
            if (!(index < ovpMediaOptionsList.size())) break;
            if (ovpMediaOptionsList.get(index) != null) {
                return ovpMediaOptionsList.get(index);
            }
            index++;
        }
        return null;
    }

    private OTTMediaOptions getNextMediaOptions(int index, OTTPlaylistOptions ottPlaylistOptions) {
        while(index < ottPlaylistOptions.ottMediaOptionsList.size()) {
            if (ottPlaylistOptions.ottMediaOptionsList.get(index) != null) {
                return ottPlaylistOptions.ottMediaOptionsList.get(index);
            }
            index++;
        }
        return null;
    }

    private PKMediaEntry getNextMediaOptions(int index, BasicPlaylistOptions basicPlaylistOptions) {
        while(true) {
            List<PKMediaEntry> pkMediaEntryList = basicPlaylistOptions.pkMediaEntryList;
            if (!(index < pkMediaEntryList.size())) break;
            if (pkMediaEntryList.get(index) != null) {
                return pkMediaEntryList.get(index);
            }
            index++;
        }
        return null;
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
    public void release() {
        kalturaPlayer.removeListeners(this);
        //kalturaPlayer = null;
    }

    @Override
    public void setPlaylistOptions(PlaylistOptions playlistOptions) {
        this.playlistOptions = playlistOptions;
        shuffle(playlistOptions.shuffleEnabled);
        loop(playlistOptions.loopEnabled);
    }

    private void subscribeToPlayerEvents() {

        kalturaPlayer.addListener(this, PlayerEvent.ended, event -> {
            log.d("event received: " + event.eventType().name());
            playNext();

        });

        kalturaPlayer.addListener(this, PlayerEvent.playheadUpdated, event -> {
            log.d("event received: " + event.eventType().name());
            if (kalturaPlayer.getCurrentPosition() + 10 >= kalturaPlayer.getDuration()) {
                //SEND EVENT FOR CONFIGURATION
            }

        });

        kalturaPlayer.addListener(this, PlayerEvent.error, event -> {
            PlayerEvent.Error errorEvent = event;
            log.e("errorEvent.error.errorType"  + " " + event.error.message);
            //playNext();
        });
    }
}
