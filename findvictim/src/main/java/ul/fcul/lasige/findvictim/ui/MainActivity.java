package ul.fcul.lasige.findvictim.ui;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.unzi.findalert.data.Alert;
import com.example.unzi.findalert.data.Route;
import com.example.unzi.findalert.ui.RegisterInFind;
import com.example.unzi.offlinemaps.TilesProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ul.fcul.lasige.findvictim.R;
import ul.fcul.lasige.findvictim.app.Constants;
import ul.fcul.lasige.findvictim.app.PostMessage;
import ul.fcul.lasige.findvictim.data.DatabaseHelper;
import ul.fcul.lasige.findvictim.data.MessageGenerator;
import ul.fcul.lasige.findvictim.sensors.SensorsService;

import static android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import static ul.fcul.lasige.findvictim.sensors.SensorsService.Callback;
import static ul.fcul.lasige.findvictim.sensors.SensorsService.startSensorsService;

public class MainActivity extends AppCompatActivity implements
        Callback,OnNavigationItemSelectedListener {

    // debug
    private static final String TAG = MainActivity.class.getSimpleName();

    // sensor service
    private ServiceConnection mSensorsConnection;
    private SensorsService mSensors;

    // maps
    private static GoogleMap googleMap;

    // record voice message
    private boolean recordOn;
    private MediaRecorder recorder;
    // post message
    private String message = "";
    // database
    private static SQLiteDatabase mDb;
    // voice commands
    private TextToSpeech tts;
    // navigation drawer
    private  NavigationView navigationView;
    // double click timestamp
    private long timestamp;

    //alert borders
    private  Polygon mAlertBorders;
    private ArrayList<Polyline> mRoutes;


    private TilesProvider mTileProvider;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "On create main activity");
        // set view
        setContentView(R.layout.activity_main_app);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        mRoutes = new ArrayList<Polyline>();

        // set layout to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // set volume to type music
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //create app folders
        createFolders();
        // start sensor service
        initSensorsService();
        // start maps
        initMaps();
        // start database
        initDB();
        // check for alerts
        //checkAlert();


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
                // set navigation drawer
                setNavigationDrawer();
            }
        };
        bindService(new Intent(this, SensorsService.class), mSensorsConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "On resume main activity");

        //Make sure we have registered the app and downloaded the map
        RegisterInFind findRegister = RegisterInFind.sharedInstance(this);
        findRegister.register();
        initMaps();
        checkAlert();
        if(mSensors!=null){
            updatePlatformStatus();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSensors != null) {
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
        super.onBackPressed();
        /*if (Constants.ONGOING_ALERT == 2) {
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
        } else
            super.onBackPressed();*/
    }

    //cancel permanent alert notification
    private void cancelNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(2);
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
                        } else if (spokenText.contains("two")) {
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
                        newMsg.sender = "Me";
                        newMsg.sender_type = "";
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
       //downloaded all maps for crete
        /* if(mTileProvider!=null) {
            mTileProvider.downloadTilesInBound(35.377589, 24.450248, 35.3447841, 24.4948556, 1, 16, getApplicationContext());
            return true;
        }*/
        int id = item.getItemId();

        if (id == R.id.menu_victim_record && !recordOn) {
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
                    dlg.setMessage("Speak now to send a voice message. " + (millisUntilFinished / 1000) + " seconds remaining");
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

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.toggleButton) {
            if(mSensors != null) {

                long aux = System.currentTimeMillis();
                if (aux - timestamp < 1000) {

                    if(mSensors.isActivated()) {
                        // turn off
                            AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                            b.setTitle("Stop Services");
                            b.setMessage("Do you really want to stop the services?");
                            b.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    item.setTitle("Idle");
                                    item.setIcon(R.drawable.stop);
                                    mSensors.deactivateSensors();
                                    Constants.MANUALLY_STOPPED = true;
                                    Constants.MANUALLY_STARTED = false;
                                    Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
                                    cancelNotification();

                                }
                            });
                            b.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                            b.show();

                    }
                    else {

                        item.setTitle("Running...");
                        item.setIcon(R.drawable.running);
                        mSensors.activateSensors(true);
                        Constants.MANUALLY_STARTED = true;
                        Constants.MANUALLY_STOPPED = false;
                        Toast.makeText(getApplicationContext(), "Started", Toast.LENGTH_SHORT).show();


                    }
                } else {
                    timestamp = aux;
                }
                return true;
            }
        }
        if (id == R.id.nav_take_picture) {
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
                        newMsg.sender = "Me";
                        newMsg.sender_type = "";
                        newMsg.content = message;

                        long currentTime = System.currentTimeMillis() / 1000L;
                        newMsg.timeSent = currentTime;
                        newMsg.timeReceived = currentTime;

                        PostMessage.Store.addMessage(DatabaseHelper.getInstance(getApplicationContext()).getWritableDatabase(), newMsg);
                        MessageGenerator.getSharedInstance().addTextMessage(message);
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


    public static GoogleMap getGoogleMaps() {
        return googleMap;
    }

    private void createFolders() {
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

    private DrawerLayout setNavigationDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_Layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();

        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);
        //navigationView.setItemTextColor(ColorStateList.valueOf(Color.WHITE));
        //navigationView.setBackgroundColor(Color.BLACK);
        navigationView.getMenu().findItem(R.id.nav_take_picture).getIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        navigationView.getMenu().findItem(R.id.nav_send_msg).getIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        navigationView.getMenu().findItem(R.id.nav_msg_board).getIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

        updatePlatformStatus();

        return drawer;
    }

    private void updatePlatformStatus(){
        // /start and stop
        if ( mSensors.isActivated()) {
            navigationView.getMenu().findItem(R.id.toggleButton).setTitle("Running...").setIcon(R.drawable.running);
        } else {
            navigationView.getMenu().findItem(R.id.toggleButton).setTitle("Idle").setIcon(R.drawable.stop);
        }
    }




    /**
     * Initializes the sensors service
     */
    private void initSensorsService() {
        startSensorsService(this);
    }

    /**
     * Initializes the maps ans shoe the localization of the user
     */
    private void initMaps() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.victimMap);
        googleMap = mapFragment.getMap();
        String path = getFilesDir() + "/mapapp/world.sqlitedb";
        if (new File(path).exists()) {
          mTileProvider=  new TilesProvider(googleMap, path);
        }

    }

    /**
     * Initializes the database
     */
    private void initDB() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        mDb = dbHelper.getWritableDatabase();
    }

    public static SQLiteDatabase getDB() {
        return mDb;
    }


    public void checkAlert() {
        Log.d(TAG,"Checking alerts");
        Cursor cursor = Alert.Store.fetchAlerts(
                com.example.unzi.findalert.data.DatabaseHelper.getInstance(getApplicationContext()).getReadableDatabase(),
                Alert.STATUS.ONGOING);
        if (cursor.moveToFirst()) {
            final Alert a = Alert.fromCursor(cursor);
            setAlertWindow(a);
            MenuItem mi = navigationView.getMenu().findItem(R.id.toggleButton);
            mi.setTitle("Running...");
            setAlertBounds(a);
            setRoutes();
            cursor.close();

            return ;
        }
        cursor.close();

        cursor = Alert.Store.fetchAlerts(
                com.example.unzi.findalert.data.DatabaseHelper.getInstance(getApplicationContext()).getReadableDatabase(),
                Alert.STATUS.SCHEDULED);
        if (cursor.moveToFirst()) {
            final Alert a = Alert.fromCursor(cursor);
            setAlertWindow(a);
            setRoutes();
            setAlertBounds(a);
            cursor.close();
            return;
        }
        hideAlertWindow();
        clearFinishedAlert();
        clearRoutes();

    }

    private void setRoutes(){
        Cursor cursor = Route.Store.fetchAllRoutes(
                com.example.unzi.findalert.data.DatabaseHelper.getInstance(getApplicationContext()).getReadableDatabase());
        while(cursor.moveToNext()) {
            Route route = Route.fromCursor(cursor);
            drawRoute(route);
        }
        cursor.close();
    }

    private void drawRoute(Route route) {
        Polyline p = googleMap.addPolyline(new PolylineOptions()
                .add(new LatLng(route.getStart_lat(), route.getStart_lng()), new LatLng(route.getEnd_lat(), route.getEnd_lng()))
                .width(7)
                .color(Color.BLUE));
        p.setZIndex(151);
        mRoutes.add(p);
    }

    private void clearRoutes() {
        for(Polyline p : mRoutes)
            p.remove();
    }

    private void hideAlertWindow(){
        Log.d(TAG,"Hiding alerts");
        View header = navigationView.getHeaderView(0);
        header.findViewById(R.id.alertDetails).setVisibility(View.GONE);
        header.findViewById(R.id.alert_status).setVisibility(View.GONE);

        TextView tv = (TextView) header.findViewById(R.id.alert_name);
        tv.setText("No active alerts");
        TextView type = (TextView)  header.findViewById(R.id.alertType);
        type.setText("");
    }

    private void setAlertWindow(Alert a){
        Log.d(TAG,"Set nav alert window");

        View header = navigationView.getHeaderView(0);
        header.findViewById(R.id.alertDetails).setVisibility(View.VISIBLE);
        header.findViewById(R.id.alert_status).setVisibility(View.VISIBLE);
        TextView tv = (TextView) header.findViewById(R.id.alert_name);
        tv.setText(a.getName() );
        TextView type = (TextView)  header.findViewById(R.id.alertType);
        type.setText(" - " + a.getType());
        TextView description = (TextView)  header.findViewById(R.id.alertDescription);
        description.setText( a.getDescription());
        TextView date = (TextView)  header.findViewById(R.id.alertDate);
        date.setText( a.getDate().toString());
    }



    private void clearFinishedAlert() {
        Log.d(TAG,"Clearing last alert");
        if(mAlertBorders!=null) {
            Log.d(TAG,"Clearing Polyline");
            mAlertBorders.remove();
        }
        View header = navigationView.getHeaderView(0);
        TextView tv = (TextView) header.findViewById(R.id.alert_name);
        tv.setText("No active alerts");

    }

    private void setAlertBounds(Alert mAlert) {
        Log.d(TAG,"Set alert bounds");
        if(mAlertBorders==null) {
            PolygonOptions rectOptions = new PolygonOptions()
                    .add(new LatLng(mAlert.getLatStart(), mAlert.getLonStart()))
                    .add(new LatLng(mAlert.getLatEnd(), mAlert.getLonStart()))
                    .add(new LatLng(mAlert.getLatEnd(), mAlert.getLonEnd()))
                    .add(new LatLng(mAlert.getLatStart(), mAlert.getLonEnd()));

            mAlertBorders = googleMap.addPolygon(rectOptions);
            mAlertBorders.setZIndex(100);
        }
        LatLng focus = midPoint(mAlert.getLatStart(), mAlert.getLonStart(), mAlert.getLatEnd(), mAlert.getLonEnd());
        LatLngBounds lngBounds = new LatLngBounds(new LatLng(mAlert.getLatEnd(), mAlert.getLonEnd()), new LatLng(mAlert.getLatStart(), mAlert.getLonStart()));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(lngBounds, 400, 600, 0));
    }

    private LatLng midPoint(double lat1, double lon1, double lat2, double lon2) {

        double dLon = Math.toRadians(lon2 - lon1);
        //convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);

        return new LatLng(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }


}

