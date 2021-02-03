package com.barikoi.barikoitrace.models.events;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.List;

@Keep
public class Location {
    private List<Double> coordinates = new ArrayList();
    private String type;

    public List<Double> getCoordinates() {
        return this.coordinates;
    }

    public String getType() {
        return this.type;
    }

    public void setCoordinates(List<Double> list) {
        this.coordinates = list;
    }

    public void setType(String str) {
        this.type = str;
    }
}
