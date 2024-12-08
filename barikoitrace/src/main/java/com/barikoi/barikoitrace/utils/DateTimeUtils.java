package com.barikoi.barikoitrace.utils;


import com.barikoi.barikoitrace.exceptions.BarikoiTraceException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class DateTimeUtils {

    public static long timeDifferenceInSeconds(String str, String str2) throws BarikoiTraceException {
        try {
            return (getDateTime12Hour(str).getTime() - getDateTime12Hour(str2).getTime()) / 1000;
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }


    public static String getCurrentTimeLocal() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Calendar.getInstance().getTime());
    }


    private static Date getDateTime12Hour(String str) throws BarikoiTraceException {
        try {
        Date parse = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.getDefault()).parse(str);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh.mm.ss.SSS aa", Locale.US);

            return simpleDateFormat.parse(simpleDateFormat.format(parse));
        } catch (ParseException e) {
            throw new BarikoiTraceException(e);
        }
    }


    public static long getTimeDIffInSeconds(String str, String str2) throws BarikoiTraceException {
        try {
            return (getDateTimeLocalfromUTC(str).getTime() - getDateTimeLocalfromUTC(str2).getTime()) / 1000;
        } catch (Exception e) {
            throw new BarikoiTraceException(e);
        }
    }


    public static String getCurrentDateTimeStringPST() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Calendar.getInstance().getTime());
//        return new SimpleDateFormat("Z").format(Calendar.getInstance().getTime());
    }


    public static Date getDateTimeLocalfromUTC(String str) throws BarikoiTraceException {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parse = null;
            parse= simpleDateFormat.parse(str);

            SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss.SSS aa");
            simpleDateFormat2.setTimeZone(TimeZone.getTimeZone(Calendar.getInstance().getTimeZone().getID()));
            return simpleDateFormat2.parse(simpleDateFormat2.format(parse));
        }catch (ParseException e) {
            throw new BarikoiTraceException(e);
        }
    }


    public static long getDateTimeUTC(String str) throws BarikoiTraceException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return simpleDateFormat.parse(str).getTime();
        } catch (ParseException e) {
            throw new BarikoiTraceException(e);
        }
    }
    public static String getDateTimeLocal(long time){
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(time);
    }

    public static String getCurrentDateTimeUTC() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = simpleDateFormat.parse(simpleDateFormat.format(Calendar.getInstance().getTime()));
        } catch (ParseException e) {
        }
        return simpleDateFormat.format(date.getTime());
    }
}
