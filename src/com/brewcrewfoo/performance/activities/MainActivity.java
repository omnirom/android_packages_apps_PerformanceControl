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
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
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
import android.util.Log;

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
import com.brewcrewfoo.performance.fragments.Wakelocks;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.widgets.CustomDrawerLayout;


public class MainActivity extends Fragment implements Constants, ActivityThemeChangeInterface {

    //==================================
    // Static Fields
    //==================================
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
    private static final String PREF_IS_TABBED = "pref_is_tabbed";
    private static final String TAG = "MainActivity";

    //==================================
    // Drawer
    //==================================
    private ActionBarDrawerToggle mDrawerToggle;
    private CustomDrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;
    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;
    private Fragment mCurrentFragment;
    private View mFragmentContainer;

    //==================================
    // Fields
    //==================================
    private static int DRAWER_MODE = 0;
    private static boolean mVoltageExists;
    private SharedPreferences mPreferences;
    private boolean mIsTabbed = true;

    //==================================
    // Overridden Methods
    //==================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVoltageExists = Helpers.voltageFileExists();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = mPreferences.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (getResources().getBoolean(R.bool.config_allow_toggle_tabbed))
            mIsTabbed = mPreferences.getBoolean(PREF_IS_TABBED,
                    getResources().getBoolean(R.bool.config_use_tabbed));
        else
            mIsTabbed = getResources().getBoolean(R.bool.config_use_tabbed);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView;

