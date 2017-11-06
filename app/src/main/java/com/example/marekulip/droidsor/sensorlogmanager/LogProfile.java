package com.example.marekulip.droidsor.sensorlogmanager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fredred on 28.10.2017.
 */

public class LogProfile {
    private String profileName;
    private List<LogProfileItem> logItems;

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
}
