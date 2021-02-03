package com.barikoi.barikoitrace.models.events;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.List;

@Keep
public class BarikoiTraceEvents {
    private List<BarikoiTraceEvent> events = new ArrayList();

    public List<BarikoiTraceEvent> getEvents() {
        return this.events;
    }

    public void setEvents(List<BarikoiTraceEvent> list) {
        this.events = list;
    }
}
