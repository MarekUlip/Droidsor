package com.marekulip.droidsor.viewmodels;

import androidx.lifecycle.ViewModel;

import com.marekulip.droidsor.sensorlogmanager.LogProfileItem;

import java.util.ArrayList;
import java.util.List;

public class LogProfileViewModel extends ViewModel {
    private List<LogProfileItem> items = new ArrayList<>();

    public List<LogProfileItem> getItems() {
        return items;
    }

    public void setItems(List<LogProfileItem> items){
        this.items = items;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        items.clear();
        items = null;
    }
}
