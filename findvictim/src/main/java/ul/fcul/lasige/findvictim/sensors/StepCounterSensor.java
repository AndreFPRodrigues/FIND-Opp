package ul.fcul.lasige.findvictim.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

/**
 * Created by afonso on 27-03-2016.
 * Sensor to detect the steps of the user
 */
public class StepCounterSensor extends AbstractSensor implements SensorEventListener {

    private SharedPreferences sp;

    private SensorManager sm;
    private Integer value;
    /**
     * Creates a new sensor
     *
     * @param c Android mContext from which it is possible to obtain sensors.
     */
    public StepCounterSensor(Context c) {
        super(c);
        sp = c.getSharedPreferences("victimSharedPreferencesFile", Context.MODE_PRIVATE);
        sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void startSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_NORMAL);
        }
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
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            value = (int) event.values[0];
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("step_counter", value);
            editor.apply();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
