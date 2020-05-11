package com.kaltura.tvplayer;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.providers.ott.OTTMediaAsset;
import com.kaltura.playkit.providers.ovp.OVPMediaAsset;
import com.kaltura.tvplayer.playlist.BasicMediaOptions;
import com.kaltura.tvplayer.playlist.BasicPlaylistOptions;
import com.kaltura.tvplayer.playlist.CountDownOptions;
import com.kaltura.tvplayer.playlist.OTTPlaylistOptions;
import com.kaltura.tvplayer.playlist.OVPPlaylistIdOptions;
import com.kaltura.tvplayer.playlist.OVPPlaylistOptions;
import com.kaltura.tvplayer.playlist.PKBasicPlaylist;
import com.kaltura.tvplayer.playlist.PKPlaylistType;
import com.kaltura.tvplayer.playlist.PlaylistController;
import com.kaltura.tvplayer.playlist.PlaylistEvent;
import com.kaltura.tvplayer.playlist.PlaylistOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PKPlaylistController implements PlaylistController {

    private static final PKLog log = PKLog.get("PlaylistController");

    private KalturaPlayer kalturaPlayer;
    private PKPlaylist playlist;
    private PKPlaylistType playlistType;
    private PlaylistOptions playlistOptions;
    private @Nullable CountDownOptions playlistCountDownOptions;

    private int currentPlayingIndex = -1;
    private boolean playlistAutoContinue = true;
    private boolean loopEnabled;
    private boolean recoverOnError;

    private List<PKPlaylistMedia> origlPlaylistEntries;
    private Map<String, PKMediaEntry> loadedMediasMap; // map of the media id and it's PKMediaEntry (ovp/ott in entryId format basic any string that ws given by user as id)

    private enum CacheMediaType {
        Prev, Current, Next
    }

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
        if (playlist != null && playlist.getMediaList() != null && !playlist.getMediaList().isEmpty()) {
            return playlist.getMediaList().get(currentPlayingIndex);
        }
        return null;
    }

    @Override
    public int getCurrentMediaIndex() {
        return currentPlayingIndex;
    }

    @Override
    public CountDownOptions getCurrentCountDownOptions() {
        return playlistCountDownOptions;
    }

    @Override
    public void disableCountDown() {
        if (playlistCountDownOptions != null) {
            playlistCountDownOptions.setShouldDisplay(false);
        }
    }

    @Override
    public void preloadNext() {
        if (kalturaPlayer.getTvPlayerType() != KalturaPlayer.Type.basic) {
            if (playlist != null && currentPlayingIndex + 1 == playlist.getMediaListSize()) {
                preloadItem(0);
                return;
            }
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

        if (loadedMediasMap.containsKey(getCacheMediaId(CacheMediaType.Current))) {
            return;
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
        playlistCountDownOptions = null;
        if (playlist instanceof PKBasicPlaylist && ((PKBasicPlaylist) playlist).getBasicMediaOptionsList() == null) {
            return;
        } else if (!(playlist instanceof PKBasicPlaylist) && (playlist == null ||  playlist.getMediaList() == null)) {
            return;
        }

        boolean isValidIndex = isValidPlaylistIndex(index);
        if (!isValidIndex) {
            return;
        }
        String mediaId = getCacheMediaId(CacheMediaType.Current);

        if (loadedMediasMap.containsKey(mediaId) && loadedMediasMap.get(mediaId) != null) {
            kalturaPlayer.setMedia(loadedMediasMap.get(mediaId));
            return;
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
            OVPMediaAsset ovpMediaAsset = ovpMediaOptions.getOvpMediaAsset();
            if (ovpMediaAsset != null) {
                if (ovpMediaAsset.getKs() == null) {
                    ovpMediaAsset.setKs(ovpPlaylistOptions.ks);
                }
                if (ovpMediaAsset.getReferrer() == null) {
                    ovpMediaAsset.setReferrer(kalturaPlayer.getInitOptions().referrer);
                }
            }
        } else { // PlaylistId case
            OVPPlaylistIdOptions ovpPlaylistIdOptions = (OVPPlaylistIdOptions) playlistOptions;
            OVPMediaAsset ovpMediaAsset = new OVPMediaAsset();
            ovpMediaAsset.setEntryId(playlist.getMediaList().get(index).getId());
            ovpMediaAsset.setKs((playlist.getMediaList().get(index).getKs() != null) ? playlist.getMediaList().get(index).getKs() : playlist.getKs());
            ovpMediaAsset.setReferrer(kalturaPlayer.getInitOptions().referrer);

            ovpMediaOptions = new OVPMediaOptions(ovpMediaAsset);
            ovpMediaOptions.setUseApiCaptions(ovpPlaylistIdOptions.useApiCaptions);
        }

        kalturaPlayer.loadMedia(ovpMediaOptions, (entry, loadError) -> {
            if (loadError != null) {
                log.e("OVPMedia error = " + loadError.getMessage());
                if (kalturaPlayer.getMessageBus() == null) {
                    return;
                }
                kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistLoadMediaError(index, new ErrorElement(loadError.getMessage(), loadError.getCode())));
            } else {
                if (playlist.getMediaList() != null && !playlist.getMediaList().isEmpty() && playlist.getMediaList().get(index) != null) {
                    loadedMediasMap.put(getCacheMediaId(CacheMediaType.Current), entry);
                    log.d("OVPMedia onEntryLoadComplete entry = " + entry.getId());
                } else {
                    log.e("OVPMedia onEntryLoadComplete playlist.getMediaList().get(" + index + ") == null");
                }
            }
        });
    }

    private void playItemOTT(int index) {
        log.d("playItemOTT  " + index) ;

        OTTPlaylistOptions ottPlaylistOptions =  (OTTPlaylistOptions) playlistOptions;
        OTTMediaOptions ottMediaOptions = getNextMediaOptions(index, ottPlaylistOptions);
        if (ottMediaOptions == null) {
            return; // error cannot play any next item
        }
        OTTMediaAsset ottMediaAsset = ottMediaOptions.getOttMediaAsset();
        if (ottMediaAsset != null) {
            if (ottMediaAsset.getKs() == null) {
                ottMediaAsset.setKs(ottPlaylistOptions.ks);
            }
            if (ottMediaAsset.getReferrer() == null) {
                ottMediaAsset.setReferrer(kalturaPlayer.getInitOptions().referrer);
            }
        }

        kalturaPlayer.loadMedia(ottMediaOptions, (entry, loadError) -> {
            if (loadError != null) {
                log.e(loadError.getMessage());
                if (kalturaPlayer.getMessageBus() == null) {
                    return;
                }
                String errMsg = loadError.getMessage();
                if (TextUtils.equals(errMsg,"Asset not found")) {
                    errMsg  = "Asset: [" + ottMediaOptions.getOttMediaAsset().getAssetId() + "] not found";
                }
                kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistLoadMediaError(index, new ErrorElement(errMsg, loadError.getCode())));
            }
            else {
                if (playlist.getMediaList() != null && !playlist.getMediaList().isEmpty() && playlist.getMediaList().get(index) != null) {
                    loadedMediasMap.put(getCacheMediaId(CacheMediaType.Current), entry);
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

        List<PKPlaylistMedia> basicMediaOptionsList = ((PKBasicPlaylist) playlist).getBasicMediaOptionsList();
        if (basicMediaOptionsList != null && !basicMediaOptionsList.isEmpty() && basicMediaOptionsList.get(index) != null) {
            loadedMediasMap.put(getCacheMediaId(CacheMediaType.Current), pkMediaEntry);
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
            if (kalturaPlayer.getMessageBus() == null) {
                return false;
            }
            kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistError
                    (new ErrorElement(errorMessage, errorCode)));
        }
        return isValidIndex;
    }

    // incase the index that was requested to be played does not contain a valid media
    // the controller will try to find the next media which is avaliable for playback
    private OVPMediaOptions getNextMediaOptions(int index, OVPPlaylistOptions ovpPlaylistOptions) {
        while (true) {
            List<OVPMediaOptions> ovpMediaOptionsList = ovpPlaylistOptions.ovpMediaOptionsList;
            if (!(index < ovpMediaOptionsList.size())) {
                break;
            }
            if (ovpMediaOptionsList.get(index) != null) {
                return ovpMediaOptionsList.get(index);
            }
            index++;
        }
        return null;
    }

    // incase the index that was requested to be played does not contain a valid media
    // the controller will try to find the next media which is avaliable for playback
    private OTTMediaOptions getNextMediaOptions(int index, OTTPlaylistOptions ottPlaylistOptions) {
        while(true) {
            List<OTTMediaOptions> ottMediaOptionsList = ottPlaylistOptions.ottMediaOptionsList;
            if (!(index < ottMediaOptionsList.size())) {
                break;
            }
            if (ottMediaOptionsList.get(index) != null) {
                return ottMediaOptionsList.get(index);
            }
            index++;
        }
        return null;
    }

    // incase the index that was requested to be played does not contain a valid media
    // the controller will try to find the next media which is avaliable for playback
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
        kalturaPlayer.pause();
        int playlistSize = playlist.getMediaListSize();
        if (currentPlayingIndex + 1 == playlistSize) {
            if (loopEnabled) {
                replay();
                return;
            }
            log.d("Error invalid index - Ignore playNext - invalid index!");
            //playPrev(); for now do not replat last media if play next is not valid
            return;
        }

        if ((kalturaPlayer.getTvPlayerType() != KalturaPlayer.Type.basic && playlist.getMediaList().get(currentPlayingIndex + 1) == null) ||
                (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic && ((PKBasicPlaylist)playlist).getBasicMediaOptionsList().get(currentPlayingIndex + 1) == null) ||
                (loadedMediasMap.containsKey(getCacheMediaId(CacheMediaType.Next)) && loadedMediasMap.get(getCacheMediaId(CacheMediaType.Next)) == null)
        ) {
            if (recoverOnError) {
                ++currentPlayingIndex;
                playNext();
                return;
            }
        }
        playItem(++currentPlayingIndex);
    }

    @Override
    public void playPrev() {
        log.d("playPrev");
        kalturaPlayer.pause();
        int playlistSize = playlist.getMediaListSize();
        if (currentPlayingIndex - 1 < 0) {
            if (loopEnabled) {
                currentPlayingIndex = playlistSize - 1;
                playItem(currentPlayingIndex);
            } else {
                log.d("Error invalid index - Ignore playPrev - invalid index!");
                //playNext(); for now do not replay last media if play prev is not valid
            }
            return;
        }

        if ((kalturaPlayer.getTvPlayerType() != KalturaPlayer.Type.basic && playlist.getMediaList().get(currentPlayingIndex - 1) == null) ||
                (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.basic && ((PKBasicPlaylist)playlist).getBasicMediaOptionsList().get(currentPlayingIndex - 1) == null) ||
                (loadedMediasMap.containsKey(getCacheMediaId(CacheMediaType.Prev)) && loadedMediasMap.get(getCacheMediaId(CacheMediaType.Prev)) == null)) {
            if (recoverOnError) {
                if (currentPlayingIndex - 1 < 0) {
                    playItem(currentPlayingIndex, isAutoContinueEnabled());
                } else {
                    --currentPlayingIndex;
                    playPrev();
                }
                return;
            }
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
    public boolean isMediaLoaded(String mediaId) {
        log.d("isMediaLoaded mediaId = " + mediaId);
        if (loadedMediasMap == null) {
            return false;
        }
        return loadedMediasMap.containsKey(mediaId) && loadedMediasMap.get(mediaId) != null;
    }

    @Override
    public void setLoop(boolean mode) {
        log.d("setLoop mode = " + mode);
        loopEnabled = mode;
        if (kalturaPlayer.getMessageBus() == null) {
            return;
        }
        kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistLoopStateChanged(mode));
    }

    @Override
    public boolean isLoopEnabled() {
        return loopEnabled;
    }


    @Override
    public void setRecoverOnError(boolean mode) {
        log.d("setRecoverOnError mode = " + mode);
        recoverOnError = mode;
    }

    @Override
    public boolean isRecoverOnError() {
        return recoverOnError;
    }

    @Override
    public void setAutoContinue(boolean mode) {
        log.d("setAutoContinue mode = " + mode);
        playlistAutoContinue = mode;
        if (kalturaPlayer.getMessageBus() == null) {
            return;
        }
        kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistAutoContinueStateChanged(mode));
    }


    @Override
    public boolean isAutoContinueEnabled() {
        return playlistAutoContinue;
    }

    public void reset() {
        log.d("reset");
        currentPlayingIndex = -1;
        loopEnabled  = false;
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
        if (kalturaPlayer != null) {
            kalturaPlayer.removeListeners(this);
        }
    }

    @Override
    public void setPlaylistOptions(PlaylistOptions playlistOptions) {
        this.playlistOptions = playlistOptions;
        setLoop(playlistOptions.loopEnabled);
        setAutoContinue(playlistOptions.autoContinue);
        setRecoverOnError(playlistOptions.recoverOnError);
    }

    @Override
    public void setPlaylistCountDownOptions(@Nullable CountDownOptions playlistCountDownOptions) {
        if (playlistOptions == null && playlistCountDownOptions != null) {
            return;
        }
        playlistOptions.playlistCountDownOptions = playlistCountDownOptions;
    }

    private void subscribeToPlayerEvents() {

        kalturaPlayer.addListener(this, PlayerEvent.playing, event -> {
        });

        kalturaPlayer.addListener(this, PlayerEvent.replay, event -> {
            resetCountDownOptions();
        });

        kalturaPlayer.addListener(this, PlayerEvent.ended, event -> {
            log.d("ended event received");
            handlePlaylistMediaEnded();
        });

        kalturaPlayer.addListener(this, PlayerEvent.seeking, event -> {
            log.d("seeking event received");

            // do nothing
            if (playlistCountDownOptions == null || event.targetPosition >= kalturaPlayer.getDuration()) {
                return;
            }

            // do nothing
            if (event.targetPosition == playlistCountDownOptions.getTimeToShowMS()) {
                return;
            }

            // start counting again
            if (event.targetPosition > (playlistCountDownOptions.getTimeToShowMS() + playlistCountDownOptions.getDurationMS())) {
                playlistCountDownOptions.setEventSent(false);
                playlistCountDownOptions.setTimeToShowMS(event.targetPosition);
                return;
            }

            // reset
            if (event.targetPosition < event.currentPosition && event.targetPosition < playlistCountDownOptions.getTimeToShowMS()) {
                playlistCountDownOptions.setTimeToShowMS(playlistCountDownOptions.getOrigTimeToShowMS());
                playlistCountDownOptions.setEventSent(false);
                return;
            }

            // start counting again
            if (playlistCountDownOptions.isEventSent() && event.targetPosition < playlistCountDownOptions.getTimeToShowMS()) {
                playlistCountDownOptions.setEventSent(false);
            }
        });

        kalturaPlayer.addListener(this, PlayerEvent.playheadUpdated, event -> {
            //log.d("playheadUpdated received position = " + event.position + "/" + event.duration);
            
            if (playlistCountDownOptions == null) {
                CountDownOptions tmpCountDownOptions = null;
                if (playlistOptions instanceof OVPPlaylistOptions) {
                    tmpCountDownOptions = ((OVPPlaylistOptions) playlistOptions).ovpMediaOptionsList.get(currentPlayingIndex).playlistCountDownOptions;
                } else if (playlistOptions instanceof OTTPlaylistOptions) {
                    tmpCountDownOptions = ((OTTPlaylistOptions) playlistOptions).ottMediaOptionsList.get(currentPlayingIndex).playlistCountDownOptions;
                } else if (playlistOptions instanceof BasicPlaylistOptions) {
                    tmpCountDownOptions = ((BasicPlaylistOptions) playlistOptions).basicMediaOptionsList.get(currentPlayingIndex).getPlaylistCountDownOptions();
                }

                if (tmpCountDownOptions == null) {
                    tmpCountDownOptions = (playlistOptions.playlistCountDownOptions != null) ? playlistOptions.playlistCountDownOptions : new CountDownOptions();
                }
                long fixedTimeToShow = (tmpCountDownOptions.getTimeToShowMS() == -1) ? event.duration - tmpCountDownOptions.getDurationMS() : tmpCountDownOptions.getTimeToShowMS();
                playlistCountDownOptions = new CountDownOptions(fixedTimeToShow, tmpCountDownOptions.getDurationMS(), tmpCountDownOptions.shouldDisplay());
            }

            if (event.position >= event.duration) {
                return;
            }

            handleCountDownEvent(event);
        });

        kalturaPlayer.addListener(this, PlayerEvent.error, event -> {
            log.e("errorEvent.error.errorType"  + " " + event.error.message + " severity = " + event.error.severity);
            if (event.error.severity == PKError.Severity.Fatal) {
                kalturaPlayer.stop();
                if (!isRecoverOnError()) {
                    return;
                }
                String mediaId = getCacheMediaId(CacheMediaType.Current);
                loadedMediasMap.put(mediaId, null);
                if (isAutoContinueEnabled()) {
                    playNext();
                } else {
                    int playlistSize = playlist.getMediaListSize();
                    if (currentPlayingIndex + 1 < playlistSize) {
                        playItem(currentPlayingIndex + 1, false);
                    } else {
                        if (loopEnabled) {
                            playNext();
                        } else {
                            playPrev();
                        }
                    }
                }
            }
        });
    }

    private String getCacheMediaId(CacheMediaType cacheMediaType) {

        if (currentPlayingIndex < 0 || currentPlayingIndex >= playlist.getMediaList().size()) {
            return "";
        }

        int mediaListIndex = currentPlayingIndex;
        if (cacheMediaType == CacheMediaType.Next) {
            mediaListIndex += 1;
        } else if (cacheMediaType == CacheMediaType.Prev) {
            mediaListIndex -= 1;
        }

        String mediaId = playlist.getMediaList().get(mediaListIndex).getId();
        if (kalturaPlayer.getTvPlayerType() == KalturaPlayer.Type.ott) {
            mediaId = playlist.getMediaList().get(mediaListIndex).getMetadata().get("entryId");
        }
        return mediaId;
    }

    private void handleCountDownEvent(PlayerEvent.PlayheadUpdated event) {
        if (playlistCountDownOptions != null && playlistCountDownOptions.shouldDisplay() && playlistAutoContinue) {
            if (event.position >= playlistCountDownOptions.getTimeToShowMS()) {
                if (kalturaPlayer.getMessageBus() == null) {
                    return;
                }
                int playlistSize = playlist.getMediaListSize();
                boolean isLastMediaInPlaylist = ((currentPlayingIndex + 1) == playlistSize);

                if (isLastMediaInPlaylist && !loopEnabled) {
                   return;
                }

                if (!playlistCountDownOptions.isEventSent()) {
                    log.d("SEND COUNT DOWN EVENT position = " + event.position);
                    kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistCountDownStart(currentPlayingIndex, playlistCountDownOptions));
                    playlistCountDownOptions.setEventSent(true);
                    preloadNext();
                } else if (event.position >= Math.min(playlistCountDownOptions.getTimeToShowMS() + playlistCountDownOptions.getDurationMS(), event.duration)) {
                    kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistCountDownEnd(currentPlayingIndex, playlistCountDownOptions));
                    //log.d("playhead updated handlePlaylistMediaEnded");
                    handlePlaylistMediaEnded();
                }
            }
        }
    }

    private void handlePlaylistMediaEnded() {
        log.d("PLAYLIST handlePlaylistMediaEnded");
        resetCountDownOptions();
        int playlistSize = playlist.getMediaListSize();
        boolean isLastMediaInPlaylist = ((currentPlayingIndex + 1) == playlistSize);

        if (isLastMediaInPlaylist) {
            log.d("PLAYLIST ENDED");
            if (kalturaPlayer.getMessageBus() == null) {
                return;
            }
            kalturaPlayer.getMessageBus().post(new PlaylistEvent.PlaylistEnded(playlist));
            if (loopEnabled) {
                log.d("PLAYLIST REPLAY");
                replay();
            }
        } else {
            if (playlistAutoContinue) {
                log.d("PLAYLIST PLAY NEXT");
                playNext();
            }
        }
    }

    private void resetCountDownOptions() {
        if (playlistCountDownOptions != null) {
            playlistCountDownOptions.setEventSent(false);
            playlistCountDownOptions = null;
        }
    }
}
