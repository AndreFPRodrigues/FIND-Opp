package ul.fcul.lasige.findvictim.sensors;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;

import java.util.Locale;

import ul.fcul.lasige.findvictim.app.Constants;

/**
 * Created by afons on 06/04/2016.
 * Sensor to detect the time passed
 */
public class TimeSensor extends AbstractSensor {

    private TextToSpeech tts;
    private int time;

    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            time++;
            checkTime(context);
        }
    };

    /**
     * Creates a new sensor
     *
     * @param c Android mContext from which it is possible to obtain sensors.
     */
    public TimeSensor(Context c) {
        super(c);
        time = 0;
    }

    @Override
    public void startSensor() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        mContext.registerReceiver(timeReceiver, iFilter);
    }

    @Override
    public Object getCurrentValue() {
        return null;
    }

    @Override
    public void stopSensor() {
        mContext.unregisterReceiver(timeReceiver);
    }

    private void checkTime(final Context context) {
        if (time % Constants.ASK_STATE_TIME == 0) {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(1000);

            Activity activity = (Activity) context;
            activity.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);


            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        tts.setLanguage(Locale.UK);
                        tts.setSpeechRate(0.8f);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                tts.speak("Are you ok?", TextToSpeech.QUEUE_FLUSH, null, null);
                        } else {
                            tts.speak("Are you ok?", TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                }
            });

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            activity.startActivityForResult(intent, Constants.ASK_REACH_PHONE_REQUEST_CODE);
        }
    }
}
