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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.Helpers;

import static com.brewcrewfoo.performance.util.Constants.VERSION_NUM;

public class PCSettings extends PreferenceActivity implements OnPreferenceChangeListener {

    SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.pc_settings);

        Preference mVersion = findPreference("version_info");
        mVersion.setTitle(getString(R.string.pt_ver) + VERSION_NUM);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
