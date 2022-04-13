package com.barikoi.barikoitrace.localstorage.sqlitedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.barikoi.barikoitrace.Utils.DateTimeUtils;
import com.barikoi.barikoitrace.Utils.SystemSettingsManager;
import com.barikoi.barikoitrace.callback.BarikoiTraceBulkUpdateCallback;
import com.barikoi.barikoitrace.exceptions.BarikoiTraceLogView;
import com.barikoi.barikoitrace.localstorage.ConfigStorageManager;
import com.barikoi.barikoitrace.models.BarikoiTraceError;
import com.barikoi.barikoitrace.network.ApiRequestManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public final class LogDbHelper extends SQLiteOpenHelper {


    private static LogDbHelper INSTANCE;


    private SQLiteDatabase sqLiteDatabase;


    private LogBuilder logBuilder;


    private Context context;

    private String userid="";
    private ConfigStorageManager configStorageManager;
    private File file2;

    public void writeDeviceLog(){
        writeLog("location permission: " +SystemSettingsManager.checkPermissions(context)+
        "\nlocation settings: "+SystemSettingsManager.checkLocationSettings(context)+
                "\nBattery optimization ignored: "+ SystemSettingsManager.isIgnoringBatteryOptimization(context)+
                "\nBattery saver on: "+ SystemSettingsManager.isInPowerSaveMode(context));
    }


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
        super(context, "log", (SQLiteDatabase.CursorFactory) null, 3);
        //BarikoiTraceLogView.debugLog("log db created");
        this.context = context.getApplicationContext();
        this.configStorageManager = ConfigStorageManager.getInstance(context);
        userid=configStorageManager.getUserID();
    }


    public static LogDbHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LogDbHelper(context);
        }
        return INSTANCE;
    }

    public void setUserid(String userid){
        this.userid=userid;
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




    private void m309e() {
        openDb();
        Cursor query = this.sqLiteDatabase.query("log", null, null, null, null, null, null, null);
        if (query.getCount() == 0 || !query.moveToFirst()) {
            query.close();
            closeDb();
        }
//        do {
//            this.logBuilder.buildDailyLog(query.getString(query.getColumnIndex("date")), query.getString(query.getColumnIndex("description")));
//        } while (query.moveToNext());
        query.close();
        closeDb();
    }


    private void openDb() {
        this.sqLiteDatabase = getWritableDatabase();
    }




    public void writeLog(String str) {
//        if (this.configStorageManager.isLogging()) {
            openDb();
            ContentValues contentValues = new ContentValues();
            contentValues.put("date", DateTimeUtils.getCurrentTimeLocal());
            contentValues.put("userId", userid);
            contentValues.put("description", ": " + str);
            this.sqLiteDatabase.insert("log", null, contentValues);
            BarikoiTraceLogView.debugLog(str);
            closeDb();
//        }
    }



    //delete
    public void deleteTable() {
        openDb();
        this.sqLiteDatabase.delete("log", null, null);
        //this.sqLiteDatabase.delete("latencyLog", null, null);
        //this.sqLiteDatabase.delete("devicelog", null, null);
        closeDb();
    }



    public void deleteTableById(String tableName) {
        openDb();
        this.sqLiteDatabase.delete(tableName, null, null);
        closeDb();
    }

    public void generateDBFile(){
        JSONArray dbArray1= getResults("log");
        //JSONArray dbArray2= getResults("devicelog");
        //JSONArray dbArray3= getResults("locationlog");
        if (dbArray1.length()>0) {
            /*if (createDailyLogFile()){
            }*/
            Log.d("DB", "DBArray: " + dbArray1);
            try {
                StringBuilder description = new StringBuilder();
                description.append("\n").append("-----Device Log Table-----").append("\n");
                description.append("Date").append("\t\t\t").append("Description").append("\n");
                description.append("-----------------------------------------").append("\n");
                description.append("Model : ").append(Build.MODEL);
                description.append("\n");
                description.append("Manufacture : ").append(Build.MANUFACTURER);
                description.append("\n");
                description.append("Brand : ").append(Build.BRAND);
                description.append("\n");
                description.append("SDK Version : ").append(Build.VERSION.SDK_INT);
                description.append("\n");
                description.append("User_Id : ").append(userid);
                description.append("\n");
                description.append("\n").append("\n").append("-----Log Table-----").append("\n");
                description.append("Date").append("\t\t\t").append("Description").append("\n");
                description.append("-----------------------------------------").append("\n");
                for (int j = 0; j < dbArray1.length(); j++) {

                    JSONObject obj = dbArray1.getJSONObject(j);
                    description.append(obj.getString("date"));
                    if (obj.has("description")) {
                        description.append("  ").append(obj.getString("description")).append("\n");
                    }
                }


                description.append("\n").append("\n").append("-----END-----").append("\n");


                exportDailyLogFile(String.valueOf(description));
            } catch (JSONException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            }


        }
    }

    public boolean createDailyLogFile() throws IOException {
        /*if (SystemSettingsManager.checkKitktatSorageWritePermission(this.context)) {
        }*/
        //File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+"/BarikoiTrace/Logfiles");
        //File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),"BarikoiTrace/Logfiles/");


        //str2.replace(" ","_");

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());

            /*File file = new File(Environment.getExternalStorageDirectory(),"BarikoiTrace/Logfiles/");
            if (!file.exists()) {
                file.mkdirs();
            }
            file2 = new File(file, "Log_"+timeStamp+".txt");
            file2.createNewFile();*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            file2 = new File(context.getFilesDir(),"Log_"+timeStamp+".txt");
        }
        Log.d("DB", "File created: "+file2.getName()+ " filepath: "+file2.getAbsolutePath());
        //configStorageManager.setLogFilePath(file2.getAbsolutePath());
        return true;
    }
    public void exportDailyLogFile(String str) throws IOException{


        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
            File root = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                root = new File(context.getFilesDir(), "Logs");
            }
            if (root != null && !root.exists()) {
                root.mkdirs();
            }
            final File gpxfile = new File(context.getFilesDir(), "Log_"+timeStamp+".txt");

            FileWriter writer = new FileWriter(gpxfile);
            writer.append(str);
            writer.flush();
            writer.close();
            //Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
            Log.d("DB", "File exported"+gpxfile.getAbsolutePath());
            for(final File f : context.getFilesDir().listFiles()) {
                if(f.isFile() && f.getName().startsWith("Log_"))
                ApiRequestManager.getInstance(context).insertLogFile(f.getAbsolutePath(), new BarikoiTraceBulkUpdateCallback() {
                    @Override
                    public void onBulkUpdate() {
                        deleteTable();
                        f.delete();
                    }

                    @Override
                    public void onFailure(BarikoiTraceError barikoiError) {

                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getResults(String tableName)
    {
        String myPath = context.getDatabasePath(getDatabaseName()).toString();
        String logTable = tableName;
        SQLiteDatabase myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

        String searchQuery = "SELECT  * FROM " + logTable;
        Cursor cursor = myDataBase.rawQuery(searchQuery, null );

        JSONArray resultSet     = new JSONArray();

        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {

            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();

            for( int i=0 ;  i< totalColumn ; i++ )
            {
                if( cursor.getColumnName(i) != null )
                {
                    try
                    {
                        if( cursor.getString(i) != null )
                        {
                            //Log.d("TAG_NAME", cursor.getString(i) );
                            rowObject.put(cursor.getColumnName(i) ,  cursor.getString(i) );
                        }
                        else
                        {
                            rowObject.put( cursor.getColumnName(i) ,  "" );
                        }
                    }
                    catch( Exception e )
                    {
                        //Log.d("TAG_NAME", e.getMessage()  );
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }
        cursor.close();
        //Log.d("TAG_NAME", resultSet.toString() );
        return resultSet;
    }


    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE log ( id INTEGER PRIMARY KEY AUTOINCREMENT, userId TEXT NOT NULL, date TEXT NOT NULL, description TEXT NOT NULL  ) ");
        sQLiteDatabase.execSQL("CREATE TABLE latencyLog ( id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, startDate TEXT NOT NULL, endDate TEXT NOT NULL, time TEXT NOT NULL  ) ");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS log");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS latencyLog");
        onCreate(sQLiteDatabase);
    }
}

