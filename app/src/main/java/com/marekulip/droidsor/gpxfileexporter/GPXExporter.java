package com.marekulip.droidsor.gpxfileexporter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.marekulip.droidsor.R;
import com.marekulip.droidsor.sensorlogmanager.SensorData;
import com.marekulip.droidsor.sensorlogmanager.SensorsEnum;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Class for creating GPX format file.
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
    private static final String myNamespaceName = "droidsor";
    private static final String myNamespaceTagStart = "<"+myNamespaceName+":";
    private static final String myNamespaceTagEnd = "</"+myNamespaceName+":";
    private static final String extensionsStartTag = "<extensions>";
    private static final String extensionsEndTag = "</extensions>";
    private static final String xValueStartTag = myNamespaceTagStart+"x>";
    private static final String xValueEndTag = myNamespaceTagEnd+"x>";
    private static final String yValueStartTag = myNamespaceTagStart+"y>";
    private static final String yValueEndTag = myNamespaceTagEnd+"y>";
    private static final String zValueStartTag = myNamespaceTagStart+"z>";
    private static final String zValueEndTag = myNamespaceTagEnd+"z>";
    private static final String eleStartTag = "<ele>";
    private static final String eleEndTag = "</ele>";
    private static final String vdopStartTag = "<vdop>";
    private static final String vdopEndTag = "</vdop>";
    private static final String hdopStartTag = "<hdop>";
    private static final String hdopEndTag = "</hdop>";
    private static final String speedStartTag = "<speed>";
    private static final String speedEndTag = "</speed>";
    private static final String fixStartTag = "<fix>";
    private static final String fixEndTag = "</fix>";
    private static final DecimalFormat decimalFormat;
    private static final int timeGap = 500;
    public static final String GPX_LONGITUDE = "gpx_longitude";
    public static final String GPX_LATITUDE = "gpx_latitude";

    static {
        //Init decimal formatting
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        decimalFormat =  new DecimalFormat("##.#####",symbols);
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
    }


    /**
     * Exports provided sensor data to a file
     * @param entries List of sensor data to be exported
     * @param fileName Name of a file
     * @param context Should be application context when using with large files
     */
    public static void exportLogItems(List<SensorData> entries, String fileName, Context context){
        DroidsorExporter.writeToFile(createGPXfromDatas(entries,context),fileName+".gpx",context);
    }

    /**
     * Create formated GPX string from provided data
     * @param datas Data to be transformed into GPX
     * @param context should be application context. It is used to get sensor names from strings resources file.
     * @return GPX formatted  string
     */
    private static String createGPXfromDatas(List<SensorData> datas,Context context){
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        int size = datas.size();
        StringBuilder sb = new StringBuilder(loadBaseHTMLFromFile(context));
        sb.append(trkStartTag);
        sb.append(trksegStartTag).append(System.lineSeparator());
        SensorData data;
        SensorsEnum sensorsEnum;
        long lastTime =  datas.get(0).time;
        String[] sensorXMLName;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        double longitude = prefs.getFloat(GPX_LONGITUDE,0);
        double latitude = prefs.getFloat(GPX_LATITUDE,0);
        for(int i = 0; i<size;i++){
            data = datas.get(i);
            sb.append(System.lineSeparator()).append(trkptStartTag);
            if(data.latitude>=0)sb.append("lat=\"").append(data.latitude).append("\" lon=\"").append(data.longitude).append("\">");
            else sb.append("lat=\"").append(latitude).append("\" lon=\"").append(longitude).append("\">");
            sb.append(System.lineSeparator()).append(timeStartTag).append(df.format(new Date(data.time))).append(timeEndTag).append(System.lineSeparator());
            if(data.altitude >= 0)sb.append(eleStartTag).append(data.altitude).append(eleEndTag).append(System.lineSeparator());
            sb.append(fixStartTag);
            if(data.latitude>=0&&data.altitude>=0)sb.append("3d");
            else if(data.latitude>=0)sb.append("2d");
            else sb.append("none");
            sb.append(fixEndTag).append(System.lineSeparator());
            if(data.accuracy>=0){
                sb.append(hdopStartTag).append((int)(data.accuracy)/5).append(hdopEndTag).append(System.lineSeparator());
            }
            sb.append(extensionsStartTag).append(System.lineSeparator());
            if(data.speed>=0)sb.append(speedStartTag).append(data.speed).append(speedEndTag).append(System.lineSeparator());
            for(; i<size;i++){
                data = datas.get(i);
                if(data.time - lastTime > timeGap){
                    lastTime = data.time;
                    i--;
                    break;
                }
                sensorsEnum = SensorsEnum.resolveEnum(data.sensorType);
                sensorXMLName = sensorsEnum.getSensorNamesXmlFriendly(context);
                sb.append(myNamespaceTagStart).append(sensorXMLName[0]).append(">").append(System.lineSeparator());
                switch(sensorsEnum.itemCount){
                    case 1:
                        sb.append(xValueStartTag).append(decimalFormat.format(data.values.x)).append(xValueEndTag).append(System.lineSeparator());
                        break;
                    case 2:
                        sb.append(xValueStartTag).append(decimalFormat.format(data.values.x)).append(xValueEndTag).append(System.lineSeparator())
                        .append(yValueStartTag).append(decimalFormat.format(data.values.y)).append(yValueEndTag).append(System.lineSeparator());
                        break;
                    case 3:
                        sb.append(xValueStartTag).append(decimalFormat.format(data.values.x)).append(xValueEndTag).append(System.lineSeparator())
                        .append(yValueStartTag).append(decimalFormat.format(data.values.y)).append(yValueEndTag).append(System.lineSeparator())
                        .append(zValueStartTag).append(decimalFormat.format(data.values.z)).append(zValueEndTag).append(System.lineSeparator());
                    break;

                }
                sb.append(myNamespaceTagEnd).append(sensorXMLName[1]).append(">").append(System.lineSeparator());
            }

            sb.append(extensionsEndTag).append(trkptEndTag).append(System.lineSeparator());

        }
        sb.append(trksegEndTag).append(trkEndTag).append("</gpx>");
        return sb.toString();
    }

    /**
     * Loads header of GPX file from resources.
     * @param context context to be used to load the resource
     * @return GPX header
     */
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
