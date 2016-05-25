package com.example.unzi.offlinemaps;


import android.app.NotificationManager;
import android.content.Context;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class DownloadFile {
    private final static String TAG = "DownloadFile";


    static double downloadedSize = 0;
    static double totalSize = 0;
    static String download_file_path = "http://accessible-serv.lasige.di.fc.ul.pt/~lost/LostMap/world.sqlitedb";

    public static boolean getMapDatabase(final Context context) {

        try {

            URL url = new URL(download_file_path);
            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            Looper.prepare();
            // connect
            urlConnection.connect();

            // set the path where we want to save the file
            Log.d(TAG, context.getFilesDir() + "/mapapp/");
            File SDCardRoot = new File(context.getFilesDir() + "/mapapp/");
            if (!SDCardRoot.exists()) {
                SDCardRoot.mkdir();
            }
            // create a new file, to save the downloaded file
            File file = new File(SDCardRoot, "world.sqlitedb");

            FileOutputStream fileOutput = new FileOutputStream(file);

            // Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();

            // this is the total size of the file which we are downloading
            totalSize = urlConnection.getContentLength();


            // create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;

            }
            // close the output stream when complete //
            fileOutput.close();
            Log.d(TAG, "finito db");
            return true;

        } catch (final MalformedURLException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Deprecated
    public DownloadFile(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                downloadFile(context);
            }
        }).start();
    }

    //Deprecated
    void downloadFile(Context context) {

        try {
            URL url = new URL(download_file_path);
            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            Looper.prepare();
            // connect
            urlConnection.connect();

            // set the path where we want to save the file
            Log.d(TAG,context.getFilesDir()+"/mapapp/" );
            File SDCardRoot = new File (context.getFilesDir()+"/mapapp/");
            if(!SDCardRoot.exists()){
                SDCardRoot.mkdir();
            }
            // create a new file, to save the downloaded file
            File file = new File(SDCardRoot, "world.sqlitedb");

            FileOutputStream fileOutput = new FileOutputStream(file);

            // Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();

            // this is the total size of the file which we are downloading
            totalSize = urlConnection.getContentLength();


            // create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;

            }
            // close the output stream when complete //
            fileOutput.close();
            Log.d(TAG, "finito db");

            return ;

        } catch (final MalformedURLException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

}

