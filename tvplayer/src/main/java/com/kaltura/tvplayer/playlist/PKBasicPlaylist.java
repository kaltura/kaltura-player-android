package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;

import java.util.List;

public class PKBasicPlaylist extends PKPlaylist {
    private List<PKPlaylistMedia> basicMediaOptionsList;

    public List<PKPlaylistMedia> getBasicMediaOptionsList() {
        return basicMediaOptionsList;
    }

    public void setBasicMediaOptionsList(List<PKPlaylistMedia> basicMediaOptionsList) {
        this.basicMediaOptionsList = basicMediaOptionsList;
    }

    @Override
    public int getMediaListSize() {
        return basicMediaOptionsList != null ? basicMediaOptionsList.size() : 0;
    }

    @Override
    public List<PKPlaylistMedia> getMediaList() {
        return getBasicMediaOptionsList();
    }
}
