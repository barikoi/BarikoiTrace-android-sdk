package com.barikoi.barikoitrace.localstorage;

import android.content.Context;
import android.content.SharedPreferences;


import java.util.Set;



public final class SharedPRefHelper {


    private Context context;

    SharedPRefHelper(Context context) {
        this.context = context;
    }


    private SharedPreferences getSharedPref() {
        return this.context.getSharedPreferences("BARIKOITRACE_SP", Context.MODE_PRIVATE);
    }


    void putInt(String str, int i) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putInt(str.toUpperCase(), i);
        edit.apply();
        edit.commit();
    }



    void putLong(String str, long j) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putLong(str.toUpperCase(), j);
        edit.apply();
        edit.commit();
    }



    public void putString(String str, String str2) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putString(str.toUpperCase(), str2);
        edit.apply();
        edit.commit();
    }



    public void putStringSet(String str, Set<String> set) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putStringSet(str.toUpperCase(), set);
        edit.apply();
        edit.commit();
    }



    public void putBoolean(String str, boolean z) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.putBoolean(str.toUpperCase(), z);
        edit.apply();
        edit.commit();
    }



    public boolean getBoolean(String str) {
        return getSharedPref().getBoolean(str.toUpperCase(), false);
    }



    public int getInt(String str) {
        return getSharedPref().getInt(str.toUpperCase(), 0);
    }



    public long getLong(String str) {
        return getSharedPref().getLong(str.toUpperCase(), 0);
    }



    public String getString(String str) {
        return getSharedPref().getString(str.toUpperCase(), null);
    }






    public void remove(String str) {
        SharedPreferences.Editor edit = getSharedPref().edit();
        edit.remove(str.toUpperCase());
        edit.apply();
        edit.commit();
    }
}
