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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
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
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

public class Advanced extends PreferenceFragment
        implements OnSharedPreferenceChangeListener, Constants {

    private CheckBoxPreference mDsync;

    private Preference mBltimeout;
    private CheckBoxPreference mBltouch;

    private CheckBoxPreference mBln;

    private CheckBoxPreference mHomeOn;
    private CheckBoxPreference mMenuBackOn;

    private Preference mHomeAllowedIrqs;
    private Preference mHomeReportWait;

    private Preference mMenuBackIrqChecks;
    private Preference mMenuBackFirstErrWait;
    private Preference mMenuBackLastErrWait;

    private CheckBoxPreference mDynamicWriteBackOn;
    private Preference mDynamicWriteBackActive;
    private Preference mDynamicWriteBackSuspend;

    private ListPreference mReadAhead;
    private SharedPreferences mPreferences;

    private int mSeekbarProgress;
    private EditText settingText;
    private String sreadahead;
    private String BLN_PATH;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.xml.advanced);

        sreadahead = getResources().getString(R.string.ps_read_ahead, "");

        mReadAhead = (ListPreference) findPreference(PREF_READ_AHEAD);
        mBltimeout = findPreference(PREF_BLTIMEOUT);
        mBltouch = (CheckBoxPreference) findPreference(PREF_BLTOUCH);
        mBln = (CheckBoxPreference) findPreference(PREF_BLN);
        mDsync = (CheckBoxPreference) findPreference(PREF_DSYNC);
        mHomeOn = (CheckBoxPreference) findPreference(PFK_HOME_ON);
        mHomeAllowedIrqs = findPreference(PREF_HOME_ALLOWED_IRQ);
        mHomeReportWait = findPreference(PREF_HOME_REPORT_WAIT);
        mMenuBackOn = (CheckBoxPreference) findPreference(PFK_MENUBACK_ON);
        mMenuBackIrqChecks = findPreference(PREF_MENUBACK_INTERRUPT_CHECKS);
        mMenuBackFirstErrWait = findPreference(PREF_MENUBACK_FIRST_ERR_WAIT);
        mMenuBackLastErrWait = findPreference(PREF_MENUBACK_LAST_ERR_WAIT);

        mDynamicWriteBackOn = (CheckBoxPreference) findPreference(PREF_DYNAMIC_DIRTY_WRITEBACK);
        mDynamicWriteBackActive = findPreference(PREF_DIRTY_WRITEBACK_ACTIVE);
        mDynamicWriteBackSuspend = findPreference(PREF_DIRTY_WRITEBACK_SUSPEND);


        if (!new File(DSYNC_PATH).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("dsync");
            getPreferenceScreen().removePreference(hideCat);
        } else {
            mDsync.setChecked(Helpers.readOneLine(DSYNC_PATH).equals("1"));
        }
        if (!new File(PFK_HOME_ENABLED).exists() || !new File(PFK_MENUBACK_ENABLED).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("pfk");
            getPreferenceScreen().removePreference(hideCat);
        } else {
            mHomeOn.setChecked(Helpers.readOneLine(PFK_HOME_ENABLED).equals("1"));
            mHomeOn.setSummary(getString(R.string.ps_home_enabled, Helpers.readOneLine(PFK_HOME_IGNORED_KP)));
            mHomeAllowedIrqs.setSummary(Helpers.readOneLine(PFK_HOME_ALLOWED_IRQ));
            mHomeReportWait.setSummary(Helpers.readOneLine(PFK_HOME_REPORT_WAIT) + " ms");

            mMenuBackOn.setChecked(Helpers.readOneLine(PFK_MENUBACK_ENABLED).equals("1"));
            mMenuBackOn.setSummary(getString(R.string.ps_menuback_enabled, Helpers.readOneLine(PFK_MENUBACK_IGNORED_KP)));
            mMenuBackIrqChecks.setSummary(Helpers.readOneLine(PFK_MENUBACK_INTERRUPT_CHECKS));
            mMenuBackFirstErrWait.setSummary(
                    Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT) + " ms");
            mMenuBackLastErrWait.setSummary(Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT) + " ms");
        }
        if (!new File(BL_TIMEOUT_PATH).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("bltimeout");
            getPreferenceScreen().removePreference(hideCat);
        } else {
            mBltimeout.setSummary(Helpers.readOneLine(BL_TIMEOUT_PATH) + " ms");
        }
        if (!new File(BL_TOUCH_ON_PATH).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("bltouch");
            getPreferenceScreen().removePreference(hideCat);
        } else {
            mBltouch.setChecked(Helpers.readOneLine(BL_TOUCH_ON_PATH).equals("1"));
        }
        BLN_PATH = Helpers.bln_path();
        if (BLN_PATH == null) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("bln");
            getPreferenceScreen().removePreference(hideCat);
        } else {
            mBln.setChecked(Helpers.readOneLine(BLN_PATH).equals("1"));
        }
        if (!new File(DYNAMIC_DIRTY_WRITEBACK_PATH).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory)
                    findPreference("cat_dynamic_write_back");
            getPreferenceScreen().removePreference(hideCat);
        } else {
            boolean ison = Helpers.readOneLine(DYNAMIC_DIRTY_WRITEBACK_PATH).equals("1");
            mDynamicWriteBackOn.setChecked(ison);
            mDynamicWriteBackActive.setSummary(Helpers.readOneLine(DIRTY_WRITEBACK_ACTIVE_PATH));
            mDynamicWriteBackSuspend.setSummary(Helpers.readOneLine(DIRTY_WRITEBACK_SUSPEND_PATH));
        }
        final String readahead = Helpers.readOneLine(READ_AHEAD_PATH);
        mReadAhead.setValue(readahead);
        mReadAhead.setSummary(getString(R.string.ps_read_ahead, readahead + "  kb"));

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
            inflater.inflate(R.menu.advanced_menu, menu);
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

        if (preference == mDsync) {
            if (Integer.parseInt(Helpers.readOneLine(DSYNC_PATH)) == 0) {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(DSYNC_PATH, "1");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 1 > " + DSYNC_PATH);
                }
            } else {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(DSYNC_PATH, "0");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 0 > " + DSYNC_PATH);
                }
            }
            return true;
        } else if (preference == mBltimeout) {
            String title = getString(R.string.bltimeout_title) + " (ms)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(BL_TIMEOUT_PATH));
            openDialog(currentProgress, title, 0, 5000, preference, BL_TIMEOUT_PATH, PREF_BLTIMEOUT);
            return true;
        } else if (preference == mBltouch) {
            if (Integer.parseInt(Helpers.readOneLine(BL_TOUCH_ON_PATH)) == 0) {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(BL_TOUCH_ON_PATH, "1");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 1 > " + BL_TOUCH_ON_PATH);
                }
            } else {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(BL_TOUCH_ON_PATH, "0");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 0 > " + BL_TOUCH_ON_PATH);
                }
            }
            return true;
        } else if (preference == mBln) {
            if (Integer.parseInt(Helpers.readOneLine(BLN_PATH)) == 0) {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(BLN_PATH, "1");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 1 > " + BLN_PATH);
                }
            } else {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(BLN_PATH, "0");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 0 > " + BLN_PATH);
                }
            }
            return true;
        } else if (preference == mHomeOn) {
            if (Integer.parseInt(Helpers.readOneLine(PFK_HOME_ENABLED)) == 0) {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(PFK_HOME_ENABLED, "1");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 1 > " + PFK_HOME_ENABLED);
                }
            } else {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(PFK_HOME_ENABLED, "0");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 0 > " + PFK_HOME_ENABLED);
                }
            }
            return true;
        } else if (preference == mMenuBackOn) {
            if (Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_ENABLED)) == 0) {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(PFK_MENUBACK_ENABLED, "1");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 1 > " + PFK_MENUBACK_ENABLED);
                }
            } else {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(PFK_MENUBACK_ENABLED, "0");
                } else {
                    new CMDProcessor().su.runWaitFor("busybox echo 0 > " + PFK_MENUBACK_ENABLED);
                }
            }
            return true;
        } else if (preference == mHomeAllowedIrqs) {
            String title = getString(R.string.home_allowed_irq_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_HOME_ALLOWED_IRQ));
            openDialog(currentProgress, title, 1, 32, preference,
                    PFK_HOME_ALLOWED_IRQ, PREF_HOME_ALLOWED_IRQ);
            return true;
        } else if (preference == mHomeReportWait) {
            String title = getString(R.string.home_report_wait_title) + " (ms)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_HOME_REPORT_WAIT));
            openDialog(currentProgress, title, 5, 25, preference,
                    PFK_HOME_REPORT_WAIT, PREF_HOME_REPORT_WAIT);
            return true;
        } else if (preference == mMenuBackIrqChecks) {
            String title = getString(R.string.menuback_interrupt_checks_title);
            int currentProgress = Integer.parseInt(
                    Helpers.readOneLine(PFK_MENUBACK_INTERRUPT_CHECKS));
            openDialog(currentProgress, title, 1, 10, preference,
                    PFK_MENUBACK_INTERRUPT_CHECKS, PREF_MENUBACK_INTERRUPT_CHECKS);
            return true;
        } else if (preference == mMenuBackFirstErrWait) {
            String title = getString(R.string.menuback_first_err_wait_title) + " (ms)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT));
            openDialog(currentProgress, title, 50, 1000, preference,
                    PFK_MENUBACK_FIRST_ERR_WAIT, PREF_MENUBACK_FIRST_ERR_WAIT);
            return true;
        } else if (preference == mMenuBackLastErrWait) {
            String title = getString(R.string.menuback_last_err_wait_title) + " (ms)";
            int currentProgress = Integer.parseInt(Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT));
            openDialog(currentProgress, title, 50, 100, preference,
                    PFK_MENUBACK_LAST_ERR_WAIT, PREF_MENUBACK_LAST_ERR_WAIT);
            return true;
        } else if (preference == mDynamicWriteBackOn) {
            if (Integer.parseInt(Helpers.readOneLine(DYNAMIC_DIRTY_WRITEBACK_PATH)) == 0) {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(DYNAMIC_DIRTY_WRITEBACK_PATH, "1");
                } else {
                    new CMDProcessor().su.runWaitFor(
                            "busybox echo 1 > " + DYNAMIC_DIRTY_WRITEBACK_PATH);
                }
            } else {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(DYNAMIC_DIRTY_WRITEBACK_PATH, "0");
                } else {
                    new CMDProcessor().su.runWaitFor(
                            "busybox echo 0 > " + DYNAMIC_DIRTY_WRITEBACK_PATH);
                }
            }
            return true;
        } else if (preference == mDynamicWriteBackActive) {
            String title = getString(R.string.dynamic_writeback_active_title);
            int currentProgress = Integer.parseInt(Helpers.readOneLine(DIRTY_WRITEBACK_ACTIVE_PATH));
            openDialog(currentProgress, title, 0, 5000, preference,
                    DIRTY_WRITEBACK_ACTIVE_PATH, PREF_DIRTY_WRITEBACK_ACTIVE);
            return true;
        } else if (preference == mDynamicWriteBackSuspend) {
            String title = getString(R.string.dynamic_writeback_suspend_title);
            int currentProgress = Integer.parseInt(
                    Helpers.readOneLine(DIRTY_WRITEBACK_SUSPEND_PATH));
            openDialog(currentProgress, title, 0, 5000, preference,
                    DIRTY_WRITEBACK_SUSPEND_PATH, PREF_DIRTY_WRITEBACK_SUSPEND);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        if (key.equals(PREF_READ_AHEAD)) {
            final String values = mReadAhead.getValue();
            if (!values.equals(Helpers.readOneLine(READ_AHEAD_PATH))) {
                new CMDProcessor().su.runWaitFor("busybox echo " + values + " > " + READ_AHEAD_PATH);
            }
            mReadAhead.setSummary(sreadahead + values + " kb");
        } else if (key.equals(PREF_BLTIMEOUT)) {
            mBltimeout.setSummary(Helpers.readOneLine(BL_TIMEOUT_PATH) + " ms");
        } else if (key.equals(PREF_HOME_REPORT_WAIT)) {
            mHomeReportWait.setSummary(Helpers.readOneLine(PFK_HOME_REPORT_WAIT) + " ms");
        } else if (key.equals(PREF_MENUBACK_FIRST_ERR_WAIT)) {
            mMenuBackFirstErrWait.setSummary(
                    Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT) + " ms");
        } else if (key.equals(PREF_MENUBACK_LAST_ERR_WAIT)) {
            mMenuBackLastErrWait.setSummary(Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT) + " ms");
        } else if (key.equals(BLX_SOB)) {
            if (sharedPreferences.getBoolean(key, false)) {
                editor.putInt(PREF_BLX, Integer.parseInt(Helpers.readOneLine(BLX_PATH))).apply();
            } else {
                editor.remove(PREF_BLX).apply();
            }
        } else if (key.equals(BLTIMEOUT_SOB)) {
            if (sharedPreferences.getBoolean(key, false)) {
                editor.putInt(PREF_BLTIMEOUT, Integer.parseInt(
                        Helpers.readOneLine(BL_TIMEOUT_PATH))).apply();
            } else {
                editor.remove(PREF_BLTIMEOUT).apply();
            }
        } else if (key.equals(PFK_SOB)) {
            if (sharedPreferences.getBoolean(key, false)) {
                if (Helpers.readOneLine(PFK_HOME_ENABLED).equals("1")) {
                    editor.putBoolean(PFK_HOME_ON, true);
                } else {
                    editor.putBoolean(PFK_HOME_ON, false);
                }
                editor.putInt(PREF_HOME_ALLOWED_IRQ, Integer.parseInt(
                        Helpers.readOneLine(PFK_HOME_ALLOWED_IRQ)))
                        .putInt(PREF_HOME_REPORT_WAIT, Integer.parseInt(
                                Helpers.readOneLine(PFK_HOME_REPORT_WAIT)));
                if (Helpers.readOneLine(PFK_MENUBACK_ENABLED).equals("1")) {
                    editor.putBoolean(PFK_MENUBACK_ON, true);
                } else {
                    editor.putBoolean(PFK_MENUBACK_ON, false);
                }
                editor.putInt(PREF_MENUBACK_INTERRUPT_CHECKS, Integer.parseInt(
                        Helpers.readOneLine(PFK_MENUBACK_INTERRUPT_CHECKS)))
                        .putInt(PREF_MENUBACK_FIRST_ERR_WAIT, Integer.parseInt(
                                Helpers.readOneLine(PFK_MENUBACK_FIRST_ERR_WAIT)))
                        .putInt(PREF_MENUBACK_LAST_ERR_WAIT, Integer.parseInt(
                                Helpers.readOneLine(PFK_MENUBACK_LAST_ERR_WAIT)))
                        .apply();
            } else {
                editor.remove(PFK_HOME_ON)
                        .remove(PREF_HOME_ALLOWED_IRQ)
                        .remove(PREF_HOME_REPORT_WAIT)
                        .remove(PFK_MENUBACK_ON)
                        .remove(PREF_MENUBACK_INTERRUPT_CHECKS)
                        .remove(PREF_MENUBACK_FIRST_ERR_WAIT)
                        .remove(PREF_MENUBACK_LAST_ERR_WAIT)
                        .apply();
            }
        } else if (key.equals(DYNAMIC_DIRTY_WRITEBACK_SOB)) {
            if (sharedPreferences.getBoolean(key, false)) {
                if (Helpers.readOneLine(DYNAMIC_DIRTY_WRITEBACK_PATH).equals("1")) {
                    editor.putBoolean(PREF_DYNAMIC_DIRTY_WRITEBACK, true);
                } else {
                    editor.putBoolean(PREF_DYNAMIC_DIRTY_WRITEBACK, false);
                }
                editor.putInt(PREF_DIRTY_WRITEBACK_ACTIVE, Integer.parseInt(
                        Helpers.readOneLine(DIRTY_WRITEBACK_ACTIVE_PATH)))
                        .putInt(PREF_DIRTY_WRITEBACK_SUSPEND, Integer.parseInt(
                                Helpers.readOneLine(DIRTY_WRITEBACK_SUSPEND_PATH)))
                        .apply();
            } else {
                editor.remove(PREF_DYNAMIC_DIRTY_WRITEBACK)
                        .remove(PREF_DIRTY_WRITEBACK_ACTIVE)
                        .remove(PREF_DIRTY_WRITEBACK_SUSPEND)
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

        OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {
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

