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
			final String max = preferences.getString(PREF_MAX_CPU, Helpers.readOneLine(MAX_FREQ_PATH));
			final String min = preferences.getString(PREF_MIN_CPU, Helpers.readOneLine(MIN_FREQ_PATH));
			final String gov = preferences.getString(PREF_GOV, Helpers.readOneLine(GOVERNOR_PATH));
			final String io = preferences.getString(PREF_IO, Helpers.getIOScheduler());

			boolean mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();

			for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
				sb.append("busybox echo " + max + " > " + MAX_FREQ_PATH.replace("cpu0", "cpu" + i) + " \n");
				sb.append("busybox echo " + min + " > " + MIN_FREQ_PATH.replace("cpu0", "cpu" + i) + " \n");
				sb.append("busybox echo " + gov + " > " + GOVERNOR_PATH.replace("cpu0", "cpu" + i) + " \n");
			}
			if (mIsTegra3) {
				sb.append("busybox echo " + max + " > " + TEGRA_MAX_FREQ_PATH + " \n");
			}
			for(int i=0;i<IO_SCHEDULER_PATH.length; i++){
				sb.append("busybox echo "+io+" > " + IO_SCHEDULER_PATH[i] + "\n");

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
		if (new File(FASTCHARGE_PATH).exists()) {
			if(preferences.getBoolean(PREF_FASTCHARGE, false)){
				sb.append("busybox echo 1 > " + FASTCHARGE_PATH + " \n");
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
		if (new File(BLX_PATH).exists()) {
			if (preferences.getBoolean(BLX_SOB, false)) {
				sb.append("busybox echo " + preferences.getString(PREF_BLX, ( Integer.parseIntHelpers.readOneLine(BLX_PATH)))
				+ " > " + BLX_PATH + " \n");
			}
		}
		if (new File(DSYNC_PATH).exists()) {
			if (preferences.getBoolean(PREF_DSYNC, false)) {
				sb.append("busybox echo 1 > " + DSYNC_PATH + " \n");
			}
			else{
				sb.append("busybox echo 0 > " + DSYNC_PATH + " \n");
			}
		}
		if (new File(BL_TIMEOUT_PATH).exists()) {
			if (preferences.getBoolean(BLTIMEOUT_SOB, false)) {
				sb.append("busybox echo " + preferences.getString(PREF_BLTIMEOUT,  Integer.parseInt(Helpers.readOneLine(BL_TIMEOUT_PATH)))
				+ " > " + BL_TIMEOUT_PATH + " \n");
			}
		}
		if (new File(BL_TOUCH_ON_PATH).exists()) {
			if (preferences.getBoolean(PREF_BLTOUCH, false)) {
				sb.append("busybox echo 1 > " + BL_TOUCH_ON_PATH + " \n");
			}
			else{
				sb.append("busybox echo 0 > " + BL_TOUCH_ON_PATH + " \n");
			}
		}	
		if (preferences.getBoolean(PREF_MINFREE_BOOT, false)) {
			sb.append("busybox echo " + preferences.getString(PREF_MINFREE, Helpers.readOneLine(MINFREE_PATH)) + " > " + MINFREE_PATH + " \n");
		}
		if (preferences.getBoolean(PREF_READ_AHEAD_BOOT, false)) {
			final String values = preferences.getString(PREF_READ_AHEAD,Helpers.readOneLine(READ_AHEAD_PATH[0]));
			for(int i=0; i<READ_AHEAD_PATH.length; i++){
				sb.append("busybox echo "+values+" > " + READ_AHEAD_PATH[i] + "\n");
			}
		}

		if (new File(PFK_HOME_ENABLED).exists() && new File(PFK_MENUBACK_ENABLED).exists()) {
			if (preferences.getBoolean(PFK_SOB, false)) {
				sb.append("busybox echo " + preferences.getString(PREF_HOME_ALLOWED_IRQ, Integer.parseInt(Helpers.readOneLine(PFK_HOME_ALLOWED_IRQ)))
				+ " > " + PFK_HOME_ALLOWED_IRQ + " \n");
				sb.append("busybox echo " + preferences.getString(PREF_HOME_REPORT_WAIT,Integer.parseInt(Helpers.readOneLine(PFK_HOME_REPORT_WAIT)))
				+ " > " + PFK_HOME_REPORT_WAIT + " \n");
				sb.append("busybox echo " + preferences.getString(PREF_MENUBACK_INTERRUPT_CHECKS,Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_INTERRUPT_CHECKS)))
				+ " > " + PFK_MENUBACK_INTERRUPT_CHECKS + " \n");
				sb.append("busybox echo " + preferences.getString(PREF_MENUBACK_FIRST_ERR_WAIT,Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT)))
				+ " > " + PFK_MENUBACK_FIRST_ERR_WAIT + " \n");
				sb.append("busybox echo " + preferences.getString(PREF_MENUBACK_LAST_ERR_WAIT,Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT)))
				+ " > " + PFK_MENUBACK_LAST_ERR_WAIT + " \n");
				if (preferences.getBoolean(PFK_HOME_ON, false)) {
					sb.append("busybox echo 1 > " + PFK_HOME_ENABLED + " \n");
				}
				else{
					sb.append("busybox echo 0 > " + PFK_HOME_ENABLED + " \n");
				}
				if (preferences.getBoolean(PFK_MENUBACK_ON, false)) {
					sb.append("busybox echo 1 > " + PFK_MENUBACK_ENABLED + " \n");
				}
				else{
					sb.append("busybox echo 0 > " + PFK_MENUBACK_ENABLED + " \n");
				}
			}
		}
		boolean isdynamic=false;
		if (new File(DYNAMIC_DIRTY_WRITEBACK_PATH).exists()) {
			if (preferences.getBoolean(DYNAMIC_DIRTY_WRITEBACK_SOB, false)) {
				if (preferences.getBoolean(PREF_DYNAMIC_DIRTY_WRITEBACK, false)) {
					sb.append("busybox echo 1 > " + DYNAMIC_DIRTY_WRITEBACK_PATH + " \n");
					isdynamic=true;
				}
				else{
					sb.append("busybox echo 0 > " + DYNAMIC_DIRTY_WRITEBACK_PATH + " \n");
				}
				sb.append("busybox echo " + preferences.getString(PREF_DIRTY_WRITEBACK_ACTIVE,Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_ACTIVE_PATH)))
				+ " > " + DIRTY_WRITEBACK_ACTIVE_PATH + " \n");
				sb.append("busybox echo " + preferences.getString(PREF_DIRTY_WRITEBACK_SUSPEND,Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_SUSPEND_PATH)))
				+ " > " + DIRTY_WRITEBACK_SUSPEND_PATH + " \n");
			}
		}			
		
		if (preferences.getBoolean(VM_SOB, false)) {
			sb.append("busybox echo " + preferences.getString(PREF_DIRTY_RATIO,Integer.parseInt(Helpers.readOneLine(DIRTY_RATIO_PATH)))
				+ " > " + DIRTY_RATIO_PATH + " \n");
			sb.append("busybox echo " + preferences.getString(PREF_DIRTY_BACKGROUND, Integer.parseInt(Helpers.readOneLine(DIRTY_BACKGROUND_PATH)))
				+ " > " + DIRTY_BACKGROUND_PATH + " \n");
			sb.append("busybox echo " + preferences.getString(PREF_DIRTY_EXPIRE, Integer.parseInt(Helpers.readOneLine(DIRTY_EXPIRE_PATH)))
				+ " > " + DIRTY_EXPIRE_PATH + " \n");
			if(!isdynamic){
			sb.append("busybox echo " + preferences.getString(PREF_DIRTY_WRITEBACK, Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_PATH)))
				+ " > " + DIRTY_WRITEBACK_PATH + " \n");
			}
			sb.append("busybox echo " + preferences.getString(PREF_MIN_FREE_KB, Integer.parseInt(Helpers.readOneLine(MIN_FREE_PATH)))
				+ " > " + MIN_FREE_PATH + " \n");
			sb.append("busybox echo " + preferences.getString(PREF_OVERCOMMIT, Integer.parseInt(Helpers.readOneLine(OVERCOMMIT_PATH)))
				+ " > " + OVERCOMMIT_PATH + " \n");
			sb.append("busybox echo " + preferences.getString(PREF_SWAPPINESS, Integer.parseInt(Helpers.readOneLine(SWAPPINESS_PATH)))
				+ " > " + SWAPPINESS_PATH + " \n");
			sb.append("busybox echo " + preferences.getString(PREF_VFS, Integer.parseInt(Helpers.readOneLine(VFS_CACHE_PRESSURE_PATH)))
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
