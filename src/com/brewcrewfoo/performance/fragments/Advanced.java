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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

public class Advanced extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener, Constants {

	private Preference mDirtyRatio;
	private Preference mDirtyBackground;
	private Preference mDirtyExpireCentisecs;
	private Preference mDirtyWriteback;
	private Preference mMinFreeK;
	private Preference mOvercommit;
	private Preference mSwappiness;
//--------
	private Preference mBlx;
	
	private CheckBoxPreference mDsync;
	
	private Preference mBltimeout;
	private CheckBoxPreference mBltouch;	
//--------
	private CheckBoxPreference mHomeOn;
	private CheckBoxPreference mMenuBackOn;
	
	private Preference mHomeAllowedIrqs;
	private Preference mHomeReportWait;

	private Preference mMenuBackIrqChecks;
	private Preference mMenuBackFirstErrWait;
	private Preference mMenuBackLastErrWait;	
//--------
	private Preference mVfs;
	private CheckBoxPreference mDynamicWriteBackOn;
	private Preference mDynamicWriteBackActive;
	private Preference mDynamicWriteBackSuspend;

	private ListPreference mReadAhead;
	private CheckBoxPreference mFastCharge;
	private SharedPreferences mPreferences;
	protected Context mContext;
	
	private int mSeekbarProgress;
	private EditText settingText;
	private String sminfree;
	private String sreadahead;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.layout.advanced);
        
	sreadahead=getResources().getString(R.string.ps_read_ahead,"");

        mReadAhead = (ListPreference) findPreference(PREF_READ_AHEAD);
	mFastCharge = (CheckBoxPreference) findPreference(PREF_FASTCHARGE);
	mBlx=(Preference) findPreference(PREF_BLX);
	mBltimeout=(Preference) findPreference(PREF_BLTIMEOUT);
	mBltouch=(CheckBoxPreference) findPreference(PREF_BLTOUCH);
	mDsync=(CheckBoxPreference) findPreference(PREF_DSYNC);
	mHomeOn=(CheckBoxPreference) findPreference(PFK_HOME_ON);
	mHomeAllowedIrqs = (Preference) findPreference(PREF_HOME_ALLOWED_IRQ);
	mHomeReportWait = (Preference) findPreference(PREF_HOME_REPORT_WAIT);
	mMenuBackOn= (CheckBoxPreference) findPreference(PFK_MENUBACK_ON);
	mMenuBackIrqChecks=(Preference) findPreference(PREF_MENUBACK_INTERRUPT_CHECKS);
	mMenuBackFirstErrWait=(Preference) findPreference(PREF_MENUBACK_FIRST_ERR_WAIT);
	mMenuBackLastErrWait=(Preference) findPreference(PREF_MENUBACK_LAST_ERR_WAIT);
        mDirtyRatio = (Preference) findPreference(PREF_DIRTY_RATIO);
        mDirtyBackground = (Preference) findPreference(PREF_DIRTY_BACKGROUND);
        mDirtyExpireCentisecs = (Preference) findPreference(PREF_DIRTY_EXPIRE);
        mDirtyWriteback = (Preference) findPreference(PREF_DIRTY_WRITEBACK);
        mMinFreeK = (Preference) findPreference(PREF_MIN_FREE_KB);
        mOvercommit = (Preference) findPreference(PREF_OVERCOMMIT);
        mSwappiness = (Preference) findPreference(PREF_SWAPPINESS);
        mVfs = (Preference) findPreference(PREF_VFS);
        mDynamicWriteBackOn = (CheckBoxPreference) findPreference(PREF_DYNAMIC_DIRTY_WRITEBACK);
        mDynamicWriteBackActive = (Preference) findPreference(PREF_DIRTY_WRITEBACK_ACTIVE);
        mDynamicWriteBackSuspend = (Preference) findPreference(PREF_DIRTY_WRITEBACK_SUSPEND);        
		
        if (!new File(FASTCHARGE_PATH).exists()) {
		PreferenceCategory hideCat = (PreferenceCategory) findPreference("kernel");
		getPreferenceScreen().removePreference(hideCat);
        }
	else{
		mFastCharge.setChecked(mPreferences.getBoolean(PREF_FASTCHARGE, false));
		if(Helpers.readOneLine(FASTCHARGE_PATH).equals("1")){
			mFastCharge.setSummary(getString(R.string.ps_fast_charge_active));
		}
		else{
			mFastCharge.setSummary(getString(R.string.ps_fast_charge_inactive));
		}
	}
        if (!new File(BLX_PATH).exists()) {
		PreferenceCategory hideCat = (PreferenceCategory) findPreference("blx");
		getPreferenceScreen().removePreference(hideCat);
        }
	else{
		mBlx.setSummary(Helpers.readOneLine(BLX_PATH)+"%");
	}
        if (!new File(DSYNC_PATH).exists()) {
		PreferenceCategory hideCat = (PreferenceCategory) findPreference("dsync");
		getPreferenceScreen().removePreference(hideCat);
        }
	else{
		mDsync.setChecked(Helpers.readOneLine(DSYNC_PATH).equals("1"));
	}
        if (!new File(PFK_HOME_ENABLED).exists() || !new File(PFK_MENUBACK_ENABLED).exists()) {
		PreferenceCategory hideCat = (PreferenceCategory) findPreference("pfk");
		getPreferenceScreen().removePreference(hideCat);
        }
	else{
		mHomeOn.setChecked(Helpers.readOneLine(PFK_HOME_ENABLED).equals("1"));
		mHomeOn.setSummary(getString(R.string.ps_home_enabled,Helpers.readOneLine(PFK_HOME_IGNORED_KP)));
		mHomeAllowedIrqs.setSummary(Helpers.readOneLine(PFK_HOME_ALLOWED_IRQ));
		mHomeReportWait.setSummary(Helpers.readOneLine(PFK_HOME_REPORT_WAIT) +" ms");
		
		mMenuBackOn.setChecked(Helpers.readOneLine(PFK_MENUBACK_ENABLED).equals("1"));
		mMenuBackOn.setSummary(getString(R.string.ps_menuback_enabled,Helpers.readOneLine(PFK_MENUBACK_IGNORED_KP)));
		mMenuBackIrqChecks.setSummary(Helpers.readOneLine(PFK_MENUBACK_INTERRUPT_CHECKS));
		mMenuBackFirstErrWait.setSummary(Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT)+" ms");
		mMenuBackLastErrWait.setSummary(Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT)+" ms");
	}
        if (!new File(BL_TIMEOUT_PATH).exists()) {
		PreferenceCategory hideCat = (PreferenceCategory) findPreference("bltimeout");
		getPreferenceScreen().removePreference(hideCat);
        }
	else{
		mBltimeout.setSummary(Helpers.readOneLine(BL_TIMEOUT_PATH)+" ms");
	}
        if (!new File(BL_TOUCH_ON_PATH).exists()) {
		PreferenceCategory hideCat = (PreferenceCategory) findPreference("bltouch");
		getPreferenceScreen().removePreference(hideCat);
        } 
	else{
		mBltouch.setChecked(Helpers.readOneLine(BL_TOUCH_ON_PATH).equals("1"));
	}
        if (!new File(DYNAMIC_DIRTY_WRITEBACK_PATH).exists()) {
		mDirtyWriteback.setEnabled(true);
		PreferenceCategory hideCat = (PreferenceCategory) findPreference("cat_dynamic_write_back");
		getPreferenceScreen().removePreference(hideCat);
        }
        else{
		mDirtyWriteback.setEnabled(!mPreferences.getBoolean(PREF_DYNAMIC_DIRTY_WRITEBACK, false));
		mDynamicWriteBackActive.setSummary(Helpers.readOneLine(DIRTY_WRITEBACK_ACTIVE_PATH));
        	mDynamicWriteBackSuspend.setSummary(Helpers.readOneLine(DIRTY_WRITEBACK_SUSPEND_PATH));  
        }	
		
	mReadAhead.setValue(Helpers.readOneLine(READ_AHEAD_PATH[0]));
        mReadAhead.setSummary(getString(R.string.ps_read_ahead, Helpers.readOneLine(READ_AHEAD_PATH[0]) + "  kb"));
        mDirtyRatio.setSummary(Helpers.readOneLine(DIRTY_RATIO_PATH));
        mDirtyBackground.setSummary(Helpers.readOneLine(DIRTY_BACKGROUND_PATH));
        mDirtyExpireCentisecs.setSummary(Helpers.readOneLine(DIRTY_EXPIRE_PATH));
        mDirtyWriteback.setSummary(Helpers.readOneLine(DIRTY_WRITEBACK_PATH));
        mMinFreeK.setSummary(Helpers.readOneLine(MIN_FREE_PATH));
        mOvercommit.setSummary(Helpers.readOneLine(OVERCOMMIT_PATH));
        mSwappiness.setSummary(Helpers.readOneLine(SWAPPINESS_PATH));
        mVfs.setSummary(Helpers.readOneLine(VFS_CACHE_PRESSURE_PATH));
            
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.advanced_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.app_settings) {
            Intent intent = new Intent(getActivity(), PCSettings.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (PREF_FASTCHARGE.equals(key)) {
            if (mPreferences.getBoolean(PREF_FASTCHARGE, false)) {
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
					mFastCharge.setChecked(false);
				}
			})
			.setPositiveButton(ok,
				new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,int which) {
					mPreferences.edit().putBoolean(PREF_FASTCHARGE,true).apply();
					mFastCharge.setChecked(true);
				}
			}).create().show();
                return true;
            }
        }
	else if (preference == mBlx){
            String title = getString(R.string.blx_title)+" (%)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(BLX_PATH));
            openDialog(currentProgress, title, 50,100, preference,BLX_PATH, PREF_BLX);
            return true;
	}
	else if (preference == mDsync){
		if (Integer.parseInt(Helpers.readOneLine(DSYNC_PATH))==0){
			new CMDProcessor().su.runWaitFor("busybox echo 1 > " + DSYNC_PATH);
		}
		else{
			new CMDProcessor().su.runWaitFor("busybox echo 0 > " + DSYNC_PATH);
		}
            return true;
	}
	else if (preference == mBltimeout){
            String title = getString(R.string.bltimeout_title)+" (ms)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(BL_TIMEOUT_PATH));
            openDialog(currentProgress, title, 0,5000, preference,BL_TIMEOUT_PATH, PREF_BLTIMEOUT);
            return true;
	}
	else if (preference == mBltouch){
		if (Integer.parseInt(Helpers.readOneLine(BL_TOUCH_ON_PATH))==0){
			new CMDProcessor().su.runWaitFor("busybox echo 1 > " + BL_TOUCH_ON_PATH);
		}
		else{
			new CMDProcessor().su.runWaitFor("busybox echo 0 > " + BL_TOUCH_ON_PATH);
		}
            return true;
	}	
	else if (preference == mHomeOn){
		if (Integer.parseInt(Helpers.readOneLine(PFK_HOME_ENABLED))==0){
			new CMDProcessor().su.runWaitFor("busybox echo 1 > " + PFK_HOME_ENABLED);
		}
		else{
			new CMDProcessor().su.runWaitFor("busybox echo 0 > " + PFK_HOME_ENABLED);
		}
            return true;
	}
	else if (preference == mMenuBackOn){
		if (Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_ENABLED))==0){
			new CMDProcessor().su.runWaitFor("busybox echo 1 > " + PFK_MENUBACK_ENABLED);
		}
		else{
			new CMDProcessor().su.runWaitFor("busybox echo 0 > " + PFK_MENUBACK_ENABLED);
		}
            return true;
	}
	else if (preference == mHomeAllowedIrqs) {
            String title = getString(R.string.home_allowed_irq_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_HOME_ALLOWED_IRQ));
            openDialog(currentProgress, title, 1,32, preference, PFK_HOME_ALLOWED_IRQ, PREF_HOME_ALLOWED_IRQ);
            return true;		
	}
	else if (preference == mHomeReportWait) {
            String title = getString(R.string.home_report_wait_title)+" (ms)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_HOME_REPORT_WAIT));
            openDialog(currentProgress, title, 5,25, preference, PFK_HOME_REPORT_WAIT, PREF_HOME_REPORT_WAIT);
            return true;
	}
	else if (preference == mMenuBackIrqChecks) {
            String title = getString(R.string.menuback_interrupt_checks_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_INTERRUPT_CHECKS));
            openDialog(currentProgress, title, 1,10, preference, PFK_MENUBACK_INTERRUPT_CHECKS, PREF_MENUBACK_INTERRUPT_CHECKS);
            return true;
	}
	else if (preference == mMenuBackFirstErrWait) {
            String title = getString(R.string.menuback_first_err_wait_title)+" (ms)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT));
            openDialog(currentProgress, title, 50,1000, preference, PFK_MENUBACK_FIRST_ERR_WAIT, PREF_MENUBACK_FIRST_ERR_WAIT);
            return true;
	}
	else if (preference == mMenuBackLastErrWait) {
            String title = getString(R.string.menuback_last_err_wait_title)+" (ms)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT));
            openDialog(currentProgress, title, 50,100, preference,PFK_MENUBACK_LAST_ERR_WAIT, PREF_MENUBACK_LAST_ERR_WAIT);
            return true;
	}		
	else if (preference == mDirtyRatio) {
            String title = getString(R.string.dirty_ratio_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_RATIO_PATH));
            openDialog(currentProgress, title, 0,100, preference,DIRTY_RATIO_PATH, PREF_DIRTY_RATIO);
            return true;
        }
	else if (preference == mDirtyBackground) {
            String title = getString(R.string.dirty_background_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_BACKGROUND_PATH));
            openDialog(currentProgress, title, 0,100, preference,DIRTY_BACKGROUND_PATH, PREF_DIRTY_BACKGROUND);
            return true;
        }
	else if (preference == mDirtyExpireCentisecs) {
            String title = getString(R.string.dirty_expire_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_EXPIRE_PATH));
            openDialog(currentProgress, title, 0,5000, preference,DIRTY_EXPIRE_PATH, PREF_DIRTY_EXPIRE);
            return true;
        }
	else if (preference == mDirtyWriteback) {
            String title = getString(R.string.dirty_writeback_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_PATH));
            openDialog(currentProgress, title, 0,5000, preference,DIRTY_WRITEBACK_PATH, PREF_DIRTY_WRITEBACK);
            return true;
        }
	else if (preference == mMinFreeK) {
            String title = getString(R.string.min_free_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(MIN_FREE_PATH));
            openDialog(currentProgress, title, 0,8192, preference, MIN_FREE_PATH,PREF_MIN_FREE_KB);
            return true;
        }
	else if (preference == mOvercommit) {
            String title = getString(R.string.overcommit_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(OVERCOMMIT_PATH));
            openDialog(currentProgress, title, 0,100, preference,OVERCOMMIT_PATH, PREF_OVERCOMMIT);
            return true;
        }
	else if (preference == mSwappiness) {
            String title = getString(R.string.swappiness_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(SWAPPINESS_PATH));
            openDialog(currentProgress, title, 0,100, preference,SWAPPINESS_PATH, PREF_SWAPPINESS);
            return true;
        }
	else if (preference == mVfs) {
            String title = getString(R.string.vfs_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(VFS_CACHE_PRESSURE_PATH));
            openDialog(currentProgress, title, 0,200, preference,VFS_CACHE_PRESSURE_PATH, PREF_VFS);
            return true;
        }
        /*
	else if (preference == mDynamicWriteBackActive) {
            String title = getString(R.string.vfs_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_ACTIVE_PATH));
            openDialog(currentProgress, title, 0,5000, preference,DIRTY_WRITEBACK_ACTIVE_PATH, PREF_DIRTY_WRITEBACK_ACTIVE);
            return true;
        }
        */
        
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
		if (key.equals(PREF_READ_AHEAD)) {
			String evalues = Helpers.readOneLine(READ_AHEAD_PATH[0]);
			String values = mPreferences.getString(key,evalues);
			if (!values.equals(evalues)){
				final StringBuilder sb = new StringBuilder();
				for(int i=0; i<READ_AHEAD_PATH.length; i++){
					sb.append("busybox echo "+values+" > " + READ_AHEAD_PATH[i] + "\n");
				}
				Helpers.shExec(sb);
			}
			mReadAhead.setSummary(sreadahead+values + " kb");
		}	
		else if (key.equals(PREF_BLX)) {
			mBlx.setSummary(Helpers.readOneLine(BLX_PATH)+"%");
		}
		else if (key.equals(PREF_BLTIMEOUT)) {
			mBltimeout.setSummary(Helpers.readOneLine(BL_TIMEOUT_PATH)+"ms");
		}
		else if (key.equals(PREF_HOME_REPORT_WAIT)){
			mHomeReportWait.setSummary(Helpers.readOneLine(PFK_HOME_REPORT_WAIT) +"ms");
		}
		else if (key.equals(PREF_MENUBACK_FIRST_ERR_WAIT)){
			mMenuBackFirstErrWait.setSummary(Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT)+"ms");
		}
		else if (key.equals(PREF_MENUBACK_LAST_ERR_WAIT)){
			mMenuBackLastErrWait.setSummary(Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT)+"ms");
		}
    }
	
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
    public void openDialog(int currentProgress, String title, final int min, final int max,
                           final Preference pref, final String path, final String key) {
        Resources res = getActivity().getResources();
        String cancel = res.getString(R.string.cancel);
        String ok = res.getString(R.string.ok);
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View alphaDialog = factory.inflate(R.layout.seekbar_dialog, null);

        final SeekBar seekbar = (SeekBar) alphaDialog.findViewById(R.id.seek_bar);

	seekbar.setMax(max);
	seekbar.setProgress(currentProgress);
        
        settingText = (EditText) alphaDialog.findViewById(R.id.setting_text);
        settingText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				int val = Integer.valueOf(settingText.getText().toString());
				seekbar.setProgress(val);
				return true;
			}
			return false;
		}
		});
	settingText.setText(Integer.toString(currentProgress));
        settingText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
			int val = Integer.parseInt(s.toString());
			if (val > max) {
				s.replace(0, s.length(), Integer.toString(max));
				val=max;
			}
			seekbar.setProgress(val);
                } catch (NumberFormatException ex) {
                }
            }
        });

        OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				mSeekbarProgress = seekbar.getProgress();
				if(fromUser){
					settingText.setText(Integer.toString(mSeekbarProgress));
				}
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }
        };
        seekbar.setOnSeekBarChangeListener(seekBarChangeListener);

        new AlertDialog.Builder(getActivity())
			.setTitle(title)
			.setView(alphaDialog)
			.setNegativeButton(cancel,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,int which) {
				// nothing
				}
			})
			.setPositiveButton(ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int val = Integer.valueOf(settingText.getText().toString());
					if(val<min){val=min;}
					seekbar.setProgress(val);
					int newProgress = seekbar.getProgress();
					pref.setSummary(Integer.toString(newProgress));
					new CMDProcessor().su.runWaitFor("busybox echo " + newProgress + " > " + path);
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putInt(key, newProgress);
					editor.commit();
				}
			}).create().show();
    }
}
