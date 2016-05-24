package ul.fcul.lasige.findvictim.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unzi.findalert.data.Alert;
import com.example.unzi.findalert.ui.AlertActivity;
import com.example.unzi.offlinemaps.TilesProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import ul.fcul.lasige.findvictim.R;
import ul.fcul.lasige.findvictim.app.Constants;
import ul.fcul.lasige.findvictim.app.PostMessage;
import ul.fcul.lasige.findvictim.data.DatabaseHelper;
import ul.fcul.lasige.findvictim.sensors.SensorsService;

import static android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import static ul.fcul.lasige.findvictim.sensors.SensorsService.Callback;
import static ul.fcul.lasige.findvictim.sensors.SensorsService.startSensorsService;

public class MainActivity extends AppCompatActivity implements
        Callback,
        OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        LocationListener {

    // debug
    private static final String TAG = MainActivity.class.getSimpleName();
    // gcm
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    // server registration
    private String mGoogleAccount;
    // sensor service
    private ServiceConnection mSensorsConnection;
    private SensorsService mSensors;
    // ui
    private View.OnClickListener mOnTryAgainListener;
    // maps
    private static GoogleMap googleMap;
    private boolean marker;
    // record voice message
    private boolean recordOn;
    private MediaRecorder recorder;
    // post message
    private String message = "";
    // database
    private static SQLiteDatabase mDb;
    // voice commands
    private TextToSpeech tts;
    // main activity menu
    private Menu menu;
    // detecta se a plataforma foi desligada manualmente
    private boolean manuallyStop, manuallyStart;
    // alertt
    private boolean ongoingAlert = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set view
        setContentView(R.layout.activity_main_app);
        // set layout to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // set volume to type music
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // set navigation drawer
        setNavigationDrawer();
        //create app folders
        createFolders();
        // start sensor service
        initSensorsService();
        // start maps
        initMaps();
        // start database
        initDB();
        // check for alerts
        checkAlert();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // bind to sensors service
        mSensorsConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mSensors = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                final SensorsService.SensorsBinder binder = (SensorsService.SensorsBinder) service;
                mSensors = binder.getSensors();
                mSensors.addCallback(MainActivity.this);
            }
        };
        bindService(new Intent(this, SensorsService.class), mSensorsConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mSensors != null) {
            mSensors.removeCallback(this);
        }
        // unbind from sensor service
        unbindService(mSensorsConnection);
        mSensorsConnection = null;
    }

    /**
     * Don't allow the user close the application by pressing the back button
     */
    @Override
    public void onBackPressed() {
        if (ongoingAlert) {
            AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
            b.setTitle("Exit Application");
            b.setMessage("If you really want to exit the application, please avoid spending unnecessary battery. Exit anyway?");
            b.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.super.onBackPressed();
                }
            });
            b.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            b.show();
        }
        else
            super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.TAKE_PICTURE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Intent i = new Intent(this, ImagePaintActivity.class);
                    startActivityForResult(i, Constants.SEND_FILE_REQUEST_CODE);
                }
            break;
            case Constants.ASK_REACH_PHONE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String spokenText = results.get(0);

                    if (!spokenText.equals("")) {
                        if (spokenText.contains("help") || spokenText.contains("no") || spokenText.contains("bad")) {
                            tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                                @Override
                                public void onInit(int status) {
                                    tts.setLanguage(Locale.UK);
                                    tts.setSpeechRate(0.8f);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        tts.speak("Can you move or reach the phone?", TextToSpeech.QUEUE_FLUSH, null, null);
                                    } else {
                                        tts.speak("Can you move or reach the phone?", TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                }
                            });
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                            startActivityForResult(intent, Constants.ASK_OPTION_REQUEST_CODE);
                        } else if (spokenText.contains("good") || spokenText.contains("ok") || spokenText.contains("well") || spokenText.contains("yes")) {
                            Constants.ASK_STATE_TIME = 2;
                        }
                    }
                }
                break;
            case Constants.ASK_OPTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String spokenText = results.get(0);

                    if (!spokenText.equals("")) {
                        if (spokenText.contains("help") || spokenText.contains("no") || spokenText.contains("bad")) {
                            tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                                @Override
                                public void onInit(int status) {
                                    tts.setLanguage(Locale.UK);
                                    tts.setSpeechRate(0.8f);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        tts.speak("What you want do to?", TextToSpeech.QUEUE_FLUSH, null, null);
                                        tts.speak("1", TextToSpeech.QUEUE_ADD, null, null);
                                        tts.speak("Send a voice message", TextToSpeech.QUEUE_ADD, null, null);
                                        tts.speak("2", TextToSpeech.QUEUE_ADD, null, null);
                                        tts.speak("Post a message on the message board", TextToSpeech.QUEUE_ADD, null, null);
                                        tts.speak("3", TextToSpeech.QUEUE_ADD, null, null);
                                        tts.speak("Do nothing", TextToSpeech.QUEUE_ADD, null, null);
                                    } else {
                                        tts.speak("What you want do to?", TextToSpeech.QUEUE_FLUSH, null);
                                        tts.speak("1", TextToSpeech.QUEUE_ADD, null);
                                        tts.speak("Send a voice message", TextToSpeech.QUEUE_ADD, null);
                                        tts.speak("2", TextToSpeech.QUEUE_ADD, null);
                                        tts.speak("Post a message on the message board", TextToSpeech.QUEUE_ADD, null);
                                        tts.speak("3", TextToSpeech.QUEUE_ADD, null);
                                        tts.speak("Do nothing", TextToSpeech.QUEUE_ADD, null);
                                    }
                                }
                            });
                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                            startActivityForResult(intent, Constants.GET_ANSWERED_OPTION_REQUEST_CODE);
                        }
                    }
                }
                break;
            case Constants.GET_ANSWERED_OPTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String spokenText = results.get(0);

                    if (!spokenText.equals("")) {
                        if (spokenText.contains("one")) {
                            tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                                @Override
                                public void onInit(int status) {
                                    tts.setLanguage(Locale.UK);
                                    tts.setSpeechRate(0.8f);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        tts.speak("Speak now for 20 seconds to send a voice message", TextToSpeech.QUEUE_FLUSH, null, null);
                                    } else {
                                        tts.speak("Speak now for 20 seconds to send a voice message", TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                }
                            });

                            recorder = new MediaRecorder();
                            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            recorder.setOutputFile(Constants.SAVE_VOICE_MESSAGE);
                            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                            recorder.setMaxDuration(1000 * 20);
                            recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                                @Override
                                public void onInfo(MediaRecorder mr, int what, int extra) {
                                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                                        recorder.stop();
                                        recorder.release();
                                        recorder = null;

                                        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                                            @Override
                                            public void onInit(int status) {
                                                tts.setLanguage(Locale.UK);
                                                tts.setSpeechRate(0.8f);
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                    tts.speak("Your message was saved and will be sent as soon as possible", TextToSpeech.QUEUE_FLUSH, null, null);
                                                } else {
                                                    tts.speak("Your message was saved and will be sent as soon as possible", TextToSpeech.QUEUE_FLUSH, null);
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                            try {
                                recorder.prepare();
                            } catch (IOException ignored) {
                            }

                            recorder.start();
                        }
                        else if(spokenText.contains("two")) {
                            tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                                @Override
                                public void onInit(int status) {
                                    tts.setLanguage(Locale.UK);
                                    tts.setSpeechRate(0.8f);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        tts.speak("Speak now to post a message", TextToSpeech.QUEUE_FLUSH, null, null);
                                    } else {
                                        tts.speak("Speak now to post a message", TextToSpeech.QUEUE_FLUSH, null);
                                    }
                                }
                            });

                            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                            startActivityForResult(intent, Constants.POST_MESSAGE_REQUEST_CODE);
                        }
                    }
                }
                break;
            case Constants.POST_MESSAGE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String spokenText = results.get(0);

                    if (!spokenText.equals("")) {
                        PostMessage newMsg = new PostMessage();
                        newMsg.sender = "Me Myself and I";
                        newMsg.sender_type = "Rescuer";
                        newMsg.content = spokenText;

                        long currentTime = System.currentTimeMillis() / 1000L;
                        newMsg.timeSent = currentTime;
                        newMsg.timeReceived = currentTime;

                        PostMessage.Store.addMessage(mDb, newMsg);
                        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                tts.setLanguage(Locale.UK);
                                tts.setSpeechRate(0.8f);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    tts.speak("Message posted", TextToSpeech.QUEUE_FLUSH, null, null);
                                } else {
                                    tts.speak("Message posted", TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                        });
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onActivationStateChanged(boolean activated) {

    }

    /*
     * MENUS
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.victim, menu);
        this.menu = menu;
        Cursor cursor = Alert.Store.fetchAlerts(
                com.example.unzi.findalert.data.DatabaseHelper.getInstance(getApplicationContext()).getReadableDatabase(),
                Alert.STATUS.ONGOING);

        if (!cursor.moveToFirst()) {
            cursor.close();
        }
        else {
            menu.findItem(R.id.platform_status).setIcon(R.drawable.green_circle);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.menu_victim_record && !recordOn){
            item.setIcon(R.drawable.ic_mic_none_white_48dp);
            recordOn = true;
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(Constants.SAVE_VOICE_MESSAGE);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setMaxDuration(1000 * 20);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(R.drawable.ic_mic_none_black_48dp)
                    .setTitle("Voice Message")
                    .setMessage("Speak now to send a voice message. 20 seconds remaining")
                    .setNeutralButton("Stop", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            recorder.stop();
                            recorder.release();
                            recorder = null;

                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(500);

                            new AlertDialog.Builder(MainActivity.this)
                                    .setIcon(R.drawable.ic_mic_none_black_48dp)
                                    .setTitle("Voice Message")
                                    .setMessage("Do you want to send the message?")
                                    .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            recordOn = false;
                                            item.setIcon(R.drawable.ic_mic_none_white_48dp);
                                            Toast.makeText(getApplicationContext(), "Message will be sent as soon as possible", Toast.LENGTH_LONG).show();
                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    File f = new File(Constants.SAVE_VOICE_MESSAGE);
                                    if (f.delete())
                                        Log.d(TAG, "Voice message file deleted successfully");
                                    recordOn = false;
                                    item.setIcon(R.drawable.ic_mic_none_white_48dp);
                                }
                            }).setCancelable(false).show();
                        }
                    }).setCancelable(false);

            final AlertDialog dlg = builder.show();

            new CountDownTimer(20000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    dlg.setMessage("Speak now to send a voice message. " + (millisUntilFinished/1000) + " seconds remaining");
                }

                @Override
                public void onFinish() {

                }
            }.start();

            recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        recorder.stop();
                        recorder.release();
                        recorder = null;

                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(500);

                        dlg.dismiss();
                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(R.drawable.ic_mic_none_black_48dp)
                                .setTitle("Voice Message")
                                .setMessage("Do you want to send the message?")
                                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        recordOn = false;
                                        item.setIcon(R.drawable.ic_mic_none_white_48dp);
                                        Toast.makeText(getApplicationContext(), "Message will be sent as soon as possible", Toast.LENGTH_LONG).show();
                                    }
                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File f = new File(Constants.SAVE_VOICE_MESSAGE);
                                if (f.delete())
                                    Log.d(TAG, "Voice message file deleted successfully");
                                recordOn = false;
                                item.setIcon(R.drawable.ic_mic_none_white_48dp);
                            }
                        }).setCancelable(false).show();
                    }
                }
            });

            try {
                recorder.prepare();
            } catch (IOException ignored) {
            }

            recorder.start();

            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.toggleButton) {
            if(mSensors != null) {
                if(mSensors.isActivated()) {
                    // turn off
                    MenuItem status = menu.findItem(R.id.platform_status);
                    status.setIcon(R.drawable.red_circle);
                    item.setTitle("Start");
                    item.setIcon(R.drawable.red_circle);
                    mSensors.deactivateSensors();
                    manuallyStop = true;
                    Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
                }
                else {
                    MenuItem status = menu.findItem(R.id.platform_status);
                    status.setIcon(R.drawable.green_circle);
                    item.setTitle("Stop");
                    item.setIcon(R.drawable.green_circle);
                    mSensors.activateSensors(true);
                    Toast.makeText(getApplicationContext(), "Started", Toast.LENGTH_SHORT).show();
                }
            }
        /*} else if (id == R.id.nav_sos) {
            Intent sos = new Intent(Intent.ACTION_CALL, Uri.parse(Constants.CALL_112));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(sos);
            }*/
        } else if (id == R.id.nav_take_picture) {
            try {
                Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File img = new File(Constants.SAVE_IMAGE);
                if (img.createNewFile())
                    Log.d(TAG, "Saved image file created successfully");
                camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(img));
                startActivityForResult(camera, Constants.TAKE_PICTURE_REQUEST_CODE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.nav_send_msg) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Post Message");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(160)});
            builder.setView(input);

            builder.setPositiveButton("Post", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    message = input.getText().toString();
                    String check = message.trim();

                    if (!check.isEmpty()) {
                        PostMessage newMsg = new PostMessage();
                        newMsg.sender = "Me Myself and I";
                        newMsg.sender_type = "Victim";
                        newMsg.content = message;

                        long currentTime = System.currentTimeMillis() / 1000L;
                        newMsg.timeSent = currentTime;
                        newMsg.timeReceived = currentTime;

                        PostMessage.Store.addMessage(mDb, newMsg);
                        Toast.makeText(MainActivity.this, "Posted", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        } else if (id == R.id.nav_msg_board) {
            Intent board = new Intent(this, MessageBoardActivity.class);
            startActivity(board);
        } /*else if (id == R.id.nav_info) {
            Intent info = new Intent(this, AdditionalInformationActivity.class);
            startActivity(info);
        } else if (id == R.id.nav_help) {
            Intent help = new Intent(this, HelpActivity.class);
            startActivity(help);
        } else if (id == R.id.nav_options) {
            Intent advancedOptions = new Intent(this, OptionsActivity.class);
            startActivity(advancedOptions);
        } else if (id == R.id.nav_tests) {
            Intent tests = new Intent(this, TestsActivity.class);
            startActivity(tests);
        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_Layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onMapReady(GoogleMap maps) {
        googleMap = maps;
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                LatLng location2 = new LatLng(location.getLatitude(), location.getLongitude());

                if (!marker) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location2, 18.0f));
                    marker = true;
                }
            }
        });
    }

    public static GoogleMap getGoogleMaps () {
        return googleMap;
    }

    private void createFolders () {
        File f = new File(Constants.ROOT_FOLDER);
        if (!f.exists())
            if (f.mkdir())
                Log.d(TAG, "Root folder created successfully");
        f = new File(Constants.ROOT_FOLDER + Constants.RECORDS_FOLDER);
        if (!f.exists())
            if (f.mkdir())
                Log.d(TAG, "Records folder created successfully");
        f = new File(Constants.ROOT_FOLDER + Constants.RECEIVE_FOLDER);
        if (!f.exists())
            if (f.mkdir())
                Log.d(TAG, "Receive folder created successfully");
        f = new File(Constants.ROOT_FOLDER + Constants.IMAGES_FOLDER);
        if (!f.exists())
            if (f.mkdir())
                Log.d(TAG, "Images folder created successfully");
    }

    NavigationView navigationView;

    private DrawerLayout setNavigationDrawer () {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_Layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);
        //navigationView.setItemTextColor(ColorStateList.valueOf(Color.WHITE));
        //navigationView.setBackgroundColor(Color.BLACK);
        navigationView.getMenu().findItem(R.id.nav_take_picture).getIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        navigationView.getMenu().findItem(R.id.nav_send_msg).getIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        navigationView.getMenu().findItem(R.id.nav_msg_board).getIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

        return drawer;
    }

    /**
     * Initializes the sensors service
     */
    private void initSensorsService () {
        startSensorsService(this);
    }

    /**
     * Initializes the maps ans shoe the localization of the user
     */
    private void initMaps () {
        marker = false;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.victimMap);
        googleMap = mapFragment.getMap();
        String path = getFilesDir()+ "/mapapp/world.sqlitedb";
        if (new File(path).exists()) {
           new TilesProvider(googleMap, path);
        }
        else
            mapFragment.getMapAsync(this);
    }

    /**
     * Initializes the database
     */
    private void initDB() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        mDb = dbHelper.getWritableDatabase();
    }

    public static SQLiteDatabase getDB () {
        return mDb;
    }

    private void checkAlert () {
        Cursor cursor = Alert.Store.fetchAlerts(
                com.example.unzi.findalert.data.DatabaseHelper.getInstance(getApplicationContext()).getReadableDatabase(),
                Alert.STATUS.ONGOING);

        if (!cursor.moveToFirst()) {
            cursor.close();
            if (ongoingAlert) {
                MenuItem mi = navigationView.getMenu().findItem(R.id.toggleButton);
                mi.setIcon(R.drawable.red_circle);
                mi.setTitle("Start");
                View header = navigationView.getHeaderView(0);
                ImageView iv = (ImageView) header.findViewById(R.id.alert_status);
                assert iv != null;
                iv.setImageResource(R.drawable.alert_ok);
                TextView tv = (TextView) header.findViewById(R.id.alert_name);
                tv.setText("No alerts at the moment");
                Button b = (Button) header.findViewById(R.id.see_alert);
                b.setVisibility(View.GONE);
            }
        }
        else {
            if (!manuallyStop) {
                Alert a = Alert.fromCursor(cursor);
                MenuItem mi = navigationView.getMenu().findItem(R.id.toggleButton);
                mi.setIcon(R.drawable.green_circle);
                mi.setTitle("Stop");
                View header = navigationView.getHeaderView(0);
                ImageView iv = (ImageView) header.findViewById(R.id.alert_status);
                assert iv != null;
                iv.setImageResource(R.drawable.danger_alert);
                TextView tv = (TextView) header.findViewById(R.id.alert_name);
                tv.setText(a.getName());
                Button b = (Button) header.findViewById(R.id.see_alert);
                b.setVisibility(View.VISIBLE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this, AlertActivity.class));
                    }
                });
            }
        }
    }
}

