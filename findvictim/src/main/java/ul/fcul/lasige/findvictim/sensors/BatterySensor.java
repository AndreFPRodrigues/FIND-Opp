package ul.fcul.lasige.findvictim.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Sensor to obtain device's battery level
 *
 * @author André Silva <asilva@lasige.di.fc.ul.pt>
 */
public class BatterySensor extends AbstractSensor {
	private static final String TAG = BatterySensor.class.getSimpleName();

	private SharedPreferences sp;

	private Integer mBatteryLevel;
	private Integer mBatteryTemp;

	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			mBatteryLevel = level / (scale / 100);
			mBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
			//Log.d(TAG, "Battery level: " + mBatteryLevel);
			SharedPreferences.Editor editor = sp.edit();
			editor.putInt("battery_level", mBatteryLevel);
			editor.putInt("battery_temp", mBatteryTemp);
			editor.apply();
		}
	};

	/**
	 * Creates a new Battery sensor to receive battery level updates
	 * @param c Android context
	 */
	public BatterySensor(Context c) {
		super(c);
		sp = c.getSharedPreferences("victimSharedPreferencesFile", Context.MODE_PRIVATE);
		mBatteryLevel = -1;
		mBatteryTemp = -1;
	}

	@Override
	public void startSensor() {
		Log.d(TAG, "Starting battery sensor");
		IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		mContext.registerReceiver(mBatInfoReceiver, iFilter);
	}

	@Override
	public Object getCurrentValue() {
		return new Integer[]{mBatteryLevel, mBatteryTemp};
	}

	@Override
	public void stopSensor() {
		Log.d(TAG, "Stopping battery sensor");
		mContext.unregisterReceiver(mBatInfoReceiver);
	}

}