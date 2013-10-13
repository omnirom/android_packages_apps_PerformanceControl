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

package com.brewcrewfoo.performance.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.fragments.*;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;


public class MainActivity extends Fragment implements Constants,ActivityThemeChangeInterface {

    SharedPreferences mPreferences;
    PagerTabStrip mPagerTabStrip;
    ViewPager mViewPager;
    ViewGroup mRootView;

    private static boolean mVoltageExists;
    private boolean mIsLightTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setTheme();
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_main, container, false);
        mVoltageExists = Helpers.voltageFileExists();

        mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        TitleAdapter titleAdapter = new TitleAdapter(getFragmentManager());
        mViewPager.setAdapter(titleAdapter);
        mViewPager.setCurrentItem(0);

        mPagerTabStrip = (PagerTabStrip) rootView.findViewById(R.id.pagerTabStrip);
        //mPagerTabStrip.setBackgroundColor(getResources().getColor(R.color.pc_light_gray));
        mPagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.pc_blue));
        mPagerTabStrip.setDrawFullUnderline(false);

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) rootView.getLayoutParams()).removeBorders = true;
        }

        checkForSu();

        return rootView;
    }

    class TitleAdapter extends FragmentPagerAdapter {
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public TitleAdapter(FragmentManager fm) {
            super(fm);
            if (mVoltageExists) {
            	if(Helpers.showBattery()){
	                frags[0] = new CPUSettings();
		            frags[1] = new BatteryInfo();
		            frags[2] = new OOMSettings();
                    frags[3] = new VM();
	                frags[4] = new VoltageControlSettings();
	                frags[5] = new Advanced();
	                frags[6] = new TimeInState();
	                frags[7] = new CPUInfo();
                    frags[8] = new DiskInfo();
                    if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        frags[9] = new Tools();
                    }
            	}
            	else{
			        frags[0] = new CPUSettings();
	        	    frags[1] = new OOMSettings();
                    frags[2] = new VM();
                	frags[3] = new VoltageControlSettings();
                	frags[4] = new Advanced();
                	frags[5] = new TimeInState();
                	frags[6] = new CPUInfo();
                    frags[7] = new DiskInfo();
                    if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        frags[8] = new Tools();
                    }
            	}
            } 
            else {
                if(Helpers.showBattery()){
                    frags[0] = new CPUSettings();
                    frags[1] = new BatteryInfo();
                    frags[2] = new OOMSettings();
                    frags[3] = new VM();
                    frags[4] = new Advanced();
                    frags[5] = new TimeInState();
                    frags[6] = new CPUInfo();
                    frags[7] = new DiskInfo();
                    if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        frags[8] = new Tools();
                    }
                }
                else{
                    frags[0] = new CPUSettings();
                    frags[1] = new OOMSettings();
                    frags[2] = new VM();
                    frags[3] = new Advanced();
                    frags[4] = new TimeInState();
                    frags[5] = new CPUInfo();
                    frags[6] = new DiskInfo();
                    if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        frags[7] = new Tools();
                    }
                }
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //if (isThemeChanged()) {
        //    Helpers.restartPC(this);
        //}
    }

    /**
     * Check if root access, and prompt the user to grant PC access
     */
    private void checkForSu() {
        if (Helpers.isSystemApp(getActivity())) {
            return;
        }

        // If this is the first launch of the application. Check for root.
        boolean firstrun = mPreferences.getBoolean("firstrun", true);
        // Continue to bug the user that options will not work.
        boolean rootWasCanceled = mPreferences.getBoolean("rootcanceled", false);

        // Don't bother AOKP users ;)
        PackageManager pm = getActivity().getPackageManager();
        boolean rcInstalled = false;
        try {
            pm.getPackageInfo("com.aokp.romcontrol",PackageManager.GET_ACTIVITIES);
            rcInstalled = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            rcInstalled = false;
        }

        // Now that we've decided what to do. Launch the appropriate dialog
        if (firstrun || rootWasCanceled) {
            SharedPreferences.Editor e = mPreferences.edit();
            e.putBoolean("firstrun", false);
            e.commit();
            if (rcInstalled) {
                Helpers.checkSu();
            }
            else {
                launchFirstRunDialog();
            }
        }
    }

    /**
     * Alert the user that a check for root will be run
     */
    private void launchFirstRunDialog() {
        String title = getString(R.string.first_run_title);
        final String failedTitle = getString(R.string.su_failed_title);
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View firstRunDialog = factory.inflate(R.layout.su_dialog, null);
        TextView tv = (TextView) firstRunDialog.findViewById(R.id.message);
        tv.setText(R.string.first_run_message);
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(firstRunDialog)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,int which) {
                                String message = getString(R.string.su_cancel_message);
                                SharedPreferences.Editor e = mPreferences.edit();
                                e.putBoolean("rootcanceled", true);
                                e.commit();
                                suResultDialog(failedTitle, message);
                            }
                        })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean canSu = Helpers.checkSu();
                        boolean canBb = Helpers.checkBusybox();
                        if (canSu && canBb) {
                            String title = getString(R.string.su_success_title);
                            String message = getString(R.string.su_success_message);
                            SharedPreferences.Editor e = mPreferences.edit();
                            e.putBoolean("rootcanceled", false);
                            e.commit();
                            suResultDialog(title, message);
                        }
                        if (!canSu || !canBb) {
                            String message = getString(R.string.su_failed_su_or_busybox);
                            SharedPreferences.Editor e = mPreferences.edit();
                            e.putBoolean("rootcanceled", true);
                            e.commit();
                            suResultDialog(failedTitle, message);
                        }
                    }
                }).create().show();
    }

    /**
     * Display the result of the check for root access so the user knows what to
     * expect in respect to functionality of the application.
     *
     * @param title   Oops or OK depending on the result
     * @param message Success or fail message
     */
    private void suResultDialog(String title, String message) {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View suResultDialog = factory.inflate(R.layout.su_dialog, null);
        TextView tv = (TextView) suResultDialog.findViewById(R.id.message);
        tv.setText(message);
        new AlertDialog.Builder(getActivity()).setTitle(title).setView(suResultDialog)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create().show();
    }

    /**
     * Get a list of titles for the tabstrip to display depending on if the
     * voltage control fragment and battery fragment will be displayed. (Depends on the result of
     * Helpers.voltageTableExists() & Helpers.showBattery()
     *
     * @return String[] containing titles
     */
    private String[] getTitles() {
        String titleString[];
        if (mVoltageExists) {
        	if(Helpers.showBattery()){
                if (getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        titleString = new String[]{
                        getString(R.string.t_cpu_settings),
                        getString(R.string.t_battery_info),
                        getString(R.string.t_oom_settings),
                        getString(R.string.prefcat_vm_settings),
                        getString(R.string.t_volt_settings),
                        getString(R.string.t_adv_settings),
                        getString(R.string.t_time_in_state),
                        getString(R.string.t_cpu_info),
                        getString(R.string.t_disk_info)};
                } else {
                    titleString = new String[]{
                        getString(R.string.t_cpu_settings),
                        getString(R.string.t_battery_info),
                        getString(R.string.t_oom_settings),
                        getString(R.string.prefcat_vm_settings),
                        getString(R.string.t_volt_settings),
                        getString(R.string.t_adv_settings),
                        getString(R.string.t_time_in_state),
                        getString(R.string.t_cpu_info),
                        getString(R.string.t_disk_info),
                        getString(R.string.t_tools)};
                }
            }
            else{
                if (getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_volt_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info)};
                } else {
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_volt_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info),
                            getString(R.string.t_tools)};
                }
            }
        } 
        else {
        	if(Helpers.showBattery()){
                if (getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_battery_info),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info)};
                } else {
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_battery_info),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info),
                            getString(R.string.t_tools)};
                }
            }
        	else{
                if (getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info)};
                } else {
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info),
                            getString(R.string.t_tools)};
                }
            }
        }
        return titleString;
    }

    @Override
    public boolean isThemeChanged() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        return is_light_theme != mIsLightTheme;
    }

    @Override
    public void setTheme() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        mIsLightTheme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        //setTheme(is_light_theme ? R.style.Theme_Light : R.style.Theme_Dark);
    }
}

