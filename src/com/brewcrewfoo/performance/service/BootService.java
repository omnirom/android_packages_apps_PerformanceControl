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
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Voltage;


import java.io.File;
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
        final String FASTCHARGE_PATH=Helpers.fastcharge_path();
        final String BLN_PATH=Helpers.bln_path();
		
		if (preferences.getBoolean(CPU_SOB, false)) {
			final String max = preferences.getString(PREF_MAX_CPU, Helpers.readOneLine(MAX_FREQ_PATH));
			final String min = preferences.getString(PREF_MIN_CPU, Helpers.readOneLine(MIN_FREQ_PATH));
			final String gov = preferences.getString(PREF_GOV, Helpers.readOneLine(GOVERNOR_PATH));
			final String io = preferences.getString(PREF_IO, Helpers.getIOScheduler());

			for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
				sb.append("busybox echo ").append(max).append(" > ").append(MAX_FREQ_PATH.replace("cpu0", "cpu" + i)).append(" \n");
				sb.append("busybox echo ").append(min).append(" > ").append(MIN_FREQ_PATH.replace("cpu0", "cpu" + i)).append(" \n");
				sb.append("busybox echo ").append(gov).append(" > ").append(GOVERNOR_PATH.replace("cpu0", "cpu" + i)).append(" \n");
			}
			if (new File(TEGRA_MAX_FREQ_PATH).exists()) {
				sb.append("busybox echo ").append(max).append(" > ").append(TEGRA_MAX_FREQ_PATH).append(" \n");
			}
            if(new File(DYN_FREQ_PATH).exists()){
                sb.append("busybox echo ").append(max).append(" > ").append(DYN_FREQ_PATH).append(" \n");
            }
			for(int i=0;i<IO_SCHEDULER_PATH.length; i++){
                if (new File(IO_SCHEDULER_PATH[i]).exists())
                    sb.append("busybox echo ").append(io).append(" > ").append(IO_SCHEDULER_PATH[i]).append(" \n");
			}
		}

		if (preferences.getBoolean(VOLTAGE_SOB, false)) {
			if(Helpers.voltageFileExists()){
				final List<Voltage> volts = VoltageControlSettings.getVolts(preferences);
				if (Helpers.getVoltagePath().equals(VDD_PATH)) {
					for (final Voltage volt : volts) {
						if(!volt.getSavedMV().equals(volt.getCurrentMv())){
							for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
								sb.append("busybox echo ").append(volt.getFreq()).append(" ").append(volt.getSavedMV()).append(" > ").append(Helpers.getVoltagePath().replace("cpu0", "cpu" + i)).append(" \n");
							}
						}
					}
				}
				else{
					//other formats
					final StringBuilder b = new StringBuilder();
					for (final Voltage volt : volts) {
						b.append(volt.getSavedMV()).append(" ");
					}
					for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
						sb.append("busybox echo ").append(b.toString()).append(" > ").append(Helpers.getVoltagePath().replace("cpu0", "cpu" + i)).append(" \n");
					}
				}
			}
		}

        if (preferences.getBoolean(PREF_READ_AHEAD_BOOT, false)) {
            final String values = preferences.getString(PREF_READ_AHEAD,Helpers.readOneLine(READ_AHEAD_PATH[0]));
            for(int i=0; i<READ_AHEAD_PATH.length; i++){
                if (new File(READ_AHEAD_PATH[i]).exists())
                sb.append("busybox echo ").append(values).append(" > ").append(READ_AHEAD_PATH[i]).append(" \n");
            }
        }

		if (FASTCHARGE_PATH!=null) {
			if(preferences.getBoolean(PREF_FASTCHARGE, false)){
				sb.append("busybox echo 1 > ").append(FASTCHARGE_PATH).append(" \n");
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
				sb.append("busybox echo ").append(preferences.getInt(PREF_BLX, Integer.parseInt(Helpers.readOneLine(BLX_PATH)))).append(" > ").append(BLX_PATH).append(" \n");
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
				sb.append("busybox echo ").append(preferences.getInt(PREF_BLTIMEOUT, Integer.parseInt(Helpers.readOneLine(BL_TIMEOUT_PATH)))).append(" > ").append(BL_TIMEOUT_PATH).append(" \n");
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
        if (BLN_PATH!=null) {
            if (preferences.getBoolean(PREF_BLN, false)) {
                sb.append("busybox echo 1 > " + BLN_PATH + " \n");
            }
            else{
                sb.append("busybox echo 0 > " + BLN_PATH + " \n");
            }
        }
		if (new File(PFK_HOME_ENABLED).exists() && new File(PFK_MENUBACK_ENABLED).exists()) {
			if (preferences.getBoolean(PFK_SOB, false)) {
				sb.append("busybox echo ").append(preferences.getInt(PREF_HOME_ALLOWED_IRQ, Integer.parseInt(Helpers.readOneLine(PFK_HOME_ALLOWED_IRQ)))).append(" > ").append(PFK_HOME_ALLOWED_IRQ).append(" \n");
				sb.append("busybox echo ").append(preferences.getInt(PREF_HOME_REPORT_WAIT, Integer.parseInt(Helpers.readOneLine(PFK_HOME_REPORT_WAIT)))).append(" > ").append(PFK_HOME_REPORT_WAIT).append(" \n");
				sb.append("busybox echo ").append(preferences.getInt(PREF_MENUBACK_INTERRUPT_CHECKS, Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_INTERRUPT_CHECKS)))).append(" > ").append(PFK_MENUBACK_INTERRUPT_CHECKS).append(" \n");
				sb.append("busybox echo ").append(preferences.getInt(PREF_MENUBACK_FIRST_ERR_WAIT, Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT)))).append(" > ").append(PFK_MENUBACK_FIRST_ERR_WAIT).append(" \n");
				sb.append("busybox echo ").append(preferences.getInt(PREF_MENUBACK_LAST_ERR_WAIT, Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT)))).append(" > ").append(PFK_MENUBACK_LAST_ERR_WAIT).append(" \n");
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
				sb.append("busybox echo ").append(preferences.getInt(PREF_DIRTY_WRITEBACK_ACTIVE, Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_ACTIVE_PATH)))).append(" > ").append(DIRTY_WRITEBACK_ACTIVE_PATH).append(" \n");
				sb.append("busybox echo ").append(preferences.getInt(PREF_DIRTY_WRITEBACK_SUSPEND, Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_SUSPEND_PATH)))).append(" > ").append(DIRTY_WRITEBACK_SUSPEND_PATH).append(" \n");
			}
		}			
		
		if (preferences.getBoolean(VM_SOB, false)) {
			sb.append("busybox echo ").append(preferences.getInt(PREF_DIRTY_RATIO, Integer.parseInt(Helpers.readOneLine(DIRTY_RATIO_PATH)))).append(" > ").append(DIRTY_RATIO_PATH).append(" \n");
			sb.append("busybox echo ").append(preferences.getInt(PREF_DIRTY_BACKGROUND, Integer.parseInt(Helpers.readOneLine(DIRTY_BACKGROUND_PATH)))).append(" > ").append(DIRTY_BACKGROUND_PATH).append(" \n");
			sb.append("busybox echo ").append(preferences.getInt(PREF_DIRTY_EXPIRE, Integer.parseInt(Helpers.readOneLine(DIRTY_EXPIRE_PATH)))).append(" > ").append(DIRTY_EXPIRE_PATH).append(" \n");
			if(!isdynamic){
			sb.append("busybox echo ").append(preferences.getInt(PREF_DIRTY_WRITEBACK, Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_PATH)))).append(" > ").append(DIRTY_WRITEBACK_PATH).append(" \n");
			}
			sb.append("busybox echo ").append(preferences.getInt(PREF_MIN_FREE_KB, Integer.parseInt(Helpers.readOneLine(MIN_FREE_PATH)))).append(" > ").append(MIN_FREE_PATH).append(" \n");
			sb.append("busybox echo ").append(preferences.getInt(PREF_OVERCOMMIT, Integer.parseInt(Helpers.readOneLine(OVERCOMMIT_PATH)))).append(" > ").append(OVERCOMMIT_PATH).append(" \n");
			sb.append("busybox echo ").append(preferences.getInt(PREF_SWAPPINESS, Integer.parseInt(Helpers.readOneLine(SWAPPINESS_PATH)))).append(" > ").append(SWAPPINESS_PATH).append(" \n");
			sb.append("busybox echo ").append(preferences.getInt(PREF_VFS, Integer.parseInt(Helpers.readOneLine(VFS_CACHE_PRESSURE_PATH)))).append(" > ").append(VFS_CACHE_PRESSURE_PATH).append(" \n");
		}
        if (preferences.getBoolean(PREF_MINFREE_BOOT, false)) {
                sb.append("busybox echo ").append(preferences.getString(PREF_MINFREE, Helpers.readOneLine(MINFREE_PATH))).append(" > ").append(MINFREE_PATH).append(" \n");
        }
        if (new File(USER_PROC_PATH).exists()) {
                if (preferences.getBoolean(USER_PROC_SOB, false)) {
                    if (preferences.getBoolean(PREF_USER_PROC, false)) {
                        sb.append("busybox echo 1 > " + USER_PROC_PATH + " \n");
                    }
                    else{
                        sb.append("busybox echo 0 > " + USER_PROC_PATH + " \n");
                    }
                    sb.append("busybox echo ").append(preferences.getString(PREF_USER_NAMES, Helpers.readOneLine(USER_PROC_NAMES_PATH))).append(" > ").append(USER_PROC_NAMES_PATH).append(" \n");
                }
        }
        if (new File(SYS_PROC_PATH).exists()) {
                if (preferences.getBoolean(SYS_PROC_SOB, false)) {
                    if (preferences.getBoolean(PREF_SYS_PROC, false)) {
                        sb.append("busybox echo 1 > " + SYS_PROC_PATH + " \n");
                    }
                    else{
                        sb.append("busybox echo 0 > " + SYS_PROC_PATH + " \n");
                    }
                    sb.append("busybox echo ").append(preferences.getString(PREF_SYS_NAMES, Helpers.readOneLine(USER_SYS_NAMES_PATH))).append(" > ").append(USER_SYS_NAMES_PATH).append(" \n");
                }
        }
        if (new File(KSM_RUN_PATH).exists()) {
            if (preferences.getBoolean(KSM_SOB, false)) {
                if (preferences.getBoolean(PREF_RUN_KSM, false)) {
                    sb.append("busybox echo 1 > " + KSM_RUN_PATH + " \n");
                }
                else{
                    sb.append("busybox echo 0 > " + KSM_RUN_PATH + " \n");
                }
                sb.append("busybox echo ").append(preferences.getString("pref_ksm_pagetoscan", Helpers.readOneLine(KSM_PAGESTOSCAN_PATH))).append(" > ").append(KSM_PAGESTOSCAN_PATH).append(" \n");
                sb.append("busybox echo ").append(preferences.getString("pref_ksm_sleep", Helpers.readOneLine(KSM_SLEEP_PATH))).append(" > ").append(KSM_SLEEP_PATH).append(" \n");
            }
        }
        sb.append(preferences.getString(PREF_SH,"# no custom shell command")+" \n");

		Helpers.shExec(sb);
		return null;
        }
    	@Override
    	protected void onPostExecute(Void result) {
            Helpers.updateAppWidget(c);
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
