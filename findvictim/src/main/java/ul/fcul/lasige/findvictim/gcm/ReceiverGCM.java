package ul.fcul.lasige.findvictim.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.example.unzi.findalert.data.Alert;
import com.example.unzi.findalert.interfaces.OnAlert;
import com.example.unzi.findalert.interfaces.OnRegisterComplete;
import com.example.unzi.findalert.ui.RegisterInFind;

import ul.fcul.lasige.findvictim.app.Constants;
import ul.fcul.lasige.findvictim.app.VictimApp;
import ul.fcul.lasige.findvictim.ui.MainActivity;

/**
 * Created by unzi on 20/05/2016.
 */
public class ReceiverGCM implements OnAlert, OnRegisterComplete {
    private Context mContext;

    public ReceiverGCM(Context context){
        RegisterInFind registerInFind = RegisterInFind.sharedInstance(context);
        registerInFind.observeOnAlert(this);
        registerInFind.observeOnRegisterComplete(this);
        mContext = context;
    }

    @Override
    public void onAlertReceived(Alert mAlert, boolean isInside) {
        Constants.ONGOING_ALERT = 1;
    }

    @Override
    public void onAlertStart(int mAlert) {
        VictimApp app = (VictimApp) mContext.getApplicationContext();
        app.starSensors();
        Constants.ONGOING_ALERT = 2;
    }

    @Override
    public void onAlertStop(int mAlert) {
        VictimApp app = (VictimApp) mContext.getApplicationContext();
        app.stopSensors();
        Constants.ONGOING_ALERT = 0;
    }

    @Override
    public void OnRegisterComplete() {

    }
}