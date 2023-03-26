package com.sebswebs.barcodescanner;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class FeedThread extends Thread {
    private String feedContents = "";
    private URL myUrl;

    public FeedThread(URL u) {
        myUrl =u;
    }

    public String getFeedContents() {
        return feedContents;
    }

    public void run() {
        feedContents = "";
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) myUrl.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                feedContents += line;
            }
            Log.e("BarcodeScanner", "Finished reading data from API " + new Date());
            // return the data to onPostExecute method
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
