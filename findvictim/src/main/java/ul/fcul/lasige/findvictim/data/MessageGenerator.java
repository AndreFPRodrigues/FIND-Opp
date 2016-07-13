package ul.fcul.lasige.findvictim.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;

import ul.fcul.lasige.findvictim.app.Constants;
import ul.fcul.lasige.findvictim.sensors.SensorManager;
import ul.fcul.lasige.findvictim.sensors.SensorsService;

/**
 * Created by hugonicolau on 26/11/15.
 */
public class MessageGenerator {
    private  final LinkedList<String> mTextMessages ;
    private static  MessageGenerator mSharedInstance;
    private GenerateMessageTask genarator;
    public static MessageGenerator getSharedInstance(){
        if(mSharedInstance==null)
            mSharedInstance=new MessageGenerator();
        return mSharedInstance;
    }

    private MessageGenerator(){
        mTextMessages = new LinkedList<String>();
    }

    public GenerateMessageTask startMessageGenaration(Context context, SensorManager sensormanager, SensorsService sensorsService){
        genarator= new GenerateMessageTask(context, sensormanager, sensorsService);
        return genarator;
    }
    public class GenerateMessageTask implements Runnable {
        private  final String TAG = GenerateMessageTask.class.getSimpleName();

        private final SensorManager mSensorsManager;
        private final SensorsService mSensorService;
        private final Context mContext;
        private final String mMacAdress;


        public GenerateMessageTask(Context context, SensorManager sensormanager, SensorsService sensorsService) {
            super();
            mSensorsManager = sensormanager;
            mSensorService = sensorsService;
            mContext = context;
            Log.d(TAG, "GenerateMessageTask");
            mMacAdress= com.example.unzi.findalert.data.TokenStore.getMacAddress(context);

        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            // generate message
            Log.d(TAG, "RunningMessageGenerator");
            // get sensor value
            Integer movement = (Integer) mSensorsManager.getSensorCurrentValue(SensorManager.SensorType.Accelerometer);
            Integer[] battery = (Integer[]) mSensorsManager.getSensorCurrentValue(SensorManager.SensorType.Battery);
            Integer batteryLevel = battery[0];
            Integer batteryTemp = battery[1];
            Location location = (Location) mSensorsManager.getSensorCurrentValue(SensorManager.SensorType.Location);
            Integer screenOn = (Integer)  mSensorsManager.getSensorCurrentValue(SensorManager.SensorType.ScreenOn);
            Integer proximity = (Integer) mSensorsManager.getSensorCurrentValue(SensorManager.SensorType.Proximity);
            Integer light = (Integer) mSensorsManager.getSensorCurrentValue(SensorManager.SensorType.Light);
            Integer stepCounter = (Integer) mSensorsManager.getSensorCurrentValue(SensorManager.SensorType.StepCounter);

            Log.d(TAG, "###  Movement " + movement);
            Log.d(TAG, "###  battery level " + batteryLevel);
            Log.d(TAG, "###  battery temp " + batteryTemp);
            Log.d(TAG, "###  screen on " + screenOn);
            Log.d(TAG, "###  Proximity " + proximity);
            Log.d(TAG, "###  light " + light);
            Log.d(TAG, "###  step counter " + stepCounter);

            Calendar c = Calendar.getInstance();
            Calendar calendar = new GregorianCalendar(c.getTimeZone());
          long seconds =calendar.getTimeInMillis();
            Log.d(TAG, "Time generated:" + seconds);
            int gmtOffset2 = TimeZone.getDefault().getOffset(seconds);
            // build message
            Message message = new Message();
            message.OriginMac = mMacAdress;
            message.TimeSent = seconds+gmtOffset2;//System.currentTimeMillis();
            message.TimeReceived = -1;
            message.Movement = movement;
            message.BatteryLevel = batteryLevel;
            message.BatteryTemp = batteryTemp;
            message.ScreenOn = screenOn;
            message.Proximity = proximity;
            message.Light = light;
            message.StepCounter = stepCounter;
            if(location != null) {
                message.LocationLatitude = location.getLatitude();
                message.LocationLongitude = location.getLongitude();
                message.LocationAccuracy = location.getAccuracy();
                Log.d(TAG, "gmt time location:" + location.getTime());
                int gmtOffset = TimeZone.getDefault().getOffset(location.getTime());
                Log.d(TAG,"gmt time offset:"+gmtOffset);
                message.LocationTimestamp = location.getTime()+gmtOffset;
            }

            if(mTextMessages.size()>0){
                message.TextMessage= mTextMessages.poll();
            }


            File f = new File(Constants.SAVE_VOICE_MESSAGE);

            if (f.exists()) {
                message.VoiceMessage = convertFileToByteArray(f);
                f.delete();
            }

            f = new File(Constants.SAVE_IMAGE);
            File ff = new File(Constants.SAVE_DRAW_VOICE_MESSAGE);
            Log.d(TAG, "@@@ voice message: " + f.exists() + " " + Constants.SAVE_IMAGE);
            Log.d(TAG, "@@@ voice message: " + ff.exists() + " " + Constants.SAVE_DRAW_VOICE_MESSAGE);
            if (f.exists() && ff.exists()) {
                message.DrawImage = getBytesFromBitmap(f);
                f.delete();
                message.DrawImageVoiceMessage = convertFileToByteArray(ff);
                ff.delete();
            }

            // send message
            mSensorService.sendMessage(message);
        }
    }

    public byte[] getBytesFromBitmap(File imagefile) {



        Bitmap bitmap = decodeFile(imagefile);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream);
        return stream.toByteArray();
    }

    // Decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            // The new size we want to scale to
            final int REQUIRED_SIZE=200;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while(o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }

    private static byte[] convertFileToByteArray(File f) {
        byte[] byteArray = null;
        try {
            InputStream inputStream = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024*8];
            int bytesRead;

            while ((bytesRead = inputStream.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }

            byteArray = bos.toByteArray();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return byteArray;
    }

    public void addTextMessage(String message){
        mTextMessages.add(message);
    }
}
