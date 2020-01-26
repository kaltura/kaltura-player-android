package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKPlaylist;

import java.util.List;

public class PKBasicPlaylist extends PKPlaylist {
    private List<BasicMediaOptions> basicMediaOptionsList;

    public List<BasicMediaOptions> getBasicMediaOptionsList() {
        return basicMediaOptionsList;
    }

    public void setBasicMediaOptionsList(List<BasicMediaOptions> basicMediaOptionsList) {
        this.basicMediaOptionsList = basicMediaOptionsList;
    }

    @Override
    public int getMediaListSize() {
        return basicMediaOptionsList != null ? basicMediaOptionsList.size() : 0;
    }
}
