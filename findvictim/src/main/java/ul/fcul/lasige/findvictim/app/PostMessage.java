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
public class PostMessage {

    public String sender;
    public String sender_type;
    public String content;
    public long timeSent;
    public long timeReceived;
    private String mHash;

    // hash is used as an unique identifier of the message (sender, content, time sent)
    public String getHash() {
        if (mHash == null) {
            String raw = String.format(Locale.US, "%s|%s|%d", sender, content, timeSent);
            try {
                mHash = createHash(raw);
            } catch (NoSuchAlgorithmException e) {
                mHash = null;
            }
        }
        return mHash;
    }

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
        public static final String TABLE_NAME = "messages";

        public static final String COLUMN_SENDER = "sender";
        public static final String COLUMN_SENDER_TYPE = "sender_type";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_SENT = "sent";
        public static final String COLUMN_TIME_SENT = "time_sent";
        public static final String COLUMN_TIME_RECEIVED = "time_received";
        public static final String COLUMN_HASH = "hash";

        /* SQL statements */
        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " ("
                        + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COLUMN_SENDER + " TEXT NOT NULL, "
                        + COLUMN_SENDER_TYPE + " TEXT NOT NULL, "
                        + COLUMN_CONTENT + " TEXT NOT NULL, "
                        + COLUMN_SENT + " INTEGER, "
                        + COLUMN_TIME_SENT + " INTEGER NOT NULL, "
                        + COLUMN_TIME_RECEIVED + " INTEGER NOT NULL, "
                        + COLUMN_HASH + " TEXT UNIQUE NOT NULL)";

        /* query methods */
        public static Cursor fetchAllMessages(SQLiteDatabase db) {
            String[] columns = new String[] {
                    _ID, COLUMN_SENDER, COLUMN_SENDER_TYPE, COLUMN_CONTENT, COLUMN_SENT, COLUMN_TIME_SENT, COLUMN_TIME_RECEIVED
            };
            return db.query(TABLE_NAME, columns, null, null, null, null, COLUMN_TIME_RECEIVED + " DESC");
        }

        public static Cursor fetchVictimMessages (SQLiteDatabase db) {
            return db.rawQuery("SELECT * FROM messages WHERE sender_type= ? ORDER BY time_received DESC", new String[]{"Victim"});
        }

        public static Cursor fetchRescuerMessages (SQLiteDatabase db) {
            return db.rawQuery("SELECT * FROM messages WHERE sender_type= ? ORDER BY time_received DESC", new String[]{"Rescuer"});
        }

        public static Cursor fetchMineMessageToSend (SQLiteDatabase db) {
            //return db.
            return null;
        }

        public static long addMessage(SQLiteDatabase db, PostMessage msg) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SENDER, msg.sender);
            values.put(COLUMN_SENDER_TYPE, msg.sender_type);
            values.put(COLUMN_CONTENT, msg.content);
            values.put(COLUMN_TIME_SENT, msg.timeSent);
            values.put(COLUMN_TIME_RECEIVED, msg.timeReceived);
            values.put(COLUMN_HASH, msg.getHash());

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
