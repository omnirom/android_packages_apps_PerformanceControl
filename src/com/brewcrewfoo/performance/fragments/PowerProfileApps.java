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
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;

public class PowerProfileApps extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, AdapterView.OnItemLongClickListener {
    private static final String TAG = "PowerProfileApps";

    private static final String KEY_APPS_PROFILE = "power_profile_apps";

    private static final int ACTION_DELETE = 0;
    private static final int MENU_ADD = 0;

    private List<PowerProfileManager.PowerProfile> mProfiles;
    private boolean mConfigured;
    private PreferenceGroup mApplicationPrefList;
    private Map<String, String> mProfilesApps = new HashMap<String, String>();
    private PackageManager mPackageManager;
    private PackageAdapter mPackageAdapter;
    private Menu mMenu;
    private String mDefaultProfile;
    private PowerProfileManager mManager;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.OMNI_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ContentResolver resolver = getActivity().getContentResolver();

        mManager = new PowerProfileManager();
        mConfigured = mManager.load();

        if (!mConfigured) {
            return;
        }

        mDefaultProfile = mManager.getDefaultProfile();
        mProfiles = mManager.getProfiles();

        addPreferencesFromResource(R.xml.power_profile_apps);

        mApplicationPrefList = (PreferenceGroup) findPreference(KEY_APPS_PROFILE);
        mApplicationPrefList.setOrderingAsAdded(false);

        String appData = Settings.System.getString(resolver,
                Settings.System.POWER_PROFILE_APPS);
        parseAppsData(appData);

        // Get launch-able applications
        mPackageManager = getActivity().getPackageManager();
        mPackageAdapter = new PackageAdapter();

