package com.barikoi.barikoitrace.localstorage.sqlitedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;

import com.barikoi.barikoitrace.Utils.DateTimeUtils;
import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceErrors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public final class LogDbHelper extends SQLiteOpenHelper {


    private static LogDbHelper INSTANCE;


    private SQLiteDatabase sqLiteDatabase;


    private LogBuilder logBuilder;


    private Context context;


    private ConfigStorageManager configStorageManager;



    public static class LogBuilder {


        private final StringBuilder stringBuilder;

        private LogBuilder() {
            this.stringBuilder = new StringBuilder();
        }



        public String endLog() {
            this.stringBuilder.append("\n");
            this.stringBuilder.append("\n");
            this.stringBuilder.append("----END----");
            return this.stringBuilder.toString();
        }



        public void buildDeviceInfoLog(String str) {
            this.stringBuilder.append("Model : ").append(Build.MODEL);
            this.stringBuilder.append("\n");
            this.stringBuilder.append("Manufacture : ").append(Build.MANUFACTURER);
            this.stringBuilder.append("\n");
            this.stringBuilder.append("Brand : ").append(Build.BRAND);
            this.stringBuilder.append("\n");
            this.stringBuilder.append("SDK Version : ").append(Build.VERSION.SDK_INT);
            this.stringBuilder.append("\n");
            this.stringBuilder.append("User_Id : ").append(str);
            this.stringBuilder.append("\n");
        }



        public void buildDailyLog(String str, String str2) {
            this.stringBuilder.append("\n");
            this.stringBuilder.append(str).append("---").append(str2);
            this.stringBuilder.append("\n");
        }
    }

    private LogDbHelper(Context context) {
        super(context, "log", (SQLiteDatabase.CursorFactory) null, 1);
        //BarikoiTraceLogView.debugLog("log db created");
        this.context = context.getApplicationContext();
        this.configStorageManager = ConfigStorageManager.getInstance(context);
    }


    public static LogDbHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LogDbHelper(context);
        }
        return INSTANCE;
    }


    private void createDailyLogFile(String str, String str2) throws IOException {
        if (SystemSettingsManager.checkKitktatSorageWritePermission(this.context)) {
            File file = new File(Environment.getExternalStorageDirectory(), "BarikoiTrace/files/");
            if (!file.exists()) {
                file.mkdirs();
            }
            File file2 = new File(file, str2);
            file2.createNewFile();
            ByteBuffer wrap = ByteBuffer.wrap(str.getBytes());
            FileChannel channel = new FileOutputStream(file2).getChannel();
            try {
                channel.write(wrap);
                BarikoiTraceLogView.onSuccess("Exported Successfully");
                m306b();
            } finally {
                if (channel != null) {
                    channel.close();
                }
            }
        }
    }


    private void m306b() {
        openDb();
        this.sqLiteDatabase.delete("log", null, null);
        this.sqLiteDatabase.delete("latencyLog", null, null);
        closeDb();
    }


    private void closeDb() {
        SQLiteDatabase sQLiteDatabase = this.sqLiteDatabase;
        if (sQLiteDatabase != null) {
            sQLiteDatabase.close();
        }
    }


    private JSONArray getLatencyLog() throws JSONException {
        openDb();
        JSONArray jSONArray = new JSONArray();
        Cursor query = this.sqLiteDatabase.query("latencyLog", null, null, null, null, null, null, null);
        if (query.getCount() == 0 || !query.moveToFirst()) {
            query.close();
            closeDb();
            return jSONArray;
        }
        do {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("type", query.getString(query.getColumnIndex("type")));
            jSONObject.put("start_time", query.getString(query.getColumnIndex("startDate")));
            jSONObject.put("end_time", query.getString(query.getColumnIndex("endDate")));
            jSONObject.put("milliseconds", query.getString(query.getColumnIndex("time")));
            jSONArray.put(jSONObject);
        } while (query.moveToNext());
        query.close();
        closeDb();
        return jSONArray;
    }


    private void m309e() {
        openDb();
        Cursor query = this.sqLiteDatabase.query("log", null, null, null, null, null, null, null);
        if (query.getCount() == 0 || !query.moveToFirst()) {
            query.close();
            closeDb();
        }
        do {
            this.logBuilder.buildDailyLog(query.getString(query.getColumnIndex("date")), query.getString(query.getColumnIndex("description")));
        } while (query.moveToNext());
        query.close();
        closeDb();
    }


    private void openDb() {
        this.sqLiteDatabase = getWritableDatabase();
    }




    public void m312a(String str) {
        if (this.configStorageManager.isLogging()) {
            openDb();
            ContentValues contentValues = new ContentValues();
            contentValues.put("date", DateTimeUtils.getCurrentTimeLocal());
            contentValues.put("description", ": " + str);
            this.sqLiteDatabase.insert("log", null, contentValues);
            BarikoiTraceLogView.debugLog(str);
            closeDb();
        }
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE log ( id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, description TEXT NOT NULL  ) ");
        sQLiteDatabase.execSQL("CREATE TABLE latencyLog ( id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, startDate TEXT NOT NULL, endDate TEXT NOT NULL, time TEXT NOT NULL  ) ");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS log");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS latencyLog");
        onCreate(sQLiteDatabase);
    }
}
