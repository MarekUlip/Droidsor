package com.example.marekulip.droidsor.gpxfileexporter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.marekulip.droidsor.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by Marek Ulip on 29-Oct-17.
 */

public class DroidsorExporter {

    public static final int WRITE_EXTERNAL_STORAGE_ID = 1;
    public static final int READ_EXTERNAL_STORAGE_ID = 2;
    private final static File path = Environment.getExternalStorageDirectory();//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

    public static boolean checkForWritePermission(Context context){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale((AppCompatActivity)context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(context,"Just allow that" /*R.string.permission_explain_write*/, Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions((AppCompatActivity)context,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, DroidsorExporter.WRITE_EXTERNAL_STORAGE_ID);
            return false;
        }
        return true;
    }

    public static boolean writeToFile(String dataToSave,String fileName,Context context){
        if(isExternalStorageWritable()) {
            try {
                File folder = new File(path, context.getString(R.string.app_name));
                if(!folder.exists()){
                    boolean rv = folder.mkdir();
                }
                File file = new File(folder,fileName);
                Log.d("sdd", "writeToFile: "+file.getPath());
                //path.mkdirs();
                //Log.d("sdd", "writeToFile: "+fileName);
                file.createNewFile();
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(dataToSave);
                myOutWriter.close();
                fOut.close();
                MediaScannerConnection.scanFile(context,new String[]{file.getPath()},null,null);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    /** Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /** Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
