package com.example.marekulip.droidsor.gpxfileexporter;

import android.content.Context;
import android.util.Log;

import com.example.marekulip.droidsor.R;
import com.example.marekulip.droidsor.sensorlogmanager.SensorData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Marek Ulip on 29-Oct-17.
 */

public class GPXExporter {
    private static final String trkStartTag = "<trk>";
    private static final String trkEndTag = "</trk>";
    private static final String trksegStartTag = "<trkseg>";
    private static final String trksegEndTag = "</trkseg>";
    private static final String trkptStartTag = "<trkpt ";
    private static final String trkptEndTag = "</trkpt>"; // also includes tag for div which encapsulates all entries
    private static final String timeStartTag = "<time>";
    private static final String timeEndTag = "</time>";
    private static final String myNamespaceName = "myns";
    private static final String myNamespaceTagStart = "<"+myNamespaceName+":";
    private static final String myNamespaceTagEnd = "</"+myNamespaceName+":";
    private static final String extensionsStartTag = "<extensions>";
    private static final String extensionsEndTag = "</extensions>";
    private static final String xValueStartTag = myNamespaceTagStart+"x>";
    private static final String xValueEndTag = myNamespaceTagEnd+"x>";
    private static final String yValueStartTag = myNamespaceTagStart+"y>";;
    private static final String yValueEndTag = myNamespaceTagEnd+"y>";
    private static final String zValueStartTag = myNamespaceTagStart+"z>";;
    private static final String zValueEndTag = myNamespaceTagEnd+"z>";
    private static final String eleStartTag = "<ele>";
    private static final String eleEndTag = "</ele>";


    public static void exportLogItems(List<SensorData> entries, String fileName, Context context){
        DroidsorExporter.writeToFile(createGPXfromDatas(entries,context),fileName+".gpx",context);
    }

    /*private static void prepDateFormat(){
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
    }*/

    private static String createGPXfromDatas(List<SensorData> datas,Context context){
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        int size = datas.size();
        StringBuilder sb = new StringBuilder(loadBaseHTMLFromFile(context));
        //StringBuilder sbBody = new StringBuilder("");
        sb.append(trkStartTag);
        sb.append(trksegStartTag).append(System.lineSeparator());
        //
        SensorData data;
        for(int i = 0; i<size;i++){
            data = datas.get(i);
            sb.append(trkptStartTag).append("lat=\"").append(data.latitude).append("\" lon=\"").append(data.longitude).append("\">").append(System.lineSeparator())
                    //.append(timeStartTag).append(DateFormat.getDateTimeInstance().format(new Date(data.time))).append(timeEndTag).append(System.lineSeparator())
                    .append(timeStartTag).append(df.format(new Date(data.time))).append(timeEndTag).append(System.lineSeparator())
                    .append(eleStartTag).append(data.altitude).append(eleEndTag).append(System.lineSeparator())
                    .append(extensionsStartTag).append(System.lineSeparator())
                    .append(xValueStartTag).append(data.values.x).append(xValueEndTag).append(System.lineSeparator())
                    .append(yValueStartTag).append(data.values.y).append(yValueEndTag).append(System.lineSeparator())
                    .append(zValueStartTag).append(data.values.z).append(zValueEndTag).append(System.lineSeparator())
                    .append(extensionsEndTag).append(trkptEndTag).append(System.lineSeparator());

            //sb.append(trkStartTag).append(i+1).append(trkEndTag).append(i+1).append(". ").append(entries.get(i).getEntrieName()).append(listItemEnd);
            //sbBody.append(entrieDivStart).append(i+1).append(trkEndTag).append(i+1).append(". ").append(entries.get(i).getEntrieName()).append(entrieDivStartEnd).append(entries.get(i).getEntrieBody(false)).append(entrieDivEnd);
        }
        sb.append(trksegEndTag).append(trkEndTag).append("</gpx>");
        Log.d("Result", "createHTMLfromEntries: "+sb);
        return sb.toString();
    }

    private static String loadBaseHTMLFromFile(Context context){
        StringBuilder builder = new StringBuilder("");
        try {
            builder = new StringBuilder("");
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.gpxbase)));
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            br.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
