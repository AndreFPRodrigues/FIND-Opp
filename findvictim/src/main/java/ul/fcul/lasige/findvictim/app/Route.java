package ul.fcul.lasige.findvictim.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Created by afons on 05/04/2016.
 * Insert a message in the database
 */
public class Route {

    public double start_lat;
    public double end_lat;
    public double start_lng;
    public double end_lng;
    public long timeSent;
    public long timeReceived;
    //private String mHash;

    // hash is used as an unique identifier of the message (sender, content, time sent)
   /* public String getHash() {
        if (mHash == null) {
            String raw = String.format(Locale.US, "%d|%d|%d|%d|%d", start_lat, end_lat, start_lng, end_lng, timeSent);
            try {
                mHash = createHash(raw);
            } catch (NoSuchAlgorithmException e) {
                mHash = null;
            }
        }
        return mHash;
    }*/

    public static String createHash(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(input.getBytes());
        byte[] digest = md.digest();

        StringBuilder sb = new StringBuilder(2 * digest.length);
        for (byte b : digest) {
            sb.append("0123456789ABCDEF".charAt((b & 0xF0) >> 4));
            sb.append("0123456789ABCDEF".charAt((b & 0x0F)));
        }
        return sb.toString();
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
        public static final String COLUMN_TIME_SENT = "time_sent";
        public static final String COLUMN_TIME_RECEIVED = "time_received";
        //public static final String COLUMN_HASH = "hash";

        /* SQL statements */
        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " ("
                        + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COLUMN_START_LAT + " REAL NOT NULL, "
                        + COLUMN_END_LAT + " REAL NOT NULL, "
                        + COLUMN_START_LNG + " REAL NOT NULL, "
                        + COLUMN_END_LNG + " REAL NOT NULL, "
                        + COLUMN_ROUTE_TIME + "INTEGER NOT NULL, "
                        + COLUMN_TIME_SENT + " INTEGER NOT NULL, "
                        + COLUMN_TIME_RECEIVED + " INTEGER NOT NULL, ";
                        //+ COLUMN_HASH + " TEXT UNIQUE NOT NULL)";

        /* query methods */
        public static Cursor fetchAllRoutes(SQLiteDatabase db) {
            String[] columns = new String[] {
                    _ID, COLUMN_START_LAT, COLUMN_END_LAT, COLUMN_START_LNG, COLUMN_END_LNG, COLUMN_ROUTE_TIME, COLUMN_TIME_SENT, COLUMN_TIME_RECEIVED
            };

            return db.query(TABLE_NAME, columns, null, null, null, null, COLUMN_ROUTE_TIME + " DESC");
        }

        public static Cursor fetchRoutesFromTimestamp (SQLiteDatabase db, long timestamp) {
            return db.rawQuery("SELECT * FROM routes WHERE cast(route_time as INTEGER) >= cast(? as INTEGER) ORDER BY route_time DESC", new String[]{String.valueOf(timestamp)});
        }

        public static long addRoute(SQLiteDatabase db, Route route) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_START_LAT, route.start_lat);
            values.put(COLUMN_END_LAT, route.end_lat);
            values.put(COLUMN_START_LNG, route.start_lng);
            values.put(COLUMN_END_LNG, route.end_lng);
            values.put(COLUMN_TIME_RECEIVED, route.timeReceived);
            //values.put(COLUMN_HASH, route.getHash());

            long messageId = -1;
            try {
                messageId = db.insertOrThrow(TABLE_NAME, null, values);
            } catch (SQLiteConstraintException e) {
                // duplicate
            }
            return messageId;
        }
    }
}
