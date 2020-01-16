package com.kaltura.tvplayer.playlist;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.OTTMediaOptions;
import com.kaltura.tvplayer.OVPMediaOptions;

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
    private CountDownOptions countDownOptions;

    private int currentPlayingIndex = -1;
    private boolean playlistAutoContinue = true;
    private boolean loopEnabled;
    private boolean shuffleEnabled;
    private List<PKPlaylistMedia> origlPlaylistEntries;
    private Map<Integer, PKMediaEntry> loadedMediasMap;

    public PKPlaylistController(KalturaPlayer kalturaPlayer, PKPlaylist playlist) {
        this.kalturaPlayer = kalturaPlayer;
        subscribeToPlayerEvents();
        this.playlist = playlist;

        origlPlaylistEntries = new ArrayList<>();
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
    public void preloadNext() {
        if (kalturaPlayer.getTvPlayerType() != KalturaPlayer.Type.basic) {
            preloadItem(currentPlayingIndex + 1);
        }
    }

    public void preloadItem(int index) {
        kalturaPlayer.setAutoPlay(false);
        kalturaPlayer.setPreload(false);

        if (playlist instanceof PKBasicPlaylist && ((PKBasicPlaylist) playlist).getPlaylistMediaEntryList() == null) {
            return;
        } else if (!(playlist instanceof PKBasicPlaylist) && (playlist == null ||  playlist.getMediaList() == null)) {
            return;
        }

        boolean isValidIndex = isValidPlaylistIndex(index);
        if (!isValidIndex) {
            return;
        }
        if (shuffleEnabled) {
            int origIndex = playlist.getMediaList().get(index).getMediaIndex();
            if (loadedMediasMap.containsKey(origIndex)) {
                //kalturaPlayer.setMedia(loadedMediasMap.get(origIndex));
                return;
            }
        } else {
            if (loadedMediasMap.containsKey(index)) {
                //kalturaPlayer.setMedia(loadedMediasMap.get(index));
                return;
            }
        }

        if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ovp) {
            playItemOVP(index);
        } else if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ott) {
            playItemOTT(index);
        } else if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic) {
            playItemBasic(index);
        }
    }

    @Override
    public void playItem(int index) {
        log.d("playItem index = " + index);
        kalturaPlayer.setAutoPlay(true);
        kalturaPlayer.setPreload(true);

        if (playlist instanceof PKBasicPlaylist && ((PKBasicPlaylist) playlist).getPlaylistMediaEntryList() == null) {
            return;
        } else if (!(playlist instanceof PKBasicPlaylist) && (playlist == null ||  playlist.getMediaList() == null)) {
            return;
        }

        boolean isValidIndex = isValidPlaylistIndex(index);
        if (!isValidIndex) {
            return;
        }
        if (shuffleEnabled) {
            int origIndex = playlist.getMediaList().get(index).getMediaIndex();
            if (loadedMediasMap.containsKey(origIndex)) {
                kalturaPlayer.setMedia(loadedMediasMap.get(origIndex));
                return;
            }
        } else {
            if (loadedMediasMap.containsKey(index)) {
                kalturaPlayer.setMedia(loadedMediasMap.get(index));
                return;
            }
        }

        currentPlayingIndex = index;
        if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ovp) {
            playItemOVP(index);
        } else if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ott) {
            playItemOTT(index);
        } else if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic) {
            playItemBasic(index);
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
                loadedMediasMap.put(playlist.getMediaList().get(index).getMediaIndex(), entry);
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
                loadedMediasMap.put(playlist.getMediaList().get(index).getMediaIndex(), entry);
                log.d("OTTMedia onEntryLoadComplete  entry = " + entry.getId());
            }
        });
    }

    private void playItemBasic(int index) {
        BasicPlaylistOptions basicPlaylistOptions = (BasicPlaylistOptions) playlistOptions;
        PKMediaEntry pkMediaEntry = getNextMediaOptions(index, basicPlaylistOptions);
        if (pkMediaEntry == null) {
            return; // error cannot play any next item
        }
        loadedMediasMap.put(((PKBasicPlaylist)playlist).getPlaylistMediaEntryList().get(index).getMediaIndex(), pkMediaEntry);
        kalturaPlayer.setMedia(pkMediaEntry, 0L);
    }

    private boolean isValidPlaylistIndex(int index) {
        boolean isValidIndex;
        int playlistSize = playlist.getMediaListSize();
        isValidIndex = index >= 0 && index < playlistSize;
        if (!isValidIndex) {
            String errorMessage = "Invalid Basic playlist index = " + index + " size = " + playlistSize;
            String errorCode = "InvalidPlaylistIndex";
            kalturaPlayer.messageBus.post(new PlaylistEvent.PlaylistError
                    (new ErrorElement(errorMessage, errorCode)));
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
            List<PlaylistPKMediaEntry> playlistPKMediaEntryList = basicPlaylistOptions.playlistPKMediaEntryList;
            if (!(index < playlistPKMediaEntryList.size())) break;
            if (playlistPKMediaEntryList.get(index) != null && playlistPKMediaEntryList.get(index).getPKMediaEntry() != null) {
                return playlistPKMediaEntryList.get(index).getPKMediaEntry();
            }
            index++;
        }
        return null;
    }

    @Override
    public void playNext() {
        log.d("playNext");
        int playlistSize = playlist.getMediaListSize();
        if (currentPlayingIndex + 1 == playlistSize) {
            if (loopEnabled) {
                replay();
            }
            log.d("Ignore playNext - invalid index!");
            return;
        }
        playItem(++currentPlayingIndex);
    }

    @Override
    public void playPrev() {
        log.d("playPrev");
        int playlistSize = playlist.getMediaListSize();
        if (currentPlayingIndex - 1 < 0) {
            if (loopEnabled) {
                currentPlayingIndex = playlistSize - 1;
                playItem(currentPlayingIndex) ;
            }
            log.d("Ignore playPrev - invalid index!");
            return;
        }
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
        kalturaPlayer.messageBus.post(new PlaylistEvent.PlaylistLoopStateChanged(mode));
    }

    @Override
    public void shuffle(boolean mode) {
        log.d("shuffle mode = " + mode);
        kalturaPlayer.messageBus.post(new PlaylistEvent.PlaylistShuffleStateChanged(mode));

        shuffleEnabled = mode;
        if (playlist != null && mode) {
            origlPlaylistEntries = playlist.getMediaList();
            playlist.setMediaList(new ArrayList<>(playlist.getMediaList()));
            Collections.shuffle(playlist.getMediaList());
        } else {
            if (origlPlaylistEntries != null && !origlPlaylistEntries.isEmpty()) {
                playlist.setMediaList(origlPlaylistEntries);
            }
        }
    }

    @Override
    public void setAutoContinue(boolean mode) {
        playlistAutoContinue = mode;
    }

    @Override
    public void reset() {
        log.d("reset");
        currentPlayingIndex = -1;
        loopEnabled  = false;
        shuffleEnabled = false;
        loadedMediasMap.clear();
        origlPlaylistEntries.clear();
    }

    @Override
    public void release() {
        kalturaPlayer.removeListeners(this);
        //kalturaPlayer = null;
    }

    @Override
    public void setPlaylistOptions(PlaylistOptions playlistOptions) {
        this.playlistOptions = playlistOptions;
        this.countDownOptions = playlistOptions.countDownOptions;
        shuffle(playlistOptions.shuffleEnabled);
        loop(playlistOptions.loopEnabled);
    }

    private void subscribeToPlayerEvents() {

        kalturaPlayer.addListener(this, PlayerEvent.playing, event -> {
        });

        kalturaPlayer.addListener(this, PlayerEvent.ended, event -> {
            log.d("ended event received");
            if (playlistAutoContinue && (countDownOptions == null || !countDownOptions.shouldDisplay()|| countDownOptions.getTimeToShowMS() == -1 || !countDownOptions.isEventSent())) {
                handlePlaylistMediaEnded();
            }
            resetCountDownOptions();
        });


        kalturaPlayer.addListener(this, PlayerEvent.seeking, event -> {
            log.d("seeking event received");
            if (countDownOptions.isEventSent() && event.targetPosition < countDownOptions.getTimeToShowMS()) {
                countDownOptions.setEventSent(false);
            }
        });

        kalturaPlayer.addListener(this, PlayerEvent.playheadUpdated, event -> {
            log.d("playheadUpdated received position = " + event.position + "/" + event.duration);

            if (countDownOptions == null) {
                if (playlistOptions instanceof OVPPlaylistOptions) {
                    countDownOptions = ((OVPPlaylistOptions) playlistOptions).ovpMediaOptionsList.get(currentPlayingIndex).countDownOptions;

                }
                if (playlistOptions instanceof OTTPlaylistOptions) {
                    countDownOptions = ((OTTPlaylistOptions) playlistOptions).ottMediaOptionsList.get(currentPlayingIndex).countDownOptions;
                }

                if (playlistOptions instanceof BasicPlaylistOptions) {
                    countDownOptions = ((BasicPlaylistOptions) playlistOptions).playlistPKMediaEntryList.get(currentPlayingIndex).getCountDownOptions();
                }

                if (countDownOptions == null) {
                    countDownOptions = playlistOptions.countDownOptions;
                }
            }

            if (playlistAutoContinue && countDownOptions != null && countDownOptions.shouldDisplay()) {
                long timeToShow = (countDownOptions.getTimeToShowMS() == -1) ? Math.max(0, event.duration - countDownOptions.getDurationMS()) : countDownOptions.getTimeToShowMS();
                if (event.position >= timeToShow) {
                    if (!countDownOptions.isEventSent()) {
                        log.d("XXX SEND COUNT DOWN EVENT position = " + event.position);
                        kalturaPlayer.messageBus.post(new PlaylistEvent.PlaylistMediaCountDown(currentPlayingIndex, countDownOptions));
                        countDownOptions.setEventSent(true);
                        preloadNext();
                    } else if (event.position >= Math.min(timeToShow + countDownOptions.getDurationMS(), event.duration)) {
                        log.d("XXX playjead updated handlePlaylistMediaEnded");
                        handlePlaylistMediaEnded();
                    }
                }
            }
        });

        kalturaPlayer.addListener(this, PlayerEvent.error, event -> {
            PlayerEvent.Error errorEvent = event;
            log.e("errorEvent.error.errorType"  + " " + event.error.message);
            //playNext();
        });
    }

    private void handlePlaylistMediaEnded() {
        int playlistSize = playlist.getMediaListSize();
        if (currentPlayingIndex + 1 == playlistSize) {
            log.d("XXX REPLAY");
            if (loopEnabled) {
                replay();
            }
            kalturaPlayer.messageBus.post(new PlaylistEvent.PlaylistEnded(playlist));
        } else {
            log.d("XXX PLAY NEXT");
            playNext();
        }
    }

    private void resetCountDownOptions() {
        countDownOptions.setEventSent(false);
        countDownOptions = null;
    }
}
