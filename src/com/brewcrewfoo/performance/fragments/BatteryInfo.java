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

package com.brewcrewfoo.performance.fragments;

import android.app.AlertDialog;
import android.app.Fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;


import java.io.File;

public class BatteryInfo extends Fragment implements SeekBar.OnSeekBarChangeListener, Constants {
    private static final int NEW_MENU_ID=Menu.FIRST+1;
    TextView mbattery_percent;
    TextView mbattery_volt;
    TextView mbattery_status;
    TextView mBlxVal;
    Switch mFastchargeOnBoot;
    SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
  	    mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.battery_menu, menu);
        final SubMenu smenu = menu.addSubMenu(0, NEW_MENU_ID, 0,getString(R.string.menu_tab));
        final ViewPager mViewPager = (ViewPager) getView().getParent();
        for(int i=0;i< mViewPager.getAdapter().getCount();i++){
            if(i!=mViewPager.getCurrentItem())
            smenu.add(0, NEW_MENU_ID +i+1, 0, mViewPager.getAdapter().getPageTitle(i));
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.app_settings) {
            Intent intent = new Intent(getActivity(), PCSettings.class);
            startActivity(intent);
        }
        final ViewPager mViewPager = (ViewPager) getView().getParent();
        for(int i=0;i< mViewPager.getAdapter().getCount();i++){
            if(item.getItemId() == NEW_MENU_ID+i+1) {
                mViewPager.setCurrentItem(i);
            }
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.battery_info, root, false);

        mbattery_percent = (TextView) view.findViewById(R.id.batt_percent);
        mbattery_volt = (TextView) view.findViewById(R.id.batt_volt);
        mbattery_volt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    Intent powerUsageIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                    startActivity(powerUsageIntent);
                }
                catch(Exception e){
                }
            }
        });

        mbattery_status = (TextView) view.findViewById(R.id.batt_status);

        SeekBar mBlxSlider = (SeekBar) view.findViewById(R.id.blx_slider);
        if (new File(BLX_PATH).exists()) {
            mBlxSlider.setMax(100);

            mBlxVal = (TextView) view.findViewById(R.id.blx_val);
            mBlxVal.setText(getString(R.string.blx_title)+" " + Helpers.readOneLine(BLX_PATH)+"%");

            mBlxSlider.setProgress(Integer.parseInt(Helpers.readOneLine(BLX_PATH)));
            mBlxSlider.setOnSeekBarChangeListener(this);
            Switch mSetOnBoot = (Switch) view.findViewById(R.id.blx_sob);
            mSetOnBoot.setChecked(mPreferences.getBoolean(BLX_SOB, false));
            mSetOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton v, boolean checked) {
                    final SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putBoolean(BLX_SOB, checked);
                    if (checked) {
                        editor.putInt(PREF_BLX, Integer.parseInt(Helpers.readOneLine(BLX_PATH)));
                    }
                    editor.commit();
                }
            });
        }
        else{
            LinearLayout mpart = (LinearLayout) view.findViewById(R.id.blx_layout);
            mpart.setVisibility(LinearLayout.GONE);
        }

        if (new File(FASTCHARGE_PATH).exists()) {

            mFastchargeOnBoot = (Switch) view.findViewById(R.id.fastcharge_sob);
            mFastchargeOnBoot.setChecked(mPreferences.getBoolean(PREF_FASTCHARGE, false));
            mFastchargeOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton v,boolean checked) {
                    mPreferences.edit().putBoolean(PREF_FASTCHARGE,checked).apply();

                    if (checked){
                     String warningMessage = getString(R.string.fast_charge_warning);
                    //----------------
                    String cancel = getString(R.string.cancel);
                    String ok = getString(R.string.ok);
                    //-----------------
                    new AlertDialog.Builder(getActivity())
                            .setMessage(warningMessage)
                            .setNegativeButton(cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,int which) {
                                            mPreferences.edit().putBoolean(PREF_FASTCHARGE,false).apply();
                                            mFastchargeOnBoot.setChecked(false);
                                        }
                                    })
                            .setPositiveButton(ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,int which) {
                                            new CMDProcessor().su.runWaitFor("busybox echo 1 > " + FASTCHARGE_PATH);
                                        }
                                    }).create().show();
                    }
                    else{
                        new CMDProcessor().su.runWaitFor("busybox echo 0 > " + FASTCHARGE_PATH);
                    }
                 }
            });
        }
         else{
            LinearLayout mpart = (LinearLayout) view.findViewById(R.id.fastcharge_layout);
            mpart.setVisibility(LinearLayout.GONE);
         }


        return view;
    }

   @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            mBlxVal.setText(getString(R.string.blx_title)+" " + progress + "%");
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // we have a break now, write the values..
        new CMDProcessor().su.runWaitFor("busybox echo " + seekBar.getProgress() + " > " + BLX_PATH);
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(PREF_BLX, seekBar.getProgress()).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

/*
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {

        if (key.equals("battery_level")||key.equals("battery_status")||key.equals("battery_temp")||key.equals("battery_volt")) {
            int level=sharedPreferences.getInt("battery_level",0);
            int scale=sharedPreferences.getInt("battery_scale",0);
            level=level*scale/100;
            mbattery_percent.setText(level+"%");
            int voltage;
            int rawvoltage=mPreferences.getInt("battery_volt",0);
            if(rawvoltage<10){
                voltage=rawvoltage*1000;
            }
            else if(rawvoltage>5000){
                voltage=Math.round(rawvoltage/1000);
            }
            else{
                voltage=rawvoltage;
            }
            if (new File(BAT_VOLT_PATH).exists()){
                voltage=Integer.parseInt(Helpers.readOneLine(BAT_VOLT_PATH));
            }
            mbattery_volt.setText(voltage+" mV");

            int status=mPreferences.getInt("battery_status",0);
            int temperature=mPreferences.getInt("battery_temp",0);
            mbattery_status.setText((temperature/10)+"°C  "+getResources().getStringArray(R.array.batt_status)[status]);
        }
    }

*/

}
