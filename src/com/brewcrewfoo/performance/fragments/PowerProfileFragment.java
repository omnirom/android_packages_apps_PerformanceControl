/*
* Copyright (C) 2014 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/

package com.brewcrewfoo.performance.fragments;

import android.app.Fragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Collections;

import com.brewcrewfoo.performance.R;

public class PowerProfileFragment extends PreferenceFragment implements OnPreferenceChangeListener  {
    private static final String TAG = "PowerProfileFragment";

    private static final String KEY_POWER_PROFILE_ENABLE = "power_profile_enable";
    private static final String KEY_DEFAULT_POWER_PROFILE = "default_power_profile";
    private static final String KEY_SCREEN_OFF_POWER_PROFILE = "screen_off_power_profile";
    private static final String KEY_POWER_PROFILE_PLUGGED = "power_profile_plugged";

    private SwitchPreference mEnablePowerProfile;
    private ListPreference mDefaultPowerProfile;
    private ListPreference mScreenOffPowerProfile;
    private List<PowerProfileManager.PowerProfile> mProfiles;
    private boolean mConfigured;
    private CheckBoxPreference mPowerProfilePlugged;
    private String mDefaultProfile;
    private PowerProfileManager mManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mManager = new PowerProfileManager();
        mConfigured = mManager.load();

        if (!mConfigured) {
            return;
        }

        mDefaultProfile = mManager.getDefaultProfile();
        mProfiles = mManager.getProfiles();

        addPreferencesFromResource(R.xml.power_profile_settings);

        mEnablePowerProfile = (SwitchPreference) findPreference(KEY_POWER_PROFILE_ENABLE);
        mDefaultPowerProfile = (ListPreference) findPreference(KEY_DEFAULT_POWER_PROFILE);
        mDefaultPowerProfile.setOnPreferenceChangeListener(this);
        mScreenOffPowerProfile = (ListPreference) findPreference(KEY_SCREEN_OFF_POWER_PROFILE);
        mScreenOffPowerProfile.setOnPreferenceChangeListener(this);
        mPowerProfilePlugged = (CheckBoxPreference) findPreference(KEY_POWER_PROFILE_PLUGGED);

        fillProfileList(mDefaultPowerProfile);
        fillProfileList(mScreenOffPowerProfile);

        final ContentResolver resolver = getActivity().getContentResolver();
        mEnablePowerProfile.setChecked(Settings.System.getInt(resolver,
                Settings.System.POWER_PROFILE_ENABLED, 0) != 0);
        String powerProfile = Settings.System.getString(resolver,
                Settings.System.POWER_PROFILE_DEFAULT);
        if (powerProfile == null) {
            powerProfile = mDefaultProfile;
        }
        mDefaultPowerProfile.setValue(powerProfile);
        int index = mDefaultPowerProfile.findIndexOfValue(powerProfile);
        mDefaultPowerProfile
                .setSummary(mDefaultPowerProfile.getEntries()[index]);

        String screenOffProfile = Settings.System.getString(resolver,
                Settings.System.POWER_PROFILE_SCREEN_OFF);
        if (screenOffProfile == null) {
            screenOffProfile = mDefaultProfile;
        }
        mScreenOffPowerProfile.setValue(screenOffProfile);

        index = mScreenOffPowerProfile.findIndexOfValue(screenOffProfile);
        mScreenOffPowerProfile
                .setSummary(mScreenOffPowerProfile.getEntries()[index]);

        mPowerProfilePlugged.setChecked(Settings.System.getInt(resolver,
                Settings.System.POWER_PROFILE_PLUGGED, 1) != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mDefaultPowerProfile) {
            Settings.System.putString(resolver,
                    Settings.System.POWER_PROFILE_DEFAULT, (String) newValue);
            int index = mDefaultPowerProfile
                    .findIndexOfValue((String) newValue);
            mDefaultPowerProfile
                    .setSummary(mDefaultPowerProfile.getEntries()[index]);
        } else if (preference == mScreenOffPowerProfile) {
            Settings.System
                    .putString(resolver,
                            Settings.System.POWER_PROFILE_SCREEN_OFF,
                            (String) newValue);
            int index = mScreenOffPowerProfile
                    .findIndexOfValue((String) newValue);
            mScreenOffPowerProfile.setSummary(mScreenOffPowerProfile
                    .getEntries()[index]);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        final ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mEnablePowerProfile) {
            boolean checked = ((SwitchPreference) preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.POWER_PROFILE_ENABLED, checked ? 1 : 0);
            return true;
        } else if (preference == mPowerProfilePlugged) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.POWER_PROFILE_PLUGGED, checked ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void fillProfileList(ListPreference list) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        Iterator<PowerProfileManager.PowerProfile> nextProfile = mProfiles.iterator();
        while (nextProfile.hasNext()) {
            PowerProfileManager.PowerProfile profile = nextProfile.next();
            if (!profile.mLowPower) {
                entries.add(profile.mName);
                values.add(profile.mName);
            }
        }

        list.setEntries(entries.toArray(new CharSequence[entries.size()]));
        list.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }
}
