/*
* Copyright (C) 2015 The OmniROM Project
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;

import java.util.List;
import java.util.Iterator;

import com.brewcrewfoo.performance.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;

public class PowerProfileSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "PowerProfileSettings";

    private static final String KEY_PROFILE_LIST = "power_profile_list";

    private List<PowerProfileManager.PowerProfile> mProfiles;
    private boolean mConfigured;
    private String mDefaultProfile;
    private PowerProfileManager mManager;
    private PreferenceGroup mProfilesList;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.OMNI_SETTINGS;
    }

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

        addPreferencesFromResource(R.xml.power_profile_edit);

        mProfilesList = (PreferenceGroup) findPreference(KEY_PROFILE_LIST);
        mProfilesList.setOrderingAsAdded(false);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mConfigured) {
            refreshProfileList();
        }
    }

    private void refreshProfileList() {
        Context context = getActivity();

        if (mProfilesList != null) {
            mProfilesList.removeAll();
            Iterator<PowerProfileManager.PowerProfile> nextProfile = mProfiles.iterator();
            while (nextProfile.hasNext()) {
                PowerProfileManager.PowerProfile profile = nextProfile.next();
                if (!profile.mLowPower) {
                    Preference pref = new Preference(context);
                    pref.setTitle(profile.mName);
                    pref.setPersistent(false);
                    pref.setSummary(profile.dataToString());

                    mProfilesList.addPreference(pref);
                }
            }
        }
    }
}
