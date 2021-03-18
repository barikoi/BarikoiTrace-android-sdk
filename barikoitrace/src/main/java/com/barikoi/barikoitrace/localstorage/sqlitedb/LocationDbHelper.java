package com.barikoi.barikoitrace.localstorage.sqlitedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public final class LocationDbHelper extends SQLiteOpenHelper {


    private static LocationDbHelper INSTANCE;


    private SQLiteDatabase db;

    private LocationDbHelper(Context context) {
        super(context, "location", (SQLiteDatabase.CursorFactory) null, 2);
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




    public void deleteLocations(String str) {
        try {
            openDb();
            this.db.delete("location", "location_id = ? ", new String[]{str});
            closedb();
        } catch (Exception e) {
        }
    }


    public void insertLocation( JSONObject jSONObject) {
        try {
            openDb();
            ContentValues contentValues = new ContentValues();
            contentValues.put("json", jSONObject.toString());
            this.db.insert("location", null, contentValues);
            closedb();
        } catch (Exception e) {
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
        long count=DatabaseUtils.queryNumEntries(this.db,"location");

        return count;
    }
    public String[] getlast100locId(){
        ArrayList<String> ids=new ArrayList<>();
        try {
            openDb();
            Cursor query = this.db.query("location", new String[]{"id"}, null, null, null, null, "id ASC", "100");
            if (query == null || query.getCount() == 0 || !query.moveToFirst()) {
                query.close();
                closedb();
                return ids.toArray(new String[0]);
            }
            do {
                ids.add(query.getInt(query.getColumnIndex("id"))+"");
            } while (query.moveToNext());
            query.close();
            closedb();
            return ids.toArray(new String[0]);
        } catch (Exception e) {
            return ids.toArray(new String[0]);
        }
    }
    public JSONArray getLocationJson(int userid) {
        JSONArray arrayList = new JSONArray();
        try {
            openDb();
            Cursor query = this.db.query("location", null, null, null, null, null, "id ASC", "100");
            if (query == null || query.getCount() == 0 || !query.moveToFirst()) {
                query.close();
                closedb();
                return arrayList;
            }
            do {
                arrayList.put(new JSONObject(query.getString(query.getColumnIndex("json"))).put("user_id",userid));
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
        } finally {
            db.endTransaction();
            closedb();
            return affectedRows;
        }

    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE location ( id INTEGER PRIMARY KEY AUTOINCREMENT, json TEXT NOT NULL  ) ");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS location");
        onCreate(sQLiteDatabase);
    }

}
