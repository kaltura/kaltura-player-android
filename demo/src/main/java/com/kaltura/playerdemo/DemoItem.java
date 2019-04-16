package com.kaltura.playerdemo;

import com.kaltura.playkit.PKMediaEntry;

public class DemoItem {
    public final String name;
    public final String id;
    public final PKMediaEntry pkMediaEntry;

    public DemoItem(String name, String id) {
        this.id = id;
        this.name = name;
        this.pkMediaEntry = null;
    }

    public DemoItem(String name, String id, PKMediaEntry pkMediaEntry) {
        this.id = id;
        this.name = name;
        this.pkMediaEntry = pkMediaEntry;
    }



    @Override
    public String toString() {
        return name + " ➜ " + id;
    }
}
