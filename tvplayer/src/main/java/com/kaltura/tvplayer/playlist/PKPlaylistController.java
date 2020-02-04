package com.kaltura.tvplayer.playlist;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.playkit.PlayerEvent;
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

    private KalturaPlayer kalturaPlayer;
    private PKPlaylist playlist;
    private PKPlaylistType playlistType;
    private PlaylistOptions playlistOptions;
    private CountDownOptions countDownOptions;

    private int currentPlayingIndex = -1;
    private boolean playlistAutoContinue = true;
    private boolean loopEnabled;
    private boolean shuffleEnabled;

    private List<PKPlaylistMedia> origlPlaylistEntries;
    private Map<Integer, PKMediaEntry> loadedMediasMap;

    public PKPlaylistController(KalturaPlayer kalturaPlayer, PKPlaylist playlist, PKPlaylistType playlistType) {
        this.kalturaPlayer = kalturaPlayer;
        subscribeToPlayerEvents();
        this.playlist = playlist;
        this.playlistType = playlistType;

        origlPlaylistEntries = new ArrayList<>();
        loadedMediasMap = new HashMap<>();
    }

    @Override
    public PKPlaylist getPlaylist() {
        return playlist;
    }

    @Override
    public PKPlaylistType getPlaylistType() {
        return playlistType;
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
    public CountDownOptions getCurrentCountDownOptions() {
        return countDownOptions;
    }

    @Override
    public void disableCountDown() {
        if (countDownOptions != null) {
            countDownOptions.setShouldDisplay(false);
        }
    }

    @Override
    public void preloadNext() {
        if (kalturaPlayer.getTvPlayerType() != KalturaPlayer.Type.basic) {
            preloadItem(currentPlayingIndex + 1);
        }
    }

    @Override
    public void preloadItem(int index) {
        kalturaPlayer.setAutoPlay(false);
        kalturaPlayer.setPreload(false);

        if (playlist instanceof PKBasicPlaylist && ((PKBasicPlaylist) playlist).getBasicMediaOptionsList() == null) {
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
                return;
            }
        } else {
            if (loadedMediasMap.containsKey(index)) {
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

    /**
     *  playback will start automatically
     */

    @Override
    public void playItem(int index) {
        playItem(index, isAutoContinueEnabled());
    }

    /**
     *  playback will start only if isAutoPlay == true
     */

    @Override
    public void playItem(int index, boolean isAutoPlay) {
        log.d("playItem index = " + index);
        kalturaPlayer.setAutoPlay(isAutoPlay);
        kalturaPlayer.setPreload(true);
        countDownOptions = null;
        if (playlist instanceof PKBasicPlaylist && ((PKBasicPlaylist) playlist).getBasicMediaOptionsList() == null) {
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
                log.e("OVPMedia error = " + loadError.getMessage());
                kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistMediaError(index, new ErrorElement(loadError.getMessage(), loadError.getCode())));
            } else {
                if (playlist.getMediaList() != null && playlist.getMediaList().get(index) != null) {
                    loadedMediasMap.put(playlist.getMediaList().get(index).getMediaIndex(), entry);
                    log.d("OVPMedia onEntryLoadComplete entry = " + entry.getId());
                } else {
                    log.e("OVPMedia onEntryLoadComplete playlist.getMediaList().get(" + index + ") == null");
                }
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
                kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistMediaError(index, new ErrorElement(loadError.getMessage(), loadError.getCode())));
            }
            else {
                if (playlist.getMediaList() != null && playlist.getMediaList().get(index) != null) {
                    loadedMediasMap.put(playlist.getMediaList().get(index).getMediaIndex(), entry);
                    log.d("OTTMedia onEntryLoadComplete entry = " + entry.getId());
                } else {
                    log.e("OTTMedia onEntryLoadComplete playlist.getMediaList().get(" + index + ") == null");
                }
            }
        });
    }

    private void playItemBasic(int index) {
        BasicPlaylistOptions basicPlaylistOptions = (BasicPlaylistOptions) playlistOptions;
        PKMediaEntry pkMediaEntry = getNextMediaOptions(index, basicPlaylistOptions);
        if (pkMediaEntry == null) {
            return; // error cannot play any next/prev item
        }

        List<BasicMediaOptions> basicMediaOptionsList = ((PKBasicPlaylist) playlist).getBasicMediaOptionsList();
        if (basicMediaOptionsList != null && basicMediaOptionsList.get(index) != null) {
            loadedMediasMap.put(basicMediaOptionsList.get(index).getMediaIndex(), pkMediaEntry);
            kalturaPlayer.setMedia(pkMediaEntry, 0L);
        } else {
            log.e("BasicMedia onEntryLoadComplete basicMediaOptionsList.get(" + index + ") == null");
        }
    }

    private boolean isValidPlaylistIndex(int index) {
        boolean isValidIndex;
        int playlistSize = playlist.getMediaListSize();
        isValidIndex = index >= 0 && index < playlistSize;
        if (!isValidIndex) {
            String errorMessage = "Invalid playlist index = " + index + " size = " + playlistSize;
            String errorCode = "InvalidPlaylistIndex";
            kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistError
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
            List<BasicMediaOptions> playlistBasicMediaEntryList = basicPlaylistOptions.basicMediaOptionsList;
            if (!(index < playlistBasicMediaEntryList.size())) {
                break;
            }

            if (playlistBasicMediaEntryList.get(index) != null && playlistBasicMediaEntryList.get(index).getPKMediaEntry() != null) {
                return playlistBasicMediaEntryList.get(index).getPKMediaEntry();
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

        if ((kalturaPlayer.getTvPlayerType() != KalturaPlayer.Type.basic && playlist.getMediaList().get(currentPlayingIndex + 1) == null) ||
                (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic && ((PKBasicPlaylist)playlist).getBasicMediaOptionsList().get(currentPlayingIndex + 1) == null) ||
                (loadedMediasMap.containsKey(currentPlayingIndex + 1) && loadedMediasMap.get(currentPlayingIndex + 1) == null)
        ) {
            ++currentPlayingIndex;
            playNext();
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
                playItem(currentPlayingIndex);
            }
            log.d("Ignore playPrev - invalid index!");
            return;
        }

        if ((kalturaPlayer.getTvPlayerType() != KalturaPlayer.Type.basic && playlist.getMediaList().get(currentPlayingIndex - 1) == null) ||
                (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic && ((PKBasicPlaylist)playlist).getBasicMediaOptionsList().get(currentPlayingIndex - 1) == null) ||
                (loadedMediasMap.containsKey(currentPlayingIndex - 1) && loadedMediasMap.get(currentPlayingIndex - 1) == null)) {
            --currentPlayingIndex;
            playPrev();
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
        return loadedMediasMap.containsKey(index) && loadedMediasMap.get(index) != null;
    }

    @Override
    public void loop(boolean mode) {
        log.d("loop mode = " + mode);
        loopEnabled = mode;
        kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistLoopStateChanged(mode));
    }

    @Override
    public boolean isLoopEnabled() {
        return loopEnabled;
    }

    @Override
    public void shuffle(boolean mode) {
        log.d("shuffle mode = " + mode);
        kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistShuffleStateChanged(mode));

        shuffleEnabled = mode;
        if (playlist != null) {
            if (mode) {
                origlPlaylistEntries = playlist.getMediaList();
                playlist.setMediaList(new ArrayList<>(playlist.getMediaList()));
                Collections.shuffle(playlist.getMediaList());
            } else {
                if (origlPlaylistEntries != null && !origlPlaylistEntries.isEmpty()) {
                    playlist.setMediaList(origlPlaylistEntries);
                }
            }
        }
    }

    @Override
    public boolean isShuffleEnabled(boolean mode) {
        return shuffleEnabled;
    }

    @Override
    public void setAutoContinue(boolean mode) {
        playlistAutoContinue = mode;
    }


    @Override
    public boolean isAutoContinueEnabled() {
        return playlistAutoContinue;
    }

    @Override
    public void reset() {
        log.d("reset");
        currentPlayingIndex = -1;
        loopEnabled  = false;
        shuffleEnabled = false;
        playlistAutoContinue = true;
        loadedMediasMap.clear();
        origlPlaylistEntries.clear();
        if (kalturaPlayer != null && kalturaPlayer.getTvPlayerType() != KalturaPlayer.Type.basic && playlist != null && playlist.getMediaList() != null) {
            playlist.getMediaList().clear();
        }
        if (kalturaPlayer != null && kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic && playlist != null && ((PKBasicPlaylist)playlist).getBasicMediaOptionsList() != null) {
            ((PKBasicPlaylist)playlist).getBasicMediaOptionsList().clear();
        }
    }

    @Override
    public void release() {
        reset();
        kalturaPlayer.removeListeners(this);
    }

    @Override
    public void setPlaylistOptions(PlaylistOptions playlistOptions) {
        this.playlistOptions = playlistOptions;
        shuffle(playlistOptions.shuffleEnabled);
        loop(playlistOptions.loopEnabled);
        setAutoContinue(playlistOptions.autoContinue);
    }

    private void subscribeToPlayerEvents() {

        kalturaPlayer.addListener(this, PlayerEvent.playing, event -> {
        });

        kalturaPlayer.addListener(this, PlayerEvent.replay, event -> {
            resetCountDownOptions();
        });

        kalturaPlayer.addListener(this, PlayerEvent.ended, event -> {
            log.d("ended event received");
//            if (countDownOptions != null && countDownOptions.isEventSent()) { // needed???
//                kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistCountDownEnd(currentPlayingIndex, countDownOptions));
//            }
            if (playlistAutoContinue) {
                handlePlaylistMediaEnded();
            } else {
                resetCountDownOptions();
            }
        });

        kalturaPlayer.addListener(this, PlayerEvent.seeking, event -> {
            log.d("seeking event received");

            // do nothing
            if (countDownOptions == null || event.targetPosition >= kalturaPlayer.getDuration()) {
                return;
            }

            // do nothing
            if (event.targetPosition == countDownOptions.getTimeToShowMS()) {
                return;
            }

            // start counting again
            if (event.targetPosition > (countDownOptions.getTimeToShowMS() + countDownOptions.getDurationMS())) {
                countDownOptions.setEventSent(false);
                countDownOptions.setTimeToShowMS(event.targetPosition);
                return;
            }

            // reset
            if (event.targetPosition < event.currentPosition && event.targetPosition < countDownOptions.getTimeToShowMS()) {
                countDownOptions.setTimeToShowMS(countDownOptions.getOrigTimeToShowMS());
                countDownOptions.setEventSent(false);
                return;
            }

            // start counting again
            if (countDownOptions.isEventSent() && event.targetPosition < countDownOptions.getTimeToShowMS()) {
                countDownOptions.setEventSent(false);
            }
        });

        kalturaPlayer.addListener(this, PlayerEvent.playheadUpdated, event -> {
            log.d("playheadUpdated received position = " + event.position + "/" + event.duration);

            if (countDownOptions == null) {
                CountDownOptions tmpCountDownOptions = null;
                if (playlistOptions instanceof OVPPlaylistOptions) {
                    tmpCountDownOptions = ((OVPPlaylistOptions) playlistOptions).ovpMediaOptionsList.get(currentPlayingIndex).countDownOptions;
                } else if (playlistOptions instanceof OTTPlaylistOptions) {
                    tmpCountDownOptions = ((OTTPlaylistOptions) playlistOptions).ottMediaOptionsList.get(currentPlayingIndex).countDownOptions;
                } else if (playlistOptions instanceof BasicPlaylistOptions) {
                    tmpCountDownOptions = ((BasicPlaylistOptions) playlistOptions).basicMediaOptionsList.get(currentPlayingIndex).getCountDownOptions();
                }

                if (tmpCountDownOptions == null) {
                    tmpCountDownOptions = (playlistOptions.countDownOptions != null) ? playlistOptions.countDownOptions : new CountDownOptions();
                }
                long fixedTimeToShow = (tmpCountDownOptions.getTimeToShowMS() == -1) ? event.duration - tmpCountDownOptions.getDurationMS() : tmpCountDownOptions.getTimeToShowMS();
                countDownOptions = new CountDownOptions(fixedTimeToShow, tmpCountDownOptions.getDurationMS(), tmpCountDownOptions.shouldDisplay());
            }

            if (event.position >= event.duration) {
                return;
            }

            handleCountDownEvent(event);
        });

        kalturaPlayer.addListener(this, PlayerEvent.error, event -> {
            log.e("errorEvent.error.errorType"  + " " + event.error.message + " severity = " + event.error.severity);
            if (event.error.severity == PKError.Severity.Fatal) {
                loadedMediasMap.put(currentPlayingIndex, null);
                kalturaPlayer.stop();
                if (isAutoContinueEnabled()) {
                    playNext();
                } else {
                    playItem(currentPlayingIndex + 1, false);
                }
            }
        });
    }

    private void handleCountDownEvent(PlayerEvent.PlayheadUpdated event) {
        if (countDownOptions != null && countDownOptions.shouldDisplay() && playlistAutoContinue && currentPlayingIndex + 1 <  playlist.getMediaListSize()) {
            if (event.position >= countDownOptions.getTimeToShowMS()) {
                if (!countDownOptions.isEventSent()) {
                    log.d("SEND COUNT DOWN EVENT position = " + event.position);
                    kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistCountDownStart(currentPlayingIndex, countDownOptions));
                    countDownOptions.setEventSent(true);
                    preloadNext();
                } else if (event.position >= Math.min(countDownOptions.getTimeToShowMS() + countDownOptions.getDurationMS(), event.duration)) {
                    kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistCountDownEnd(currentPlayingIndex, countDownOptions));
                    //log.d("playhead updated handlePlaylistMediaEnded");
                    handlePlaylistMediaEnded();
                }
            }
        }
    }

    private void handlePlaylistMediaEnded() {
        resetCountDownOptions();
        int playlistSize = playlist.getMediaListSize();
        if (currentPlayingIndex + 1 == playlistSize) {
            if (loopEnabled) {
                log.d("PLAYLIST REPLAY");
                replay();
            }
            log.d("PLAYLIST ENDED");
            kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistEnded(playlist));
        } else {
            log.d("PLAYLIST PLAY NEXT");
            playNext();
        }
    }

    private void resetCountDownOptions() {
        if (countDownOptions != null) {
            countDownOptions.setEventSent(false);
            countDownOptions = null;
        }
    }
}