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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class PowerProfileManager {
    private static final String SYSTEM_PROFILE_CONFIG = "/system/etc/power_profiles.xml";
    private static final String PROFILE_CONFIG_FILE = "power_profiles.xml";
    private static final String TAG = "PowerProfileManager";

    private List<PowerProfile> mProfiles = new ArrayList<PowerProfile>();
    private boolean mConfigured;
    private String mDefaultProfile;

    public static class PowerProfile {
        String mName;
        Map<String, String> mData = new HashMap<String, String>();
        boolean mLowPower;

        @Override
        public String toString() {
            return mName + ":" + mData;
        }

        public String dataToString() {
            StringBuffer sb = new StringBuffer();
            ArrayList<String> keys = new ArrayList<String>();
            keys.addAll(mData.keySet());
            Collections.sort(keys);
            Iterator<String> nextKey = keys.iterator();
            while (nextKey.hasNext()) {
                String key = nextKey.next();
                String value = mData.get(key);
                sb.append(key + ":" + value + " ");
            }
            return sb.toString().trim();
        }
    };

    public boolean load() {
        loadConfig(SYSTEM_PROFILE_CONFIG);
        if (mDefaultProfile == null) {
            mConfigured = false;
        }
        return mConfigured;
    }

    public String getDefaultProfile() {
        return mDefaultProfile;
    }

    public List<PowerProfile> getProfiles() {
        return mProfiles;
    }
    
    private void loadConfig(String file) {
        mProfiles.clear();

        File f = new File(file);
        if (!f.canRead()) {
            Log.e(TAG, "file not found " + file);
            return;
        }
        XmlPullParserFactory pullParserFactory;
        try {
            pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullParserFactory.newPullParser();

            FileInputStream fIs = new FileInputStream(f);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(fIs, null);
            parser.nextTag();
            parseXML(parser);
            mConfigured = true;

        } catch (XmlPullParserException e) {
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    private void parseXML(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        int eventType = parser.getEventType();
        PowerProfile currentProfile = null;
        int i = 0;
        boolean disable = false;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
            case XmlPullParser.START_DOCUMENT:
                break;
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                if (name.equalsIgnoreCase("profile")) {
                    disable = false;
                    currentProfile = new PowerProfile();
                    int count = parser.getAttributeCount();
                    for (int a = 0; a < count; a++) {
                        String key = parser.getAttributeName(a);
                        String value = parser.getAttributeValue(a);
                        if (key.equals("name")) {
                            currentProfile.mName = value;
                        } else if (key.equals("lowpower")) {
                            currentProfile.mLowPower = true;
                        } else if (key.equals("disable")) {
                            disable = true;
                        } else {
                            currentProfile.mData.put(key, value);
                        }
                    }
                }
                break;
            case XmlPullParser.END_TAG:
                name = parser.getName();
                if (name.equalsIgnoreCase("profile")
                        && currentProfile != null
                        && currentProfile.mName != null) {
                    mProfiles.add(currentProfile);
                    if (disable) {
                        mDefaultProfile = currentProfile.mName;
                    }
                    Log.i(TAG, "added profile " + currentProfile);
                    i++;
                }
            }
            eventType = parser.next();
        }
    }
}
