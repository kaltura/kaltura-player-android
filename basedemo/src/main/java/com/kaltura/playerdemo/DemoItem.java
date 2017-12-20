package com.kaltura.playerdemo;

public class DemoItem {
    public final String name;
    public final String id;

    public DemoItem(String name, String id) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " ➜ " + id;
    }
}