        if (!mIsTabbed) {
            rootView = (ViewGroup) inflater.inflate(R.layout.activity_main, container, false);

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

            mFragmentContainer = rootView.findViewById(R.id.pc_container);
            setUpNavigationDrawer(
                    rootView.findViewById(R.id.pc_navigation_drawer),
                    (CustomDrawerLayout) rootView.findViewById(R.id.pc_drawer_layout));

        } else {
            rootView = (ViewGroup) inflater.inflate(R.layout.activity_main_tabbed, container, false);

            ViewPager mViewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
            TitleAdapter titleAdapter = new TitleAdapter(getFragmentManager());
            mViewPager.setAdapter(titleAdapter);
            mViewPager.setCurrentItem(0);

            PagerTabStrip mPagerTabStrip = (PagerTabStrip) rootView.findViewById(R.id.pagerTabStrip);
            mPagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.pc_blue));
            mPagerTabStrip.setDrawFullUnderline(false);
        }

        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) rootView.getLayoutParams()).removeBorders = true;
        }

        checkForSu();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!mIsTabbed) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!mIsTabbed) {
            outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate((mIsTabbed ? R.menu.menu_tabbed : R.menu.menu_drawer), menu);
        if (!getResources().getBoolean(R.bool.config_allow_toggle_tabbed)) {
            menu.removeItem(R.id.pc_action_tabbed);
        }
        restoreActionBar();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mIsTabbed) {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent homeIntent = new Intent();
                homeIntent.setClassName("com.android.settings", "com.android.settings.Settings");
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
                return true;
            case R.id.pc_toggle_drawer:
                if (isDrawerOpen())
                    mDrawerLayout.closeDrawer(mFragmentContainerView);
                else
                    mDrawerLayout.openDrawer(mFragmentContainerView);
                return true;
            case R.id.pc_action_tabbed:
                mIsTabbed = !mIsTabbed;
                mPreferences.edit().putBoolean(PREF_IS_TABBED, mIsTabbed).commit();
                Helpers.restartPC(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean isThemeChanged() {
        /*final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        return is_light_theme != mIsLightTheme;*/
        return false;
    }

    @Override
    public void setTheme() {
        /*final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        mIsLightTheme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        setTheme(is_light_theme ? R.style.Theme_Light : R.style.Theme_Dark);*/
    }

    @Override
    public void onResume() {
        super.onResume();
        //if (isThemeChanged()) {
        //    Helpers.restartPC(this);
        //}
    }

    //==================================
    // Methods
    //==================================

    /**
     * Users of this fragment must call this method to set up the
     * navigation menu_drawer interactions.
     *
     * @param fragmentContainerView The view of this fragment in its activity's layout.
     * @param drawerLayout          The DrawerLayout containing this fragment's UI.
     */
    public void setUpNavigationDrawer(View fragmentContainerView, CustomDrawerLayout drawerLayout) {
        mFragmentContainerView = fragmentContainerView;
        mDrawerLayout = drawerLayout;

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
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
                    mUserLearnedDrawer = true;
                    mPreferences.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).commit();
                }

                getActivity().invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Remove or set it to true, if you want to use home to toggle the menu_drawer
        mDrawerToggle.setDrawerIndicatorEnabled(false);

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

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Restores the action bar after closing the menu_drawer
     */
    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getActivity().getTitle());
    }

    private ActionBar getActionBar() {
        return getActivity().getActionBar();
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
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        final int itemId = getPosition(position);

        if (mCurrentFragment != null) {
            transaction.detach(mCurrentFragment);
        }

        // Do we already have this fragment?
        String name = makeFragmentName(mFragmentContainer.getId(), itemId);
        mCurrentFragment = fragmentManager.findFragmentByTag(name);
        if (mCurrentFragment != null) {
            transaction.attach(mCurrentFragment);
        } else {
            mCurrentFragment = PlaceholderFragment.newInstance(itemId);
            transaction.add(mFragmentContainer.getId(), mCurrentFragment,
                    makeFragmentName(mFragmentContainer.getId(), itemId));
        }

        transaction.commit();
    }

    private static String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }
    /**
     * Depending on if the item is shown or not, it increases
     * the position to make the activity load the right fragment.
     *
     * @param pos The selected position
     * @return the modified position
     */
    public int getPosition(int pos) {
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
                if (pos >= 3) position = pos + 2;
                break;
        }
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
                            getString(R.string.t_wakelocks),
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
                            getString(R.string.t_wakelocks),
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
                            getString(R.string.t_wakelocks),
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
                            getString(R.string.t_wakelocks),
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
                            getString(R.string.t_wakelocks),
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
                            getString(R.string.t_wakelocks),
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
                            getString(R.string.t_wakelocks),
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
                            getString(R.string.t_wakelocks),
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
                case FRAGMENT_ID_WAKELOCKS:
                    fragment = new Wakelocks();
                    break;
            }

            return fragment;
        }

        public PlaceholderFragment() {
            // intentionally left blank
        }
    }

    //==================================
    // Adapters
    //==================================
    class TitleAdapter extends FragmentPagerAdapter {
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public TitleAdapter(FragmentManager fm) {
            super(fm);
            if (mVoltageExists) {
                if (Helpers.showBattery()) {
                    frags[0] = new CPUSettings();
                    frags[1] = new BatteryInfo();
                    frags[2] = new OOMSettings();
                    frags[3] = new VM();
                    frags[4] = new VoltageControlSettings();
                    frags[5] = new Advanced();
                    frags[6] = new TimeInState();
                    frags[7] = new Wakelocks();
                    frags[8] = new CPUInfo();
                    frags[9] = new DiskInfo();
                    if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        frags[10] = new Tools();
                    }
                } else {
                    frags[0] = new CPUSettings();
                    frags[1] = new OOMSettings();
                    frags[2] = new VM();
                    frags[3] = new VoltageControlSettings();
                    frags[4] = new Advanced();
                    frags[5] = new TimeInState();
                    frags[6] = new Wakelocks();
                    frags[7] = new CPUInfo();
                    frags[8] = new DiskInfo();
                    if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        frags[9] = new Tools();
                    }
                }
            } else {
                if (Helpers.showBattery()) {
                    frags[0] = new CPUSettings();
                    frags[1] = new BatteryInfo();
                    frags[2] = new OOMSettings();
                    frags[3] = new VM();
                    frags[4] = new Advanced();
                    frags[5] = new TimeInState();
                    frags[6] = new Wakelocks();
                    frags[7] = new CPUInfo();
                    frags[8] = new DiskInfo();
                    if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        frags[9] = new Tools();
                    }
                } else {
                    frags[0] = new CPUSettings();
                    frags[1] = new OOMSettings();
                    frags[2] = new VM();
                    frags[3] = new Advanced();
                    frags[4] = new TimeInState();
                    frags[5] = new Wakelocks();
                    frags[6] = new CPUInfo();
                    frags[7] = new DiskInfo();
                    if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
                        frags[8] = new Tools();
                    }
                }
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }
    }

    //==================================
    // Dialogs
    //==================================

    /**
     * Check if root access, and prompt the user to grant PC access
     */
    private void checkForSu() {
        if (Helpers.isSystemApp(getActivity())) {
            return;
        }

        boolean firstrun = mPreferences.getBoolean("firstrun", true);
        boolean rootWasCanceled = mPreferences.getBoolean("rootcanceled", false);

        if (firstrun || rootWasCanceled) {
            SharedPreferences.Editor e = mPreferences.edit();
            e.putBoolean("firstrun", false);
            e.commit();
            launchFirstRunDialog();
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
}

