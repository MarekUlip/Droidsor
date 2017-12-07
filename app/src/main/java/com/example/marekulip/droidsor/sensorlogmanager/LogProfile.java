package com.example.marekulip.droidsor.sensorlogmanager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fredred on 28.10.2017.
 */

public class LogProfile {
    private String profileName;
    private List<LogProfileItem> logItems;
    private boolean saveGPS;
    private int GPSFrequency;

    public LogProfile(String profileName, List<LogProfileItem> logItems){
        this.setProfileName(profileName);
        this.setLogItems(logItems);
    }

    public LogProfile(){
        this("",new ArrayList<LogProfileItem>());
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public List<LogProfileItem> getLogItems() {
        return logItems;
    }

    public void setLogItems(List<LogProfileItem> logItems) {
        this.logItems = logItems;
    }

    public boolean isSaveGPS() {
        return saveGPS;
    }

    public void setSaveGPS(boolean saveGPS) {
        this.saveGPS = saveGPS;
    }

    public int getGPSFrequency() {
        return GPSFrequency;
    }

    public void setGPSFrequency(int GPSFrequency) {
        this.GPSFrequency = GPSFrequency;
    }
}
