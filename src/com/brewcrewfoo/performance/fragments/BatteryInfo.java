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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
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
    ImageView mBattIcon;
    Switch mFastchargeOnBoot;
    SharedPreferences mPreferences;
    private String mFastChargePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
  	    mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setRetainInstance(true);
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.battery_menu, menu);
        Helpers.addItems2Menu(menu,NEW_MENU_ID,getString(R.string.menu_tab),(ViewPager) getView().getParent());
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Helpers.removeCurItem(item,NEW_MENU_ID,(ViewPager) getView().getParent());
        switch(item.getItemId()){
                case R.id.app_settings:
                Intent intent = new Intent(getActivity(), PCSettings.class);
                startActivity(intent);
            break;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.battery_info, root, false);

        mbattery_percent = (TextView) view.findViewById(R.id.batt_percent);
        mbattery_volt = (TextView) view.findViewById(R.id.batt_volt);
        mbattery_status = (TextView) view.findViewById(R.id.batt_status);
        mBattIcon=(ImageView) view.findViewById(R.id.batt_icon);

        if (new File(BAT_VOLT_PATH).exists()){
                int volt=Integer.parseInt(Helpers.readOneLine(BAT_VOLT_PATH));
                if(volt>5000) volt = (int) Math.round(volt / 1000.0);// in microvolts
                mbattery_volt.setText(volt+" mV");
                mBattIcon.setVisibility(ImageView.GONE);
                mbattery_volt.setVisibility(TextView.VISIBLE);
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
                mbattery_volt.setOnLongClickListener(new View.OnLongClickListener(){
                    @Override
                    public boolean onLongClick(View view) {
                        mBattIcon.setVisibility(ImageView.VISIBLE);
                        mbattery_volt.setVisibility(TextView.GONE);
                        return true;
                    }
                });
        }
        else{
            mBattIcon.setVisibility(ImageView.VISIBLE);
            mbattery_volt.setVisibility(TextView.GONE);
            mBattIcon.setOnClickListener(new View.OnClickListener() {
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
        }

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
        mFastChargePath=Helpers.fastcharge_path();
        if (mFastChargePath!=null) {

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
                                            new CMDProcessor().su.runWaitFor("busybox echo 1 > " + mFastChargePath);
                                        }
                                    }).create().show();
                    }
                    else{
                        new CMDProcessor().su.runWaitFor("busybox echo 0 > " + mFastChargePath);
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
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(this.batteryInfoReceiver);
    }
    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(this.batteryInfoReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED) );
    }
    private BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        private int voltage;
        @Override
        public void onReceive(Context context, Intent intent) {
            //int  health= intent.getIntExtra(BatteryManager.EXTRA_HEALTH,0);
            //String  technology= intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
            //int  plugged= intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
            //boolean  present= intent.getExtras().getBoolean(BatteryManager.EXTRA_PRESENT);
            int  scale= intent.getIntExtra(BatteryManager.EXTRA_SCALE,0);
            int  level= intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            int  status= intent.getIntExtra(BatteryManager.EXTRA_STATUS,0);
            int  temperature= intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
            int  rawvoltage= intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0);

            level=level*scale/100;
            mbattery_percent.setText(level+"%");

            switch ((int) Math.ceil(level / 20.0)){
                case 0:
                    mBattIcon.setImageResource(R.drawable.battery_0);
                    break;
                case 1:
                    mBattIcon.setImageResource(R.drawable.battery_1);
                    break;
                case 2:
                    mBattIcon.setImageResource(R.drawable.battery_2);
                    break;
                case 3:
                    mBattIcon.setImageResource(R.drawable.battery_3);
                    break;
                case 4:
                    mBattIcon.setImageResource(R.drawable.battery_4);
                    break;
                case 5:
                    mBattIcon.setImageResource(R.drawable.battery_5);
                    break;
            }
            mbattery_status.setText((temperature/10)+"°C  "+getResources().getStringArray(R.array.batt_status)[status]);

        }
    };

}
