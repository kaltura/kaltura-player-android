package com.kaltura.kalturaplayertestapp.models;

/**
 * Created by gilad.nadav on 15/01/2018.
 */

public class Configuration {

    public static final int JSON   = 0;
    public static final int FOLDER = 1;

    private String id;
    private String title;
    private String json;
    private int type;

    public Configuration() {}

    public Configuration(String id, String title, String json, int type) {
        this.id = id;
        this.title = title;
        this.json = json;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
