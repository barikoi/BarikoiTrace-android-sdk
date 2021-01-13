package com.barikoi.barikoitrace;

import android.location.Location;


/**
 * Created by Sakib on 3/8/2018.
 */

public interface LocationTaskListener {
    void onLocationAvailability( boolean available);

    void oneEndTask();

    void OnLocationChanged(Location location);
}
