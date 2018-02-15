package com.kaltura.kalturaplayertestapp;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;


class DownloadFileFromURL extends AsyncTask<String, String, String> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //showDialog(progress_bar_type);
    }


    @Override
    protected String doInBackground(String... url) {
        try {
            URL fileUrl = new URL(url[0]);
            return Utils.getResponseFromHttpUrl(fileUrl);
        } catch (IOException ex) {
            return "";
        }
    }

    protected void onProgressUpdate(String... progress) {
        // setting progress percentage
        //pDialog.setProgress(Integer.parseInt(progress[0]));
    }

    @Override
    protected void onPostExecute(String fileContent) {

        //Log.d("XXX" , fileContent);
        //dismissDialog(progress_bar_type);
    }

}