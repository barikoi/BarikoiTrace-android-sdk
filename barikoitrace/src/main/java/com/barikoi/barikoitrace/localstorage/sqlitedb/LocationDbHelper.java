package com.barikoi.barikoitrace.localstorage.sqlitedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public final class LocationDbHelper extends SQLiteOpenHelper {


    private static LocationDbHelper INSTANCE;


    private SQLiteDatabase db;

    private LocationDbHelper(Context context) {
        super(context, "location", (SQLiteDatabase.CursorFactory) null, 1);
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


    public void deleteLocation() {
        try {
            openDb();
            this.db.delete("location", null, null);
            closedb();
        } catch (Exception e) {
        }
    }


    public void deleteLocations(String str) {
        try {
            openDb();
            this.db.delete("location", "location_id = ? ", new String[]{str});
            closedb();
        } catch (Exception e) {
        }
    }


    public void insertLocation(String str, JSONObject jSONObject) {
        try {
            openDb();
            ContentValues contentValues = new ContentValues();
            contentValues.put("location_id", str);
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


    public List<JSONObject> getLocationJson() {
        ArrayList arrayList = new ArrayList();
        try {
            openDb();
            Cursor query = this.db.query("location", null, null, null, null, null, null, null);
            if (query == null || query.getCount() == 0 || !query.moveToFirst()) {
                query.close();
                closedb();
                return arrayList;
            }
            do {
                arrayList.add(new JSONObject(query.getString(query.getColumnIndex("json"))));
            } while (query.moveToNext());
            query.close();
            closedb();
            return arrayList;
        } catch (Exception e) {
            return arrayList;
        }
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE location ( id INTEGER PRIMARY KEY AUTOINCREMENT, location_id TEXT NOT NULL, json TEXT NOT NULL  ) ");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS location");
        onCreate(sQLiteDatabase);
    }
}
