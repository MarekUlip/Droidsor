package com.marekulip.droidsor.gpxfileexporter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.marekulip.droidsor.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Class used for exporting files from the app.
 * Created by Marek Ulip on 29-Oct-17.
 */

public class DroidsorExporter {

    public static final int WRITE_EXTERNAL_STORAGE_ID = 1;
    public static final int READ_EXTERNAL_STORAGE_ID = 2;
    private final static File path = Environment.getExternalStorageDirectory();//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

    /**
     * Checks whether the app has permission to write to an external storage. If not it tries to obtain required permission.
     * @param context Context of an activity which will be used to ask for permission
     * @return true if permission is already granted
     */
    public static boolean checkForWritePermission(Context context){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale((AppCompatActivity)context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(context,R.string.permission_explain_write, Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions((AppCompatActivity)context,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, DroidsorExporter.WRITE_EXTERNAL_STORAGE_ID);
            return false;
        }
        return true;
    }

    /**
     * Writes specified string to a specified file
     * @param dataToSave Text to save to a file
     * @param fileName Name of a file
     * @param context Context used for making file immediately visible from computer. This context should be Application. If not null pointer exceptions may arise.
     * @return true if file write was successful
     */
    public static boolean writeToFile(String dataToSave,String fileName,Context context){
        if(isExternalStorageWritable()) {
            try {
                fileName = getAndroidFriendlyFileName(fileName);
                File folder = new File(path, context.getString(R.string.app_name));
                if(!folder.exists()){
                    boolean rv = folder.mkdir();
                }
                File file = new File(folder,fileName);
                file.createNewFile();
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(dataToSave);
                myOutWriter.close();
                fOut.close();
                //Make file visible on PC
                MediaScannerConnection.scanFile(context,new String[]{file.getPath()},null,null);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    /**
     * Formats provided string so it is usable in Android environment
     * @param toFormat string to be formatted
     * @return formatted Android friendly string
     */
    private static String getAndroidFriendlyFileName(String toFormat){
        return toFormat.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
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
