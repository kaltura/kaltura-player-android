package com.kaltura.tvplayer.utils;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MapTokenResolver implements TokenResolver {
    private final Map<String, String> map = new HashMap<>();
    private String[] sources = new String[0];
    private String[] destinations = new String[0];

    protected void set(String key, String value) {
        if (!TextUtils.isEmpty(key)) {
            map.put(key, value);
        }
    }

    protected void removeAll(List<String> keys) {
        for (String key : keys) {
            map.remove(key);
        }
    }

    protected void rebuild() {
        sources = new String[map.size()];
        destinations = new String[map.size()];

        final Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            final Map.Entry<String, String> entry = it.next();
            sources[i] = "{{" + entry.getKey() + "}}";
            destinations[i] = entry.getValue();
            i++;
        }
    }

    @Override
    public String resolve(String string) {
        if (string == null || sources.length == 0 || destinations.length == 0) {
            return string;
        }
        return TextUtils.replace(string, sources, destinations).toString();
    }
}