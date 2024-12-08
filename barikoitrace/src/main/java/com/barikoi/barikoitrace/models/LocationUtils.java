package com.barikoi.barikoitrace.models;

import android.location.Location;

import com.barikoi.barikoitrace.TraceMode;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;

import java.util.List;


public final class LocationUtils {


    public static int f200a = 500;



    public static  class C0090a {


        static final  int[] f201a;

        static {
            int[] iArr = new int[TraceMode.AppState.values().length];
            f201a = iArr;
            try {
                iArr[TraceMode.AppState.FOREGROUND.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                f201a[TraceMode.AppState.BACKGROUND.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }


    public enum LocationStatus {
        STOP,
        MOVING
    }


    public static double getSpeedInKmph(float f) {
        return ((double) f) * 3.6d;
    }


    public static int m401a(long j, double d) {
        double d2 = ((((double) (60 * j)) / d) / 1000.0d) * 60.0d;
        try {
            if (Double.isNaN(d2) || Double.isInfinite(d2) || d2 <= 0.0d) {
                return 30;
            }
            return (int) Math.round(d2);
        } catch (Exception e) {
            return 30;
        }
    }


    public static int getAccuracyRounded(ConfigStorageManager aVar) {
        if (aVar.getType() == TraceMode.TrackingModes.ACTIVE.getOption()) {
            return 50;
        }
        if (aVar.getType() == TraceMode.TrackingModes.REACTIVE.getOption()) {
            return 75;
        }
        if (aVar.getType() == TraceMode.TrackingModes.PASSIVE.getOption()) {
            return 100;
        }
        if(aVar.getAccuracyFilter()!=0) return aVar.getAccuracyFilter();
        return 100;
    }


    public static int getDistFilterFromSpeed(ConfigStorageManager aVar, long avgspd) {
        if (aVar.getType() == TraceMode.TrackingModes.ACTIVE.getOption()) {
            if (avgspd >= 0 && avgspd <= 10) {
                return 25;
            }
            if (avgspd >= 11 && avgspd <= 20) {
                return 50;
            }
            if (avgspd >= 21 && avgspd <= 30) {
                return 75;
            }
            if (avgspd >= 31 && avgspd <= 50) {
                return 100;
            }
            if (avgspd >= 51 && avgspd <= 70) {
                return 125;
            }
            if (avgspd < 71 || avgspd > 100) {
                return avgspd >= 101 ? 250 : 100;
            }
            return 175;
        } else if (aVar.getType() == TraceMode.TrackingModes.REACTIVE.getOption()) {
            if (avgspd >= 0 && avgspd <= 10) {
                return 50;
            }
            if (avgspd >= 11 && avgspd <= 20) {
                return 100;
            }
            if (avgspd >= 21 && avgspd <= 30) {
                return 150;
            }
            if (avgspd >= 31 && avgspd <= 50) {
                return 200;
            }
            if (avgspd >= 51 && avgspd <= 70) {
                return 250;
            }
            if (avgspd < 71 || avgspd > 100) {
                return avgspd >= 101 ? 500 : 100;
            }
            return 350;
        } else if (aVar.getType() == TraceMode.TrackingModes.PASSIVE.getOption()) {
            if (avgspd >= 0 && avgspd <= 10) {
                return 100;
            }
            if (avgspd >= 11 && avgspd <= 20) {
                return 200;
            }
            if (avgspd >= 21 && avgspd <= 30) {
                return 300;
            }
            if (avgspd >= 31 && avgspd <= 50) {
                return 400;
            }
            if (avgspd >= 51 && avgspd <= 70) {
                return 500;
            }
            if (avgspd < 71 || avgspd > 100) {
                return avgspd >= 101 ? 1000 : 100;
            }
            return 700;
        } else {
            return 100;
        }
    }


    public static int m404a(ConfigStorageManager aVar, List<Integer> list, Location location, int speed) {
        int a;
        try {
            Location s = aVar.getLastLocation();
            if (s.getLatitude() == 0.0d && s.getLongitude() == 0.0d) {
                return 100;
            }
            long time = (location.getTime() - s.getTime()) / 1000;
            if (time >= 0 && time < 60) {
                list.add(Integer.valueOf(speed));
                if (list.size() < 5 || (a = m405a(list)) <= 0) {
                    return 100;
                }
                list.remove(list.size() - 1);
                return getDistFilterFromSpeed(aVar, (long) a);
            } else if (time <= 60) {
                return 100;
            } else {
                list.clear();
                list.add(Integer.valueOf(speed));
                return 100;
            }
        } catch (Exception e) {
            return 100;
        }
    }


    public static int m405a(List<Integer> list) {
        int i = 0;
        for (int i2 = 0; i2 < list.size(); i2++) {
            i += list.get(i2).intValue();
        }
        return i / 5;
    }



    public static String m409a(LocationStatus bVar) {
        return (bVar == null || bVar != LocationStatus.MOVING) ? "S" : "M";
    }


    public static boolean m410a(TraceMode.AppState appState, ConfigStorageManager aVar) {
        int i = C0090a.f201a[appState.ordinal()];
        if (i == 1) {
            return aVar.getAppState().equals("F");
        }
        if (i != 2) {
            return true;
        }
        return aVar.getAppState().equals("B");
    }
}
