package com.example.marekulip.droidsor.sensorlogmanager;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing log profile.
 * Created by Marek Ulip on 28.10.2017.
 */

public class LogProfile {
    /**
     * Id of log profile
     */
    private long id;
    /**
     * Name of log profile
     */
    private String profileName;
    /**
     * Log profile items. Each item is one desired sensor or subsensor.
     */
    private List<LogProfileItem> logItems;
    /**
     * Indicates whether user wants to save GPS position
     */
    private boolean saveGPS;
    /**
     * If user wants to save GPS position what frequency should be used.
     */
    private int GPSFrequency;

    /**
     * Constructor
     * @param profileName Name of a profile
     * @param logItems Items of a profile
     */
    public LogProfile(String profileName, List<LogProfileItem> logItems){
        this.setProfileName(profileName);
        this.setLogItems(logItems);
    }

    /**
     * Creates LogProfile object with zero length name and empty array.
     */
    public LogProfile(){
        this("",new ArrayList<LogProfileItem>());
    }

    /**
     * Get name of this profile
     * @return profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Sets profile name
     * @param profileName name to be set
     */
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    /**
     * Get items from this profile
     * @return items of this profile
     */
    public List<LogProfileItem> getLogItems() {
        return logItems;
    }

    /**
     * Set items to this profile
     * @param logItems items of this profile
     */
    public void setLogItems(List<LogProfileItem> logItems) {
        this.logItems = logItems;
    }

    /**
     * Indicates whether user wants to save position while logging
     * @return true if user wants to save position otherwise false
     */
    public boolean isSaveGPS() {
        return saveGPS;
    }

    public void setSaveGPS(boolean saveGPS) {
        this.saveGPS = saveGPS;
    }

    /**
     *
     * @return Frequency to be used while getting position
     */
    public int getGPSFrequency() {
        return GPSFrequency;
    }

    public void setGPSFrequency(int GPSFrequency) {
        this.GPSFrequency = GPSFrequency;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
