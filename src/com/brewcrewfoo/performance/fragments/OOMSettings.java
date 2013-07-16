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

public class OOMSettings extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener, Constants {


  private SharedPreferences mPreferences;
	protected Context mContext;
	
	private int mSeekbarProgress;
	private EditText settingText;

	private Preference mForegroundApp;
	private Preference mVisibleApp;
	private Preference mSecondaryServer;
	private Preference mHiddenApp;
	private Preference mContentProviders;
	private Preference mEmptyApp;
	
	private Preference mVerylight;
	private Preference mLight;
	private Preference mMedium;
	private Preference mAggressive;
	private Preference mVeryaggressive;
	
	private String values[];

    private CheckBoxPreference mUserON;
    private CheckBoxPreference mSysON;
    private Preference mUserNames;
    private Preference mSysNames;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.layout.oom_settings);
	
        values = Helpers.readOneLine(MINFREE_PATH).split(",");

        mForegroundApp=(Preference) findPreference(OOM_FOREGROUND_APP);
        mVisibleApp=(Preference) findPreference(OOM_VISIBLE_APP);
        mSecondaryServer=(Preference) findPreference(OOM_SECONDARY_SERVER);
        mHiddenApp=(Preference) findPreference(OOM_HIDDEN_APP);
        mContentProviders=(Preference) findPreference(OOM_CONTENT_PROVIDERS);
        mEmptyApp=(Preference) findPreference(OOM_EMPTY_APP);

        mVerylight=(Preference) findPreference("oom_verylight");
        mVerylight.setSummary( getString(R.string.pref_verylight));
        mLight=(Preference) findPreference("oom_light");
        mLight.setSummary( getString(R.string.pref_light));
        mMedium=(Preference) findPreference("oom_medium");
        mMedium.setSummary( getString(R.string.pref_medium));
        mAggressive=(Preference) findPreference("oom_aggressive");
        mAggressive.setSummary( getString(R.string.pref_aggressive));
        mVeryaggressive=(Preference) findPreference("oom_veryaggressive");
        mVeryaggressive.setSummary( getString(R.string.pref_veryaggressive));

        updateOOM(values);

        mUserON=(CheckBoxPreference) findPreference(PREF_USER_PROC);
        mSysON=(CheckBoxPreference) findPreference(PREF_SYS_PROC);
        mUserNames=(Preference) findPreference(PREF_USER_NAMES);
        mSysNames=(Preference) findPreference(PREF_SYS_NAMES);
        if (!new File(USER_PROC_PATH).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("notkill_user_proc");
            getPreferenceScreen().removePreference(hideCat);
        }
        else{
            mUserON.setChecked(Helpers.readOneLine(USER_PROC_PATH).equals("1"));
        }
        if (!new File(SYS_PROC_PATH).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("notkill_sys_proc");
            getPreferenceScreen().removePreference(hideCat);
        }
        else{
            mSysON.setChecked(Helpers.readOneLine(SYS_PROC_PATH).equals("1"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.oom_menu, menu);
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
		if (preference == mForegroundApp) {
			String title = getString(R.string.title_foreground_app)+" (mb)";
			int currentProgress = oomConv(values[0]);
			openDialog(0,currentProgress, title, 0,oomConv(values[1]), preference, MINFREE_PATH, PREF_MINFREE);
			return true;
		}
		else if (preference == mVisibleApp) {
			String title = getString(R.string.title_visible_app)+" (mb)";
			int currentProgress = oomConv(values[1]);
			openDialog(1,currentProgress, title, oomConv(values[0]),oomConv(values[2]), preference, MINFREE_PATH, PREF_MINFREE);
			return true;
		}
		else if (preference == mSecondaryServer) {
			String title = getString(R.string.title_secondary_server)+" (mb)";
			int currentProgress = oomConv(values[2]);
			openDialog(2,currentProgress, title, oomConv(values[1]),oomConv(values[3]), preference, MINFREE_PATH, PREF_MINFREE);
			return true;
		}
		else if (preference == mHiddenApp) {
			String title = getString(R.string.title_hidden_app)+" (mb)";
			int currentProgress = oomConv(values[3]);
			openDialog(3,currentProgress, title, oomConv(values[2]),oomConv(values[4]), preference, MINFREE_PATH, PREF_MINFREE);
			return true;
		}
		else if (preference == mContentProviders) {
			String title = getString(R.string.title_content_providers)+" (mb)";
			int currentProgress = oomConv(values[4]);
			openDialog(4,currentProgress, title, oomConv(values[3]),oomConv(values[5]), preference, MINFREE_PATH, PREF_MINFREE);
			return true;
		}
		else if (preference == mEmptyApp) {
			String title = getString(R.string.title_empty_app)+" (mb)";
			int currentProgress = oomConv(values[5]);
			openDialog(5,currentProgress, title, oomConv(values[4]),256, preference, MINFREE_PATH, PREF_MINFREE);
			return true;
		}
		else if (preference == mVerylight) {
			final String s = getString(R.string.pref_verylight);
			new CMDProcessor().su.runWaitFor("busybox echo " + s + " > " + MINFREE_PATH);
			mPreferences.edit().putString(PREF_MINFREE, s).apply();
			values = Helpers.readOneLine(MINFREE_PATH).split(",");		
			updateOOM(values);
			return true;
		}
		else if (preference == mLight) {
			final String s = getString(R.string.pref_light);
			new CMDProcessor().su.runWaitFor("busybox echo " + s + " > " + MINFREE_PATH);
			mPreferences.edit().putString(PREF_MINFREE, s).apply();
			values = Helpers.readOneLine(MINFREE_PATH).split(",");		
			updateOOM(values);
			return true;
		}
		else if (preference == mMedium) {
			final String s = getString(R.string.pref_medium);
			new CMDProcessor().su.runWaitFor("busybox echo " + s + " > " + MINFREE_PATH);
			mPreferences.edit().putString(PREF_MINFREE, s).apply();
			values = Helpers.readOneLine(MINFREE_PATH).split(",");		
			updateOOM(values);
			return true;
		}
		else if (preference == mAggressive) {
			final String s = getString(R.string.pref_aggressive);
			new CMDProcessor().su.runWaitFor("busybox echo " + s + " > " + MINFREE_PATH);
			mPreferences.edit().putString(PREF_MINFREE, s).apply();
			values = Helpers.readOneLine(MINFREE_PATH).split(",");		
			updateOOM(values);
			return true;
		}
		else if (preference == mVeryaggressive) {
			final String s = getString(R.string.pref_veryaggressive);
			new CMDProcessor().su.runWaitFor("busybox echo " + s + " > " + MINFREE_PATH);
			mPreferences.edit().putString(PREF_MINFREE, s).apply();
			values = Helpers.readOneLine(MINFREE_PATH).split(",");		
			updateOOM(values);
			return true;
		}
        else if (preference == mUserON){
            if (Integer.parseInt(Helpers.readOneLine(USER_PROC_PATH))==0){
                new CMDProcessor().su.runWaitFor("busybox echo 1 > " + USER_PROC_PATH);
            }
            else{
                new CMDProcessor().su.runWaitFor("busybox echo 0 > " + USER_PROC_PATH);
            }
            return true;
        }
        else if (preference == mSysON){
            if (Integer.parseInt(Helpers.readOneLine(SYS_PROC_PATH))==0){
                new CMDProcessor().su.runWaitFor("busybox echo 1 > " + SYS_PROC_PATH);
            }
            else{
                new CMDProcessor().su.runWaitFor("busybox echo 0 > " + SYS_PROC_PATH);
            }
            return true;
        }
        else if (preference == mUserNames){
            NamesEditDialog(key,getString(R.string.pt_user_names_proc));
        }
        else if (preference == mSysNames){
            NamesEditDialog(key,getString(R.string.pt_sys_names_proc));
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
    	if (key.equals(PREF_MINFREE_BOOT)) {
    		if(sharedPreferences.getBoolean(key,false)){
			    sharedPreferences.edit().putString(PREF_MINFREE, Helpers.readOneLine(MINFREE_PATH)).apply();
    		}
    		else{
    			sharedPreferences.edit().remove(PREF_MINFREE).apply();
    		}
	    }
        if (key.equals(USER_PROC_SOB)) {
            if(sharedPreferences.getBoolean(key,false)){
                //sharedPreferences.edit().putBoolean(PREF_USER_PROC, Helpers.readOneLine(USER_PROC_PATH).equals("1")).apply();
                sharedPreferences.edit().putString(PREF_USER_NAMES, Helpers.readOneLine(USER_PROC_NAMES_PATH)).apply();
            }
            else{
                //sharedPreferences.edit().remove(PREF_USER_PROC).apply();
                sharedPreferences.edit().remove(PREF_USER_NAMES).apply();
            }
        }
        if (key.equals(SYS_PROC_SOB)) {
            if(sharedPreferences.getBoolean(key,false)){
                //sharedPreferences.edit().putBoolean(PREF_SYS_PROC, Helpers.readOneLine(SYS_PROC_PATH).equals("1")).apply();
                sharedPreferences.edit().putString(PREF_SYS_NAMES, Helpers.readOneLine(USER_SYS_NAMES_PATH)).apply();
            }
            else{
                //sharedPreferences.edit().remove(PREF_SYS_PROC).apply();
                sharedPreferences.edit().remove(PREF_SYS_NAMES).apply();
            }
        }
    }

	private void updateOOM(String[] v) {
		mForegroundApp.setSummary(oomConv(values[0])+"mb "+"[ "+v[0]+" ]");
		mVisibleApp.setSummary(oomConv(values[1])+"mb "+"[ "+v[1]+" ]");
		mSecondaryServer.setSummary(oomConv(values[2])+"mb "+"[ "+v[2]+" ]");
		mHiddenApp.setSummary(oomConv(values[3])+"mb "+"[ "+v[3]+" ]");
		mContentProviders.setSummary(oomConv(values[4])+"mb "+"[ "+v[4]+" ]");
		mEmptyApp.setSummary(oomConv(values[5])+"mb "+"[ "+v[5]+" ]");	
	}
	
    private int oomConv(String s) {
        int v = 0;

        if (!s.equals(null) || !s.equals("")) {
            try {
                int mb = Integer.parseInt(s.trim()) * 4 / 1024;
                v = (int) Math.ceil(mb);
            } catch (NumberFormatException nfe) {
                Log.i(TAG, "error processing " + s);
            }
        }
        return v;
    }
	
	private static String implodeArray(String[] inputArray, String glueString) {

		String output = "";
		if (inputArray.length > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(inputArray[0]);
			for (int i=1; i<inputArray.length; i++) {
				sb.append(glueString);
				sb.append(inputArray[i]);
			}
			output = sb.toString();
		}
		return output;
	}

	
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }


    public void NamesEditDialog(final String key,String title) {
        Resources res = getActivity().getResources();
        String cancel = res.getString(R.string.cancel);
        String ok = res.getString(R.string.ps_volt_save);

        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View alphaDialog = factory.inflate(R.layout.sh_dialog, null);


        settingText = (EditText) alphaDialog.findViewById(R.id.shText);
        settingText.setText(mPreferences.getString(key,""));
        settingText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return true;
            }
        });

        settingText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.sh_title))
                .setMessage(getString(R.string.sh_msg))
                .setView(alphaDialog)
                .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        /* nothing */
                    }
                })
                .setPositiveButton(ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final SharedPreferences.Editor editor = mPreferences.edit();
                        editor.putString(key, settingText.getText().toString()).commit();

                    }
                })
                .create()
                .show();
    }


    public void openDialog(final int idx,int currentProgress, String title, final int min, final int max,
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
					values[idx]=Integer.toString(newProgress*256);
					pref.setSummary(newProgress+" MB "+"("+values[idx]+")");
					new CMDProcessor().su.runWaitFor("busybox echo " + implodeArray(values,",") + " > " + path);
					final SharedPreferences.Editor editor = mPreferences.edit();
					editor.putString(key, implodeArray(values,","));
					editor.commit();
				}
			}).create().show();
			
    }
}
