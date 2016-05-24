package com.example.unzi.findalert.ui;

import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.unzi.findalert.R;
import com.example.unzi.findalert.data.TokenStore;
import com.example.unzi.findalert.gcm.RegistrationIntentService;
import com.example.unzi.findalert.utils.DeviceUtils;
import com.example.unzi.offlinemaps.DownloadFile;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // gcm
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    // server registration
    private static final int REQUEST_CODE_EMAIL = 1;
    private String mGoogleAccount;


    // ui
    private View.OnClickListener mOnTryRegisterAgainListener;
    private View.OnClickListener mOnTryDownloadAgainListener;

    private View.OnClickListener mFinish;
    private ProgressDialog mDialog;
    private CoordinatorLayout mCoordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);


        mOnTryRegisterAgainListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerGCM();
            }
        };

        mOnTryDownloadAgainListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTileDatabase(getApplicationContext());
            }
        };

        mFinish = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };

        // google cloud messaging (gcm)
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean sentToken = TokenStore.isRegistered(getApplicationContext());
                if (sentToken) {
                    mDialog.dismiss();
                    getTileDatabase(context);
                    Log.d(TAG, "Successful registration");
                    //TODO make sure we have permissions
                } else {
                    Log.d(TAG, "token not sent");
                    Snackbar snack = Snackbar.make(mCoordinatorLayout, "Registration failed. Check your Internet connection",
                            Snackbar.LENGTH_INDEFINITE);
                    snack.setAction("Try again", mOnTryRegisterAgainListener);
                    View view = snack.getView();
                    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                    params.gravity = Gravity.CENTER;
                    view.setLayoutParams(params);
                    snack.show();
                    mDialog.dismiss();

                }
            }
        };

        if (checkPlayServices()) {
            // play services are installed

            boolean sentToken = TokenStore.isRegistered(getApplicationContext());
            if (!sentToken) {
                // if not registered, then ask for google account
                try {
                    Intent intent = AccountPicker.newChooseAccountIntent(null,
                            null,
                            new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                            false, null, null, null, null);
                    // we need to wait for the result
                    startActivityForResult(intent, REQUEST_CODE_EMAIL);
                } catch (ActivityNotFoundException e) {
                    mGoogleAccount = "noID";
                    registerGCM();
                }
            }
        }



    }

    public void newProgressDialog(String message) {
        //registering progress dialog
        mDialog = new ProgressDialog(this); // this = YourActivity
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setMessage(message);
        mDialog.setIndeterminate(true);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();
    }

    public void getTileDatabase(Context context) {
        Log.d(TAG, "dbLocation:" + context.getFilesDir() + "/mapapp/world.sqlitedb");
        File bd = new File(context.getFilesDir() + "/mapapp/world.sqlitedb");
        DownloadFile d;
        if (!bd.exists()) {
            DownloadDatabase dd = new DownloadDatabase(context);
            dd.execute();
        }
    }

    private class DownloadDatabase extends AsyncTask<Void, Void, Boolean> {
        private Context context;

        public DownloadDatabase(Context c) {
            super();
            this.context = c;
            newProgressDialog("Downloading map database. Please wait...");
        }

        @Override
        protected void onPostExecute(Boolean successful) {
            mDialog.dismiss();
            if(successful) {
                //get offline tile database
                Snackbar snack = Snackbar.make(mCoordinatorLayout, "Congratulations! The registration is complete", Snackbar.LENGTH_INDEFINITE).setActionTextColor(Color.GREEN).setAction("Finish", mFinish);
                View view = snack.getView();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                params.gravity = Gravity.CENTER;
                view.setLayoutParams(params);
                snack.show();
                TokenStore.saveOfflineMapCompleted(context);
                RegisterInFind.sharedInstance(getApplicationContext()).registerCompleted();
            }else{
                Snackbar snack = Snackbar.make(mCoordinatorLayout, "Download failed. Check your Internet connection",
                        Snackbar.LENGTH_INDEFINITE);
                snack.setAction("Try again", mOnTryDownloadAgainListener);
                View view = snack.getView();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                params.gravity = Gravity.CENTER;
                view.setLayoutParams(params);
                snack.show();
            }

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return  DownloadFile.getMapDatabase(context);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(TokenStore.KEY_REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Result from Google Account picker activity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EMAIL && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            mGoogleAccount = accountName;
        } else {
            mGoogleAccount = "noID";
        }
        Log.d(TAG, "Google account: " + mGoogleAccount);
        registerGCM();
        //registering progress dialog
        newProgressDialog("Registering. Please wait...");
    }

    private void registerGCM() {

        // get locale
        String locale = getResources().getConfiguration().locale.getCountry();

        // gets mac_address (user identification)
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        if (!manager.isWifiEnabled())
            manager.setWifiEnabled(true);
        String mac;
        int currentapiVersion = Build.VERSION.SDK_INT;
        if (currentapiVersion <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mac = info.getMacAddress();
        } else {
            mac = DeviceUtils.getWifiMacAddress();
        }

        Log.d(TAG, "Mac_address:" + mac);

        // Start IntentService to register this application with GCM.
        RegistrationIntentService.startGCMRegistration(this, locale, mac, mGoogleAccount);
    }


    /*
     * MENUS
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, AlertActivity.class);
            startActivity(i);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
    }


}
