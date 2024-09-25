package com.barikoi.barikoitrace.localstorage.sqlitedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;


import com.barikoi.barikoitrace.models.createtrip.Trip;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public final class LocationDbHelper extends SQLiteOpenHelper {


    private static LocationDbHelper INSTANCE;


    private SQLiteDatabase db;

    private LocationDbHelper(Context context) {
        super(context, "location", null, 4);
    }


    public static synchronized LocationDbHelper getInstance(Context context) {
        LocationDbHelper aVar;
        synchronized (LocationDbHelper.class) {
            if (INSTANCE == null) {
                INSTANCE = new LocationDbHelper(context);
            }
            aVar = INSTANCE;
        }
        return aVar;
    }


    private void openDb() {
        this.db = getWritableDatabase();
    }




    public void insertLocation( JSONObject jSONObject) {
        try {
            openDb();
            ContentValues contentValues = new ContentValues();
            contentValues.put("json", jSONObject.toString());
            this.db.insert("location", null, contentValues);
            closedb();
        } catch (Exception e) {
            Log.e("locationdbhelper", e.getMessage()+", "+e.getCause());
        }
    }


    public void closedb() {
        SQLiteDatabase sQLiteDatabase = this.db;
        if (sQLiteDatabase != null) {
            sQLiteDatabase.close();
        }
    }

    public long getofflinecount(){
        openDb();
        return DatabaseUtils.queryNumEntries(this.db,"location");
    }

    public JSONArray getLocationJson(String userid) {
        JSONArray arrayList = new JSONArray();
        try {
            openDb();
            Cursor query = this.db.query("location", null, null, null, null, null, "id ASC", "100");
            if (query == null || query.getCount() == 0 || !query.moveToFirst()) {
                if (query != null) {
                    query.close();
                }
                closedb();
                return arrayList;
            }
            do {
                arrayList.put(new JSONObject(query.getString(query.getColumnIndexOrThrow("json"))).put("user_id",userid));
            } while (query.moveToNext());
            query.close();
            closedb();
            return arrayList;
        } catch (Exception e) {
            return arrayList;
        }
    }

    public int clearlast100(){
        //String[] ids =getlast100locId();
        openDb();
        db.beginTransaction();
        int affectedRows=0;
        try {
            SQLiteStatement statement = this.db.compileStatement("delete from location where id in (select id from location order by id ASC limit 100)");
            affectedRows = statement.executeUpdateDelete();
            //this.db.rawQuery("delete from location where id in (select id from location order by id ASC limit 100)",null);
            this.db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("locationdbhelper", e.getMessage()+", " + e.getCause());
        } finally {
            db.endTransaction();
            closedb();
        }
        return affectedRows;

    }

    /*public ArrayList<Trip> getActiveTrips(){
        ArrayList<Trip> trips =new ArrayList<>();
        openDb();
        Cursor query = this.db.rawQuery("select * from trip where state =0",null);
        if (query == null || query.getCount() == 0 || !query.moveToFirst()) {
            if (query != null) {
                query.close();
            }
            closedb();
        }else{
            if (query.moveToFirst()){
                while(!query.isAfterLast()){
                    Trip trip = new Trip(
                            query.getString(query.getColumnIndexOrThrow("id")),
                            query.getString(query.getColumnIndexOrThrow("start_time")),
                            query.getString(query.getColumnIndexOrThrow("end_time")),
                            query.getString(query.getColumnIndexOrThrow("tag")),
                            query.getInt(query.getColumnIndexOrThrow("state")),
                            query.getString(query.getColumnIndexOrThrow("user_id")),
                            query.getInt(query.getColumnIndexOrThrow("synced"))
                    );
                    trips.add(trip);
                    query.moveToNext();
                }
            }
            query.close();
        }
        return trips;
    }*/
    /*public void addTrip(int user_id, String startTime, String tag, int synced){
        try {
            openDb();

            ContentValues contentValues = new ContentValues();
            contentValues.put("user_id", user_id);
            contentValues.put("start_time", startTime);
            contentValues.put("synced",synced);
            if(tag!=null)
                contentValues.put("tag", tag);

            contentValues.put("state", 0);

            this.db.insert("trip", null, contentValues);
            closedb();
        } catch (Exception e) {
            Log.e("locationdbhelper",e.getMessage());
        }
    }*/

   /* public void endTrip(int user_id, String endTime,  int synced){
        ArrayList<Trip> activeTrips= getActiveTrips();
        openDb();
        ContentValues contentValues = new ContentValues();
        contentValues.put("user_id", user_id);
        contentValues.put("end_time", endTime);
        contentValues.put("synced",synced);
        contentValues.put("state",1);
        *//*Cursor cursor=this.db.rawQuery("select * from trip where user_id="+user_id+" and state=0",null);
        *//**//*if(cursor.getCount()==1){
            int id=cursor.getInt(cursor.getColumnIndex("id"));
            this.db.update("trip",contentValues,"id=?",new String[]{id+""});
        }*//**//*
        if (cursor.moveToFirst()){
            while(!cursor.isAfterLast()){
                int id=cursor.getInt(cursor.getColumnIndex("id"));
                this.db.update("trip",contentValues,"id=?",new String[]{id+""});
            }
        }*//*
        for (Trip trip : activeTrips){
            this.db.update("trip",contentValues,"id=?",new String[]{trip.getTrip_id()});
        }
    }*/

    public void removeTrip(String id){
        openDb();
        this.db.delete("trip","id=?",new String[]{id});
    }

    public ArrayList<Trip> getofflineTrips(){
        ArrayList<Trip> trips =new ArrayList<>();
        openDb();
        Cursor query = this.db.rawQuery("select * from trip where (synced =0 or synced= 2)  and state = 1",null);
        if (query == null || query.getCount() == 0 || !query.moveToFirst()) {
            if (query != null) {
                query.close();
            }
            closedb();
        }else{
            if (query.moveToFirst()){
                while(!query.isAfterLast()){
                    Trip trip = new Trip( query.getString(query.getColumnIndexOrThrow("id"))
                            ,query.getString(query.getColumnIndexOrThrow("start_time"))
                            ,query.getString(query.getColumnIndexOrThrow("end_time"))
                            ,query.getString(query.getColumnIndexOrThrow("tag")),
                            query.getInt(query.getColumnIndexOrThrow("state")),
                            query.getString(query.getColumnIndexOrThrow("user_id")),
                            query.getInt(query.getColumnIndexOrThrow("synced")));
                    trips.add(trip);
                    query.moveToNext();
                }
            }
            query.close();
        }
        return trips;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE location ( id INTEGER PRIMARY KEY AUTOINCREMENT, json TEXT NOT NULL  ) ");
        sQLiteDatabase.execSQL("CREATE TABLE trip (trip_id TEXT, start_time TEXT NOT NULL,end_time TEXT ,tag TEXT ,user_id INTEGER NOT NULL,state INTEGER NOT NULL , synced INTEGER NOT NULL) ");

    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS location");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS trip");
        onCreate(sQLiteDatabase);
    }


}
