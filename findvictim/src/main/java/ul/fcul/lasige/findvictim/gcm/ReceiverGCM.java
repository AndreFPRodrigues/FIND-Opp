package ul.fcul.lasige.findvictim.gcm;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unzi.findalert.data.Alert;
import com.example.unzi.findalert.data.DatabaseHelper;
import com.example.unzi.findalert.interfaces.OnAlert;
import com.example.unzi.findalert.interfaces.OnRegisterComplete;
import com.example.unzi.findalert.ui.AlertActivity;
import com.example.unzi.findalert.ui.RegisterInFind;
import com.example.unzi.findalert.webservice.RequestServer;

import ul.fcul.lasige.findvictim.R;
import ul.fcul.lasige.findvictim.app.Constants;
import ul.fcul.lasige.findvictim.app.VictimApp;
import ul.fcul.lasige.findvictim.ui.MainActivity;

/**
 * Created by unzi on 20/05/2016.
 */
public class ReceiverGCM implements OnAlert, OnRegisterComplete {
    private Context mContext;
    private static final String TAG = ReceiverGCM.class.getSimpleName();
    private boolean mIsInside;

    public ReceiverGCM(Context context){
        RegisterInFind registerInFind = RegisterInFind.sharedInstance(context);
        registerInFind.observeOnAlert(this);
        registerInFind.observeOnRegisterComplete(this);
        mContext=context;
    }

    @Override
    public void onAlertReceived(Alert alert, boolean isInside) {
        mIsInside=isInside;
        if(mIsInside){
            //TODO we should convert all start/min lat and long to south/north east/west
            RequestServer.downloadRoutes( mContext.getApplicationContext(), alert.getLatEnd(),alert.getLonEnd(),alert.getLatStart(),alert.getLonStart());
        }
    }

    @Override
    public void onAlertStart(int idAlert) {
        showNotificationServiceActive(idAlert);
        VictimApp app = (VictimApp) mContext.getApplicationContext();
        app.starSensors();
        Constants.ONGOING_ALERT = 2;
        Constants.MANUALLY_STARTED = false;
        Constants.MANUALLY_STOPPED = false;
    }

    @Override
    public void onAlertStop(int mAlert) {
        cancelNotification();
        //TODO added in sync if app is victim
        VictimApp app = (VictimApp) mContext.getApplicationContext();
        app.stopSensors();
        Constants.ONGOING_ALERT = 0;
        Constants.MANUALLY_STARTED = false;
        Constants.MANUALLY_STOPPED = false;
    }

    @Override
    public void OnRegisterComplete() {

    }

    private void showNotificationServiceActive(int idAlert){
      Cursor cursor= Alert.Store.fetchAlert(DatabaseHelper.getInstance(mContext).getReadableDatabase(),
                idAlert);
        if (cursor!=null) {
            Alert alert = Alert.fromCursor(cursor);
            cursor.close();

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
                    .setSmallIcon(com.example.unzi.findalert.R.drawable.warning_notification)
                    .setContentTitle("Find running...")
                    .setContentText("Inside " + alert.getName() +" alert." ).setOngoing(true);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilder.setColor(Color.RED);
            }
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(mContext, MainActivity.class);

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(2, mBuilder.build());
        }

    }

    private void cancelNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(2);
    }


}
