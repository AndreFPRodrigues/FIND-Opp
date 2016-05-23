package ul.fcul.lasige.findvictim.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.unzi.findalert.data.Alert;

import ul.fcul.lasige.findvictim.app.PostMessage;
import ul.fcul.lasige.findvictim.app.Route;

/**
 * Created by hugonicolau on 03/12/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FindVictim.db";

    private static DatabaseHelper sInstance = null;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) { sInstance = new DatabaseHelper(context); }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PostMessage.Store.SQL_CREATE_TABLE);
        db.execSQL(Alert.Store.SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // currently undefined behaviour
    }
}
