package com.example.unzi.findalert.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by afons on 05/04/2016.
 * Insert a message in the database
 */
public class Route {


    private double start_lat;
    private double end_lat;
    private double start_lng;
    private double end_lng;
    private long timeReceived;

    public Route (double start_lat, double end_lat, double start_lng, double end_lng, long timeReceived){
        this.start_lat= start_lat;
        this.start_lng=start_lng;
        this.end_lat=end_lat;
        this.end_lng=end_lng;
        this.timeReceived=timeReceived;
    }

    public long getTimeReceived() {
        return timeReceived;
    }

    public double getStart_lat() {
        return start_lat;
    }

    public double getEnd_lat() {
        return end_lat;
    }

    public double getStart_lng() {
        return start_lng;
    }

    public double getEnd_lng() {
        return end_lng;
    }

    public static Route fromCursor(Cursor data) {
        return new Route(data.getDouble(data.getColumnIndex(Store.COLUMN_START_LAT)),
                data.getDouble(data.getColumnIndex(Store.COLUMN_END_LAT)),
                data.getDouble(data.getColumnIndex(Store.COLUMN_START_LNG)),
                data.getDouble(data.getColumnIndex(Store.COLUMN_END_LNG)),
                data.getLong(data.getColumnIndex(Store.COLUMN_ROUTE_TIME))
               );
    }
    /*
     * Messages table definition - database
     */
    public static abstract class Store implements BaseColumns {
        public static final String TABLE_NAME = "routes";

        public static final String COLUMN_START_LAT = "start_lat";
        public static final String COLUMN_END_LAT = "end_lat";
        public static final String COLUMN_START_LNG = "start_lng";
        public static final String COLUMN_END_LNG = "end_lng";
        public static final String COLUMN_ROUTE_TIME = "route_time";

        /* SQL statements */
        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " ("
                        + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COLUMN_START_LAT + " REAL NOT NULL, "
                        + COLUMN_END_LAT + " REAL NOT NULL, "
                        + COLUMN_START_LNG + " REAL NOT NULL, "
                        + COLUMN_END_LNG + " REAL NOT NULL, "
                        + COLUMN_ROUTE_TIME + " INTEGER  )";

        /* query methods */
        public static Cursor fetchAllRoutes(SQLiteDatabase db) {
            String[] columns = new String[] {
                    _ID, COLUMN_START_LAT, COLUMN_END_LAT, COLUMN_START_LNG, COLUMN_END_LNG, COLUMN_ROUTE_TIME
            };

            return db.query(TABLE_NAME, columns, null, null, null, null, COLUMN_ROUTE_TIME + " DESC");
        }


        public static long addRoute(SQLiteDatabase db, Route route) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_START_LAT, route.start_lat);
            values.put(COLUMN_END_LAT, route.end_lat);
            values.put(COLUMN_START_LNG, route.start_lng);
            values.put(COLUMN_END_LNG, route.end_lng);

            long messageId = -1;
            try {
                messageId = db.insertOrThrow(TABLE_NAME, null, values);
                Log.d("teste","Added: "+ messageId);
            } catch (SQLiteConstraintException e) {
                // duplicate
            }
            return messageId;
        }
    }
}
