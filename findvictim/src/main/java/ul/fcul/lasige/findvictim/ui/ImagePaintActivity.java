package ul.fcul.lasige.findvictim.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ul.fcul.lasige.findvictim.R;
import ul.fcul.lasige.findvictim.app.Constants;
import ul.fcul.lasige.findvictim.app.MyCanvas;

public class ImagePaintActivity extends AppCompatActivity {

    private MediaRecorder recorder;
    private boolean sent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set view
        setContentView(R.layout.activity_image_paint);
        // set layout to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // set volume to type music
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // start recording
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(Constants.SAVE_DRAW_VOICE_MESSAGE);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.prepare();
            recorder.start();
        } catch (IOException ignored) {
        }

        Button b = (Button) findViewById(R.id.drawClean);
        assert b != null;
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyCanvas miv = (MyCanvas) findViewById(R.id.drawing);
                assert miv != null;
                miv.clean();
            }
        });

        Button b2 = (Button) findViewById(R.id.drawSend);
        assert b2 != null;
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stop();
                recorder.release();
                String fileName = Constants.SAVE_DRAW_IMAGE;
                OutputStream stream;
                try {
                    stream = new FileOutputStream(new File(fileName));
                    MyCanvas miv = (MyCanvas) findViewById(R.id.drawing);
                    assert miv != null;
                    Bitmap bitmap = Bitmap.createBitmap( miv.getWidth(), miv.getHeight(), Bitmap.Config.ARGB_8888);

                    Canvas canvas = new Canvas(bitmap);
                    miv.draw(canvas);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 25, stream);
                    stream.close();
                    new File(Constants.IMAGE_FILE).delete();
                    sent = true;
                    Toast.makeText(getApplicationContext(), "Picture will be sent as soon as possible", Toast.LENGTH_SHORT).show();
                    finish();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        Button b3 = (Button) findViewById(R.id.drawRed);
        assert b3 != null;
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyCanvas miv = (MyCanvas) findViewById(R.id.drawing);
                assert miv != null;
                miv.changeColor(Color.RED);
            }
        });

        Button b4 = (Button) findViewById(R.id.drawBlue);
        assert b4 != null;
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyCanvas miv = (MyCanvas) findViewById(R.id.drawing);
                assert miv != null;
                miv.changeColor(Color.BLUE);
            }
        });

        Button b5 = (Button) findViewById(R.id.drawBlack);
        assert b5 != null;
        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyCanvas miv = (MyCanvas) findViewById(R.id.drawing);
                assert miv != null;
                miv.changeColor(Color.BLACK);
            }
        });

        Button b6 = (Button) findViewById(R.id.drawWhite);
        assert b6 != null;
        b6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyCanvas miv = (MyCanvas) findViewById(R.id.drawing);
                assert miv != null;
                miv.changeColor(Color.WHITE);
            }
        });
    }

    @Override
    public void finish() {
        if(sent)
            setResult(RESULT_OK);
        else
            setResult(RESULT_CANCELED);
        super.finish();
    }
}
