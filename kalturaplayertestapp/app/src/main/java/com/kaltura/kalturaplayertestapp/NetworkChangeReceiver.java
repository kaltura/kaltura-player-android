package com.kaltura.kalturaplayertestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Observable;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static boolean isConnected = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        getObservable().connectionChanged(isInternetOn(context));
    }

    public static class NetworkObservable extends Observable {
        private static NetworkObservable instance = null;

        private NetworkObservable() {
            // Exist to defeat instantiation.
        }

        public void connectionChanged(Boolean connected) {
            if (isConnected != connected) {
                setChanged();
                notifyObservers(connected);
                isConnected = connected;
            }
        }

        public static NetworkObservable getInstance() {
            if (instance == null) {
                instance = new NetworkObservable();
            }
            return instance;
        }

        @Override
        public int countObservers() {
            return super.countObservers();
        }
    }

    public static NetworkObservable getObservable() {
        return NetworkObservable.getInstance();
    }


    public static boolean isInternetOn(Context context) {
        ConnectivityManager conn = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
    }
}
