package com.marekulip.droidsor.viewmodels;

import androidx.lifecycle.ViewModel;

import com.marekulip.droidsor.logs.LogDetailItem;

import java.util.ArrayList;
import java.util.List;

public class LogDetailViewModel extends ViewModel{
    private List<LogDetailItem> items = new ArrayList<>();

    public List<LogDetailItem> getItems() {
        return items;
    }

    public void setItems(List<LogDetailItem> items){
        this.items = items;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        items.clear();
        items = null;
    }
}
