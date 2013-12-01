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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.fragments.Advanced;
import com.brewcrewfoo.performance.fragments.BatteryInfo;
import com.brewcrewfoo.performance.fragments.CPUInfo;
import com.brewcrewfoo.performance.fragments.CPUSettings;
import com.brewcrewfoo.performance.fragments.DiskInfo;
import com.brewcrewfoo.performance.fragments.OOMSettings;
import com.brewcrewfoo.performance.fragments.TimeInState;
import com.brewcrewfoo.performance.fragments.Tools;
import com.brewcrewfoo.performance.fragments.VM;
import com.brewcrewfoo.performance.fragments.VoltageControlSettings;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.widgets.CustomDrawerLayout;


public class MainActivity extends Fragment implements Constants, ActivityThemeChangeInterface {
    SharedPreferences mPreferences;

    private static CharSequence mTitle;

    //==================================
    // Drawer
    //==================================
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
    private ActionBarDrawerToggle mDrawerToggle;
    private CustomDrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;
    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;

    private static int DRAWER_MODE = 0;

    private static boolean mVoltageExists;
    private boolean mIsLightTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTitle = getActivity().getTitle();

        mVoltageExists = Helpers.voltageFileExists();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = mPreferences.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.activity_main, container, false);

        mDrawerListView = (ListView) rootView.findViewById(R.id.pc_navigation_drawer);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        mDrawerListView.setAdapter(new ArrayAdapter<String>(
                getActionBar().getThemedContext(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                getTitles()));
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);

        // Set up the drawer.
        setUp(
                rootView.findViewById(R.id.pc_navigation_drawer),
                (CustomDrawerLayout) rootView.findViewById(R.id.pc_drawer_layout));

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) rootView.getLayoutParams()).removeBorders = true;
        }

        checkForSu();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mDrawerLayout != null && isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            restoreActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //==================================
    // Methods
    //==================================

    /**
     * Call this in Fragments onAttach method
     *
     * @param id The id of the fragment
     */
    public void onSectionAttached(int id) {
        mTitle = getString(id);
        restoreActionBar();
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Restores the action bar after closing the drawer
     */
    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    private ActionBar getActionBar() {
        return getActivity().getActionBar();
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentContainerView The view of this fragment in its activity's layout.
     * @param drawerLayout          The DrawerLayout containing this fragment's UI.
     */
    public void setUp(View fragmentContainerView, CustomDrawerLayout drawerLayout) {
        mFragmentContainerView = fragmentContainerView;
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).commit();
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        selectItem(mCurrentSelectedPosition);
    }


    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.pc_container, PlaceholderFragment.newInstance(getPosition(position)))
                .commit();
    }

    /**
     * Depending on if the item is shown or not, it increases
     * the position to make the activity load the right fragment.
     *
     * @param pos The selected position
     * @return the modified position
     */
    public int getPosition(int pos) {
        Log.e("ASD", "Get Position: " + pos);
        int position = pos;
        switch (DRAWER_MODE) {
            default:
            case 0:
                position = pos;
                break;
            case 1:
                if (pos > 0) position = pos + 1;
                break;
            case 2:
                if (pos > 3) position = pos + 1;
                break;
            case 3:
                if (pos > 0) position = pos + 1;
                if (pos > 3) position = pos + 2;
                break;
        }
        Log.e("ASD", "Got Position: " + pos);
        return position;
    }

    /**
     * Get a list of titles for the tabstrip to display depending on if the
     * voltage control fragment and battery fragment will be displayed. (Depends on the result of
     * Helpers.voltageTableExists() & Helpers.showBattery()
     *
     * @return String[] containing titles
     */
    private String[] getTitles() {
        String titleString[];
        if (mVoltageExists) {
            if (Helpers.showBattery()) {
                if (getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                    DRAWER_MODE = 0;
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_battery_info),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_volt_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info)};
                } else {
                    DRAWER_MODE = 0;
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_battery_info),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_volt_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info),
                            getString(R.string.t_tools)};
                }
            } else {
                if (getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                    DRAWER_MODE = 1;
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_volt_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info)};
                } else {
                    DRAWER_MODE = 1;
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_volt_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info),
                            getString(R.string.t_tools)};
                }
            }
        } else {
            if (Helpers.showBattery()) {
                if (getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                    DRAWER_MODE = 2;
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_battery_info),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info)};
                } else {
                    DRAWER_MODE = 2;
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_battery_info),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info),
                            getString(R.string.t_tools)};
                }
            } else {
                if (getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                    DRAWER_MODE = 3;
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info)};
                } else {
                    DRAWER_MODE = 3;
                    titleString = new String[]{
                            getString(R.string.t_cpu_settings),
                            getString(R.string.t_oom_settings),
                            getString(R.string.prefcat_vm_settings),
                            getString(R.string.t_adv_settings),
                            getString(R.string.t_time_in_state),
                            getString(R.string.t_cpu_info),
                            getString(R.string.t_disk_info),
                            getString(R.string.t_tools)};
                }
            }
        }
        return titleString;
    }

    //==================================
    // Internal Classes
    //==================================

    /**
     * Loads our Fragments.
     */
    public static class PlaceholderFragment extends Fragment {

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static Fragment newInstance(int fragmentId) {
            Fragment fragment;
            switch (fragmentId) {
                default:
                case FRAGMENT_ID_CPUSETTINGS:
                    fragment = new CPUSettings();
                    break;
                case FRAGMENT_ID_BATTERYINFO:
                    fragment = new BatteryInfo();
                    break;
                case FRAGMENT_ID_OOMSETTINGS:
                    fragment = new OOMSettings();
                    break;
                case FRAGMENT_ID_VM:
                    fragment = new VM();
                    break;
                case FRAGMENT_ID_VOLTAGECONROL:
                    fragment = new VoltageControlSettings();
                    break;
                case FRAGMENT_ID_ADVANCED:
                    fragment = new Advanced();
                    break;
                case FRAGMENT_ID_TIMEINSTATE:
                    fragment = new TimeInState();
                    break;
                case FRAGMENT_ID_CPUINFO:
                    fragment = new CPUInfo();
                    break;
                case FRAGMENT_ID_DISKINFO:
                    fragment = new DiskInfo();
                    break;
                case FRAGMENT_ID_TOOLS:
                    fragment = new Tools();
                    break;
            }

            return fragment;
        }

        public PlaceholderFragment() {
            // intentionally left blank
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //if (isThemeChanged()) {
        //    Helpers.restartPC(this);
        //}
    }

    /**
     * Check if root access, and prompt the user to grant PC access
     */
    private void checkForSu() {
        if (Helpers.isSystemApp(getActivity())) {
            return;
        }

        // If this is the first launch of the application. Check for root.
        boolean firstrun = mPreferences.getBoolean("firstrun", true);
        // Continue to bug the user that options will not work.
        boolean rootWasCanceled = mPreferences.getBoolean("rootcanceled", false);

        // Don't bother AOKP users ;)
        PackageManager pm = getActivity().getPackageManager();
        boolean rcInstalled = false;
        try {
            pm.getPackageInfo("com.aokp.romcontrol", PackageManager.GET_ACTIVITIES);
            rcInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            rcInstalled = false;
        }

        // Now that we've decided what to do. Launch the appropriate dialog
        if (firstrun || rootWasCanceled) {
            SharedPreferences.Editor e = mPreferences.edit();
            e.putBoolean("firstrun", false);
            e.commit();
            if (rcInstalled) {
                Helpers.checkSu();
            } else {
                launchFirstRunDialog();
            }
        }
    }

    /**
     * Alert the user that a check for root will be run
     */
    private void launchFirstRunDialog() {
        String title = getString(R.string.first_run_title);
        final String failedTitle = getString(R.string.su_failed_title);
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View firstRunDialog = factory.inflate(R.layout.su_dialog, null);
        TextView tv = (TextView) firstRunDialog.findViewById(R.id.message);
        tv.setText(R.string.first_run_message);
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(firstRunDialog)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String message = getString(R.string.su_cancel_message);
                                SharedPreferences.Editor e = mPreferences.edit();
                                e.putBoolean("rootcanceled", true);
                                e.commit();
                                suResultDialog(failedTitle, message);
                            }
                        })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean canSu = Helpers.checkSu();
                        boolean canBb = Helpers.checkBusybox();
                        if (canSu && canBb) {
                            String title = getString(R.string.su_success_title);
                            String message = getString(R.string.su_success_message);
                            SharedPreferences.Editor e = mPreferences.edit();
                            e.putBoolean("rootcanceled", false);
                            e.commit();
                            suResultDialog(title, message);
                        }
                        if (!canSu || !canBb) {
                            String message = getString(R.string.su_failed_su_or_busybox);
                            SharedPreferences.Editor e = mPreferences.edit();
                            e.putBoolean("rootcanceled", true);
                            e.commit();
                            suResultDialog(failedTitle, message);
                        }
                    }
                }).create().show();
    }

    /**
     * Display the result of the check for root access so the user knows what to
     * expect in respect to functionality of the application.
     *
     * @param title   Oops or OK depending on the result
     * @param message Success or fail message
     */
    private void suResultDialog(String title, String message) {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View suResultDialog = factory.inflate(R.layout.su_dialog, null);
        TextView tv = (TextView) suResultDialog.findViewById(R.id.message);
        tv.setText(message);
        new AlertDialog.Builder(getActivity()).setTitle(title).setView(suResultDialog)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create().show();
    }

    @Override
    public boolean isThemeChanged() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        return is_light_theme != mIsLightTheme;
    }

    @Override
    public void setTheme() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        mIsLightTheme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        //setTheme(is_light_theme ? R.style.Theme_Light : R.style.Theme_Dark);
    }
}

