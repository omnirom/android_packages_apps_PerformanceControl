package com.brewcrewfoo.performance.fragments;

/**
 * Created by h0rn3t on 15.09.2013.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

public class VM extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Constants {

    private Preference mDirtyRatio;
    private Preference mDirtyBackground;
    private Preference mDirtyExpireCentisecs;
    private Preference mDirtyWriteback;
    private Preference mMinFreeK;
    private Preference mOvercommit;
    private Preference mSwappiness;
    private Preference mVfs;

    private SharedPreferences mPreferences;

    private int mSeekbarProgress;
    private EditText settingText;
    private Context context;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.xml.vm);

        mDirtyRatio = findPreference(PREF_DIRTY_RATIO);
        mDirtyBackground = findPreference(PREF_DIRTY_BACKGROUND);
        mDirtyExpireCentisecs = findPreference(PREF_DIRTY_EXPIRE);
        mDirtyWriteback = findPreference(PREF_DIRTY_WRITEBACK);
        mMinFreeK = findPreference(PREF_MIN_FREE_KB);
        mOvercommit = findPreference(PREF_OVERCOMMIT);
        mSwappiness = findPreference(PREF_SWAPPINESS);
        mVfs = findPreference(PREF_VFS);

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
        if (!new File(DYNAMIC_DIRTY_WRITEBACK_PATH).exists()) {
            mDirtyWriteback.setEnabled(true);
        } else {
            mDirtyWriteback.setEnabled(
                    !Helpers.readOneLine(DYNAMIC_DIRTY_WRITEBACK_PATH).equals("1"));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
            inflater.inflate(R.menu.vm_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_settings:
                Intent intent = new Intent(context, PCSettings.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDirtyRatio) {
            String title = getString(R.string.dirty_ratio_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_RATIO_PATH));
            openDialog(currentProgress, title, 0, 100, preference,
                    DIRTY_RATIO_PATH, PREF_DIRTY_RATIO);
            return true;
        } else if (preference == mDirtyBackground) {
            String title = getString(R.string.dirty_background_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_BACKGROUND_PATH));
            openDialog(currentProgress, title, 0, 100, preference,
                    DIRTY_BACKGROUND_PATH, PREF_DIRTY_BACKGROUND);
            return true;
        } else if (preference == mDirtyExpireCentisecs) {
            String title = getString(R.string.dirty_expire_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_EXPIRE_PATH));
            openDialog(currentProgress, title, 0, 5000, preference,
                    DIRTY_EXPIRE_PATH, PREF_DIRTY_EXPIRE);
            return true;
        } else if (preference == mDirtyWriteback) {
            String title = getString(R.string.dirty_writeback_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_PATH));
            openDialog(currentProgress, title, 0, 5000, preference,
                    DIRTY_WRITEBACK_PATH, PREF_DIRTY_WRITEBACK);
            return true;
        } else if (preference == mMinFreeK) {
            String title = getString(R.string.min_free_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(MIN_FREE_PATH));
            openDialog(currentProgress, title, 0, 8192, preference,
                    MIN_FREE_PATH, PREF_MIN_FREE_KB);
            return true;
        } else if (preference == mOvercommit) {
            String title = getString(R.string.overcommit_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(OVERCOMMIT_PATH));
            openDialog(currentProgress, title, 0, 100, preference,
                    OVERCOMMIT_PATH, PREF_OVERCOMMIT);
            return true;
        } else if (preference == mSwappiness) {
            String title = getString(R.string.swappiness_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(SWAPPINESS_PATH));
            openDialog(currentProgress, title, 0, 100, preference,
                    SWAPPINESS_PATH, PREF_SWAPPINESS);
            return true;
        } else if (preference == mVfs) {
            String title = getString(R.string.vfs_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(VFS_CACHE_PRESSURE_PATH));
            openDialog(currentProgress, title, 0, 200, preference,
                    VFS_CACHE_PRESSURE_PATH, PREF_VFS);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (key.equals(VM_SOB)) {
            if (sharedPreferences.getBoolean(key, false)) {
                editor.putInt(PREF_DIRTY_RATIO,
                        Integer.parseInt(Helpers.readOneLine(DIRTY_RATIO_PATH)))
                        .putInt(PREF_DIRTY_BACKGROUND,
                                Integer.parseInt(Helpers.readOneLine(DIRTY_BACKGROUND_PATH)))
                        .putInt(PREF_DIRTY_EXPIRE,
                                Integer.parseInt(Helpers.readOneLine(DIRTY_EXPIRE_PATH)))
                        .putInt(PREF_DIRTY_WRITEBACK,
                                Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_PATH)))
                        .putInt(PREF_MIN_FREE_KB,
                                Integer.parseInt(Helpers.readOneLine(MIN_FREE_PATH)))
                        .putInt(PREF_OVERCOMMIT,
                                Integer.parseInt(Helpers.readOneLine(OVERCOMMIT_PATH)))
                        .putInt(PREF_SWAPPINESS,
                                Integer.parseInt(Helpers.readOneLine(SWAPPINESS_PATH)))
                        .putInt(PREF_VFS,
                                Integer.parseInt(Helpers.readOneLine(VFS_CACHE_PRESSURE_PATH)))
                        .apply();
            } else {
                editor.remove(PREF_DIRTY_RATIO)
                        .remove(PREF_DIRTY_BACKGROUND)
                        .remove(PREF_DIRTY_EXPIRE)
                        .remove(PREF_DIRTY_WRITEBACK)
                        .remove(PREF_MIN_FREE_KB)
                        .remove(PREF_OVERCOMMIT)
                        .remove(PREF_SWAPPINESS)
                        .remove(PREF_VFS)
                        .apply();
            }
        }
    }

    public void openDialog(int currentProgress, String title, final int min, final int max,
                           final Preference pref, final String path, final String key) {
        Resources res = context.getResources();
        String cancel = res.getString(R.string.cancel);
        String ok = res.getString(R.string.ok);
        LayoutInflater factory = LayoutInflater.from(context);
        final View alphaDialog = factory.inflate(R.layout.seekbar_dialog, null);

        final SeekBar seekbar = (SeekBar) alphaDialog.findViewById(R.id.seek_bar);

        seekbar.setMax(max);
        seekbar.setProgress(currentProgress);

        settingText = (EditText) alphaDialog.findViewById(R.id.setting_text);
        settingText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    int val = Integer.parseInt(settingText.getText().toString());
                    seekbar.setProgress(val);
                    return true;
                }
                return false;
            }
        });
        settingText.setText(Integer.toString(currentProgress));
        settingText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int val = Integer.parseInt(s.toString());
                    if (val > max) {
                        s.replace(0, s.length(), Integer.toString(max));
                        val = max;
                    }
                    seekbar.setProgress(val);
                } catch (NumberFormatException ex) {
                }
            }
        });

        SeekBar.OnSeekBarChangeListener seekBarChangeListener =
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                        mSeekbarProgress = seekbar.getProgress();
                        if (fromUser) {
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

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(alphaDialog)
                .setNegativeButton(cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // nothing
                            }
                        })
                .setPositiveButton(ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int val = Integer.parseInt(settingText.getText().toString());
                        if (val < min) {
                            val = min;
                        }
                        seekbar.setProgress(val);
                        int newProgress = seekbar.getProgress();
                        pref.setSummary(Integer.toString(newProgress));
                        if (Helpers.isSystemApp(getActivity())) {
                            Helpers.writeOneLine(path, Integer.toString(newProgress));
                        } else {
                            new CMDProcessor().su.runWaitFor(
                                    "busybox echo " + newProgress + " > " + path);
                        }
                        final SharedPreferences.Editor editor = mPreferences.edit();
                        editor.putInt(key, newProgress);
                        editor.commit();
                    }
                }).create().show();
    }
}


