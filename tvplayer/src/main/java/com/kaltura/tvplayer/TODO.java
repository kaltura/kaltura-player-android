package com.kaltura.tvplayer;

public class TODO extends RuntimeException {
    public TODO(String message) {
        super(message);
    }

    public TODO() {
        super("Not implemented");
    }
}
