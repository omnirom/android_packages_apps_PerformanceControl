/*
 * Performance Control - An Android CPU Control application Copyright (C) 2012
 * James Roberts
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.brewcrewfoo.performance.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.fragments.VoltageControlSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Voltage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class BootService extends Service implements Constants {

    public static boolean servicesStarted = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
        }
        new BootWorker(this).execute();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class BootWorker extends AsyncTask<Void, Void, Void> {

        Context c;

        public BootWorker(Context c) {
            this.c = c;
        }

        @SuppressWarnings("deprecation")
        @Override
        protected Void doInBackground(Void... args) {
        	
	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(c);
	final StringBuilder sb = new StringBuilder();
	
	if (preferences.getBoolean(CPU_SOB, false)) {
		final String max = preferences.getString(PREF_MAX_CPU, null);
		final String min = preferences.getString(PREF_MIN_CPU, null);
		final String gov = preferences.getString(PREF_GOV, null);
		final String io = preferences.getString(PREF_IO, null);

		boolean mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();

		for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
			if (max != null) {
				sb.append("busybox echo " + max + " > " + MAX_FREQ_PATH.replace("cpu0", "cpu" + i) + " \n");
			}
			if (min != null) {
				sb.append("busybox echo " + min + " > " + MIN_FREQ_PATH.replace("cpu0", "cpu" + i) + " \n");
			}
			if (gov != null) {
				sb.append("busybox echo " + gov + " > " + GOVERNOR_PATH.replace("cpu0", "cpu" + i) + " \n");
			}
		}

		if (mIsTegra3 && max != null) {
			sb.append("busybox echo " + max + " > " + TEGRA_MAX_FREQ_PATH + " \n");
		}

		if (io != null) {
			String f = IO_SCHEDULER_PATH;
			for (int i = 0; i < Helpers.getNmmcblk(); i++) {
				sb.append("busybox echo " + io + " > " + f.replace("mmcblk0","mmcblk"+i) + " \n");
			}
		}
        }

	if (preferences.getBoolean(VOLTAGE_SOB, false)) {
		if(Helpers.voltageFileExists()){
                final List<Voltage> volts = VoltageControlSettings.getVolts(preferences);
                
    			if (Helpers.getVoltagePath() == VDD_PATH) {
				for (final Voltage volt : volts) {
					if(volt.getSavedMV() != volt.getCurrentMv()){
						for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
							sb.append("busybox echo "
								+ volt.getFreq()+" "+volt.getSavedMV() + " > "
								+ Helpers.getVoltagePath().replace("cpu0","cpu" + i) + " \n");
						}
					}
				}
			}
			else{
				//other formats
				final StringBuilder b = new StringBuilder();
				for (final Voltage volt : volts) {
					b.append(volt.getSavedMV() + " ");
				}
				for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
					sb.append("busybox echo "
					+ b.toString() + " > "
					+ Helpers.getVoltagePath().replace("cpu0","cpu" + i) + " \n");				
				}
			}
		}
	}			


	if(preferences.getBoolean(PREF_FASTCHARGE, false)){
		if (new File(FASTCHARGE_PATH).exists()) {
			new CMDProcessor().su.runWaitFor("busybox echo 1 > " + FASTCHARGE_PATH);
			Intent i = new Intent();
			i.setAction(INTENT_ACTION_FASTCHARGE);
			c.sendBroadcast(i);
			// add notification to warn user they can only charge
			CharSequence contentTitle = c.getText(R.string.fast_charge_notification_title);
			CharSequence contentText = c.getText(R.string.fast_charge_notification_message);

			Notification n = new Notification.Builder(c)
				.setAutoCancel(true).setContentTitle(contentTitle)
				.setContentText(contentText)
				.setSmallIcon(R.drawable.ic_launcher)
				.setWhen(System.currentTimeMillis()).getNotification();

			NotificationManager nm = (NotificationManager) getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
			nm.notify(1337, n);
		}
	}


	if (preferences.getBoolean(BLX_SOB, false)) {
		if (new File(BLX_PATH).exists()) {
			sb.append("busybox echo " + preferences.getInt(PREF_BLX, Integer.parseInt(Helpers.readOneLine(BLX_PATH)))
			+ " > " + BLX_PATH + " \n");
		}
	}

	if (preferences.getBoolean(PREF_MINFREE_BOOT, false)) {
		final String values = preferences.getString(PREF_MINFREE, null);
		if (!values.equals(null)) {
			sb.append("busybox echo " + values + " > " + MINFREE_PATH + " \n");				
		}
	}

	if (preferences.getBoolean(PREF_READ_AHEAD_BOOT, false)) {
		final String values = preferences.getString(PREF_READ_AHEAD,null);
		if (!values.equals(null)) {
			sb.append("busybox echo " + values + " > " + READ_AHEAD_PATH + " \n");
		}
	}

	if (preferences.getBoolean(VM_SOB, false)) {
		sb.append("busybox echo " + preferences.getInt(PREF_DIRTY_RATIO,Integer.parseInt(Helpers.readOneLine(DIRTY_RATIO_PATH)))
			+ " > " + DIRTY_RATIO_PATH + " \n");
		sb.append("busybox echo " + preferences.getInt(PREF_DIRTY_BACKGROUND, Integer.parseInt(Helpers.readOneLine(DIRTY_BACKGROUND_PATH)))
			+ " > " + DIRTY_BACKGROUND_PATH + " \n");
		sb.append("busybox echo " + preferences.getInt(PREF_DIRTY_EXPIRE, Integer.parseInt(Helpers.readOneLine(DIRTY_EXPIRE_PATH)))
			+ " > " + DIRTY_EXPIRE_PATH + " \n");
		sb.append("busybox echo " + preferences.getInt(PREF_DIRTY_WRITEBACK, Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_PATH)))
			+ " > " + DIRTY_WRITEBACK_PATH + " \n");
		sb.append("busybox echo " + preferences.getInt(PREF_MIN_FREE_KB, Integer.parseInt(Helpers.readOneLine(MIN_FREE_PATH)))
			+ " > " + MIN_FREE_PATH + " \n");
		sb.append("busybox echo " + preferences.getInt(PREF_OVERCOMMIT, Integer.parseInt(Helpers.readOneLine(OVERCOMMIT_PATH)))
			+ " > " + OVERCOMMIT_PATH + " \n");
		sb.append("busybox echo " + preferences.getInt(PREF_SWAPPINESS, Integer.parseInt(Helpers.readOneLine(SWAPPINESS_PATH)))
			+ " > " + SWAPPINESS_PATH + " \n");
		sb.append("busybox echo " + preferences.getInt(PREF_VFS, Integer.parseInt(Helpers.readOneLine(VFS_CACHE_PRESSURE_PATH)))
			+ " > " + VFS_CACHE_PRESSURE_PATH + " \n");
	}
	
	Helpers.shExec(sb);
	
	return null;
        }

    	@Override
    	protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            servicesStarted = true;
            stopSelf();
        }
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

