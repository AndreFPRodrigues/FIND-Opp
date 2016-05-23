package ul.fcul.lasige.findvictim.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.TextView;

import ul.fcul.lasige.findvictim.R;
import ul.fcul.lasige.findvictim.app.Constants;

import static java.lang.String.valueOf;

public class TestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set view
        setContentView(R.layout.activity_tests);
        // set layout to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // set volume to type music
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // get shared preferences file
        SharedPreferences sp = getSharedPreferences(Constants.VICTIM_PREF_FILE, Context.MODE_PRIVATE);

        TextView tv = (TextView) findViewById(R.id.textView6);
        assert tv != null;
        tv.setText(valueOf(sp.getInt("priority", 5)));

        TextView tv2 = (TextView) findViewById(R.id.textView8);
        switch (sp.getInt("color", Color.YELLOW)) {
            case Color.RED:
                assert tv2 != null;
                tv2.setText(R.string.red);
                break;
            case Color.BLACK:
                assert tv2 != null;
                tv2.setText(R.string.black);
                break;
            case Color.YELLOW:
                assert tv2 != null;
                tv2.setText(R.string.yellow);
                break;
            case Color.GREEN:
                assert tv2 != null;
                tv2.setText(R.string.green);
                break;
        }

        TextView tv3 = (TextView) findViewById(R.id.textView10);
        assert tv3 != null;
        tv3.setText(valueOf(sp.getInt("screen_on", 0)));

        TextView tv4 = (TextView) findViewById(R.id.textView12);
        assert tv4 != null;
        tv4.setText(valueOf(sp.getInt("battery_level", 100) + " %"));

        TextView tv5 = (TextView) findViewById(R.id.textView14);
        assert tv5 != null;
        tv5.setText(valueOf(sp.getInt("phone_movement", 0)));

        TextView tv7 = (TextView) findViewById(R.id.textView18);
        int proximity = sp.getInt("phone_proximity", -1);
        if (proximity != -1) {
            if (proximity == 0) {
                assert tv7 != null;
                tv7.setText(R.string.proximity_very_close);
            }
            else if (proximity <= 10) {
                assert tv7 != null;
                tv7.setText(R.string.proximity_close);
            }
            else {
                assert tv7 != null;
                tv7.setText(R.string.proximity_far);
            }
        }
        else {
            assert tv7 != null;
            tv7.setText(R.string.error);
        }

        TextView tv8 = (TextView) findViewById(R.id.textView20);
        int light = sp.getInt("phone_light", -1);
        if (light != -1) {
            if (light < 40) {
                assert tv8 != null;
                tv8.setText(R.string.light_very_dark);
            }
            else if (light < 100) {
                assert tv8 != null;
                tv8.setText(R.string.light_dark);
            }
            else if (light < 350) {
                assert tv8 != null;
                tv8.setText(R.string.light_normal);
            }
            else if (light < 1000) {
                assert tv8 != null;
                tv8.setText(R.string.light_bright);
            }
            else {
                assert tv8 != null;
                tv8.setText(R.string.light_very_bright);
            }
        }
        else {
            assert tv8 != null;
            tv8.setText(R.string.error);
        }

        TextView tv9 = (TextView) findViewById(R.id.textView22);
        assert tv9 != null;
        tv9.setText(valueOf(sp.getInt("battery_temp", -1) + " ºC"));

        TextView tv10 = (TextView) findViewById(R.id.textView24);
        assert tv10 != null;
        tv10.setText(valueOf(sp.getInt("step_counter", -1)));
    }
}
