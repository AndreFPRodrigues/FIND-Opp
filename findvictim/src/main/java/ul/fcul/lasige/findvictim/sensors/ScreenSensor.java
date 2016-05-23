package ul.fcul.lasige.findvictim.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Sensor to obtain device's battery level
 * 
 * Created by Ana on 08/03/2016.
 */
public class ScreenSensor extends AbstractSensor {
	private static final String TAG = ScreenSensor.class.getSimpleName();

	private SharedPreferences sp;

	private Integer screenOn;

	private BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context arg0, Intent intent) {
	    	screenOn += 1;
			//Log.d(TAG, "Battery level: " + mBatteryLevel);
			SharedPreferences.Editor editor = sp.edit();
			editor.putInt("screen_on", screenOn);
			editor.apply();
	    }
	};

	/**
	 * Creates a new Screen sensor to receive updates
	 * @param c Android context
	 */
	public ScreenSensor(Context c) {
		super(c);
		sp = c.getSharedPreferences("victimSharedPreferencesFile", Context.MODE_PRIVATE);
		screenOn = 0;
	}
	
	@Override
	public void startSensor() {
        Log.d(TAG, "Starting screen sensor");
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		mContext.registerReceiver(mScreenOnReceiver, ifilter);
	}

	@Override
	public Object getCurrentValue() {
		return screenOn!=null?screenOn:-1;
	}

	@Override
	public void stopSensor() {
        Log.d(TAG, "Stoping screen sensor");
		mContext.unregisterReceiver(mScreenOnReceiver);
	}

}
