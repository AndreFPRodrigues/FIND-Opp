package ul.fcul.lasige.findvictim.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ul.fcul.lasige.findvictim.app.Constants;
import ul.fcul.lasige.findvictim.app.Route;
import ul.fcul.lasige.findvictim.sensors.SensorManager;
import ul.fcul.lasige.findvictim.sensors.SensorsService;
import ul.fcul.lasige.findvictim.ui.MainActivity;

/**
 * Created by hugonicolau on 26/11/15.
 */
public class MessageGenerator {

    public static class GenerateMessageTask implements Runnable {
        private static final String TAG = GenerateMessageTask.class.getSimpleName();

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

            // build message
            Message message = new Message();
            message.OriginMac = mMacAdress;
            message.TimeSent = System.currentTimeMillis();
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
                message.LocationTimestamp = location.getTime();
            }
            File f = new File(Constants.SAVE_VOICE_MESSAGE);

            if (f.exists()) {
                message.VoiceMessage = convertFileToByteArray(f);
                f.delete();
            }

            f = new File(Constants.SAVE_DRAW_IMAGE);
            File ff = new File(Constants.SAVE_DRAW_VOICE_MESSAGE);
            Log.d(TAG, "@@@ voice message: " + f.exists() + " " + Constants.SAVE_DRAW_IMAGE);
            Log.d(TAG, "@@@ voice message: " + ff.exists() + " " + Constants.SAVE_DRAW_VOICE_MESSAGE);
            if (f.exists() && ff.exists()) {
                message.DrawImage = convertFileToByteArray(f);
                f.delete();
                message.DrawImageVoiceMessage = convertFileToByteArray(ff);
                ff.delete();
            }

            JSONArray array = new JSONArray();
            try (Cursor c = Route.Store.fetchAllRoutes(MainActivity.getDB())) {
                while (c.moveToNext()) {
                    JSONObject json2 = new JSONObject();
                    json2.put("start_lat", c.getDouble(c.getColumnIndex("start_lat")));
                    json2.put("end_lat", c.getDouble(c.getColumnIndex("end_lat")));
                    json2.put("start_lng", c.getDouble(c.getColumnIndex("start_lng")));
                    json2.put("end_kng", c.getDouble(c.getColumnIndex("end_lng")));
                    array.put(json2);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            message.Routes = array;

            // send message
            mSensorService.sendMessage(message);
        }
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
}