        setHasOptionsMenu(true);
        calcUserProfileConfig();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mConfigured) {
            refreshProfileApps();
            getListView().setOnItemLongClickListener(this);
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = getActivity().getContentResolver();

        ListPreference appProfile = (ListPreference) preference;
        mProfilesApps.put(appProfile.getKey(), (String) newValue);
        savePackageList();
        int index = appProfile.findIndexOfValue((String) newValue);
        appProfile.setSummary(appProfile.getEntries()[index]);

        return true;
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

    private void parseAppsData(String appData) {
        mProfilesApps.clear();

        if (appData == null) {
            return;
        }
        String[] appEntries = appData.split("\\|\\|");
        for (int i = 0; i < appEntries.length; i++) {
            String[] appEntry = appEntries[i].split("\\|");
            if (appEntry.length == 2) {
                String componentName = appEntry[0];
                String profileName = appEntry[1];
                mProfilesApps.put(componentName, profileName);
            }
        }
    }

    private void refreshProfileApps() {
        Context context = getActivity();

        if (mApplicationPrefList != null) {
            mApplicationPrefList.removeAll();
            Iterator<String> nextPackage = mProfilesApps.keySet().iterator();
            while (nextPackage.hasNext()) {
                String pkgName = nextPackage.next();
                String profileName = mProfilesApps.get(pkgName);
                try {
                    PackageInfo info = mPackageManager.getPackageInfo(pkgName,
                            PackageManager.GET_META_DATA);
                    ListPreference pref = new ListPreference(context);
                    pref.setKey(pkgName);
                    CharSequence label = info.applicationInfo.loadLabel(mPackageManager);
                    pref.setTitle(label);
                    pref.setDialogTitle(label);
                    pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
                    pref.setPersistent(false);
                    pref.setOnPreferenceChangeListener(this);
                    fillProfileList(pref);
                    pref.setValue(profileName);
                    int index = pref.findIndexOfValue(profileName);
                    pref.setSummary(pref.getEntries()[index]);

                    mApplicationPrefList.addPreference(pref);
                } catch (NameNotFoundException e) {
                    // ignore app e.g. has been uninstalled since last time
                }
            }
        }
    }

    private void addProfileAppPref(String packageName) {
        mProfilesApps.put(packageName, mDefaultProfile);
        savePackageList();
        refreshProfileApps();
    }

    private void removeProfileAppPref(String packageName) {
        if (mProfilesApps.remove(packageName) != null) {
            savePackageList();
            refreshProfileApps();
        }
    }

    private void savePackageList() {
        final ContentResolver resolver = getActivity().getContentResolver();
        List<String> settings = new ArrayList<String>();
        Iterator<String> nextPackage = mProfilesApps.keySet().iterator();
        while (nextPackage.hasNext()) {
            String pkgName = nextPackage.next();
            String profile = mProfilesApps.get(pkgName);
            settings.add(pkgName + "|" + profile);
        }
        final String value = TextUtils.join("||", settings);
        Settings.System.putString(resolver, Settings.System.POWER_PROFILE_APPS,
                value);
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        final Preference pref = (Preference) getPreferenceScreen()
                .getRootAdapter().getItem(position);

        if (mApplicationPrefList.findPreference(pref.getKey()) != pref) {
            return false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                removeProfileAppPref(pref.getKey());
                            }
                        }).setNegativeButton(android.R.string.cancel, null);

        builder.show();
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu = menu;
        mMenu.add(0, MENU_ADD, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add_white)
                .setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(MENU_ADD).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ADD:
            mPackageAdapter.reloadList();
            showAddDialog();
            return true;
        }
        return false;
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final ListView list = new ListView(getActivity());
        list.setAdapter(mPackageAdapter);

        builder.setTitle(R.string.profile_choose_app)
            .setView(list)
            .setNegativeButton(getString(android.R.string.cancel), null);
        final Dialog dialog = builder.create();

        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                PackageItem info = (PackageItem) parent
                        .getItemAtPosition(position);
                addProfileAppPref(info.packageName);
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void calcUserProfileConfig() {
        if (getActivity().getFilesDir() != null) {
            // /data/data/com.android.settings/files/
            String path =  getActivity().getFilesDir().getAbsolutePath();
            Log.i(TAG, "calcUserProfileConfig " + path);
        }
    }

    /**
     * AppItem class
     */
    private static class PackageItem implements Comparable<PackageItem> {
        CharSequence title;
        TreeSet<CharSequence> activityTitles = new TreeSet<CharSequence>();
        String packageName;
        Drawable icon;

        @Override
        public int compareTo(PackageItem another) {
            int result = title.toString().compareToIgnoreCase(
                    another.title.toString());
            return result != 0 ? result : packageName
                    .compareTo(another.packageName);
        }
    }

    /**
     * AppAdapter class
     */
    private class PackageAdapter extends BaseAdapter {
        private List<PackageItem> mInstalledPackages = new LinkedList<PackageItem>();

        public void reloadList() {
            final Handler handler = new Handler();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mInstalledPackages) {
                        mInstalledPackages.clear();
                    }

                    final Intent mainIntent = new Intent(Intent.ACTION_MAIN,
                            null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    List<ResolveInfo> installedAppsInfo = mPackageManager
                            .queryIntentActivities(mainIntent, 0);

                    for (ResolveInfo info : installedAppsInfo) {
                        ApplicationInfo appInfo = info.activityInfo.applicationInfo;

                        final PackageItem item = new PackageItem();
                        item.title = appInfo.loadLabel(mPackageManager);
                        item.activityTitles.add(info.loadLabel(mPackageManager));
                        item.icon = appInfo.loadIcon(mPackageManager);
                        item.packageName = appInfo.packageName;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                int index = Collections.binarySearch(
                                        mInstalledPackages, item);
                                if (index < 0) {
                                    mInstalledPackages.add(-index - 1, item);
                                } else {
                                    mInstalledPackages.get(index).activityTitles
                                            .addAll(item.activityTitles);
                                }
                                notifyDataSetChanged();
                            }
                        });
                    }
                }
            }).start();
        }

        public PackageAdapter() {
        }

        @Override
        public int getCount() {
            synchronized (mInstalledPackages) {
                return mInstalledPackages.size();
            }
        }

        @Override
        public PackageItem getItem(int position) {
            synchronized (mInstalledPackages) {
                return mInstalledPackages.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            synchronized (mInstalledPackages) {
                // packageName is guaranteed to be unique in mInstalledPackages
                return mInstalledPackages.get(position).packageName.hashCode();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                final LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.profile_app_item,
                        null, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.title = (TextView) convertView
                        .findViewById(com.android.internal.R.id.title);
                holder.summary = (TextView) convertView
                        .findViewById(com.android.internal.R.id.summary);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            }
            PackageItem applicationInfo = getItem(position);

            holder.title.setText(applicationInfo.title);
            holder.icon.setImageDrawable(applicationInfo.icon);

            boolean needSummary = applicationInfo.activityTitles.size() > 0;
            if (applicationInfo.activityTitles.size() == 1) {
                if (TextUtils.equals(applicationInfo.title,
                        applicationInfo.activityTitles.first())) {
                    needSummary = false;
                }
            }

            if (needSummary) {
                holder.summary.setText(TextUtils.join(", ",
                        applicationInfo.activityTitles));
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    static class ViewHolder {
        TextView title;
        TextView summary;
        ImageView icon;
    }
}
