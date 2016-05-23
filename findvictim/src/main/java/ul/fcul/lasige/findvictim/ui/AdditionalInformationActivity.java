package ul.fcul.lasige.findvictim.ui;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import ul.fcul.lasige.findvictim.R;

public class AdditionalInformationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set view
        setContentView(R.layout.activity_additional_information);
        // set layout to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // set volume to type music
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
}
