package ul.fcul.lasige.findvictim.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by afonso on 27-03-2016.
 */
public class AccelerometerSensor extends AbstractSensor implements SensorEventListener {

    private SharedPreferences sp;

    private SensorManager sm;
    private Integer value;
    private float x, y, z;
    private double lastUpdate;
    /**
     * Creates a new sensor
     *
     * @param c Android mContext from which it is possible to obtain sensors.
     */
    public AccelerometerSensor(Context c) {
        super(c);
        sp = c.getSharedPreferences("victimSharedPreferencesFile", Context.MODE_PRIVATE);
        sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        value = 0;
        x = y = z = 0;
    }

    @Override
    public void startSensor() {
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public Object getCurrentValue() {
        return value!=null?value:-1;
    }

    @Override
    public void stopSensor() {
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if (this.x == 0)
                this.x = x;
            if (this.y == 0)
                this.y = y;
            if (this.z == 0)
                this.z = z;

            double acc = (double)(x * x + y * y + z * z);

            if ( Math.sqrt(acc) > 11 || Math.sqrt(acc) < 8 ) {
                long curTime = System.currentTimeMillis();
                if ((curTime - lastUpdate) > 2000) {
                    value++;
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putInt("phone_movement", value);
                    editor.apply();

                    lastUpdate = curTime;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
