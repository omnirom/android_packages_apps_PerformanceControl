/*
 * Performance Control - An Android CPU Control application Copyright (C)
 * Brandon Valosek, 2011 <bvalosek@gmail.com> Copyright (C) Modified by 2012
 * <James Roberts "xoomdevnet@gmail.com">
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Arrays;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.BatteryStats;
import android.os.BatteryStats.Uid;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.Constants;

public class Wakelocks extends Fragment implements Constants {
    private static final String TAG = "Wakelocks";
    private static String sRefFilename = "wakelockdata.ref";
    private static final int MAX_LIST_ITEMS = 10;
    private static final int TIME_PERIOD_BOOT = 42;
    private static final int TIME_PERIOD_RESET = 43;

    private LinearLayout mStatesView;
    private TextView mTotalStateTime;
    private TextView mKernelWakelockWarning;
    private boolean mUpdatingData = false;
    private Context mContext;
    private SharedPreferences mPreferences;
    private ShareActionProvider mProvider;
    private static IBatteryStats sBatteryStats;
    private long rawUptime;
    private long rawRealtime;
    private long batteryUptime;
    private long batteryRealtime;
    private long screenOnTime;
    private long sleepTime;

    private ArrayList<WakelockStats> mKernelWakelocks = new ArrayList<WakelockStats>();
    private ArrayList<WakelockStats> mUserWakelocks = new ArrayList<WakelockStats>();
    private static ArrayList<WakelockStats> sRefKernelWakelocks = new ArrayList<WakelockStats>();
    private static ArrayList<WakelockStats> sRefUserWakelocks = new ArrayList<WakelockStats>();

    private static long sRefRealTimestamp = 0;
    private static long sRefUpTimestamp = 0;
    private static int sWhich = TIME_PERIOD_BOOT;
    private Spinner mPeriodTypeSelect;
    private int mListType;
    private Spinner mListTypeSelect;
    private static boolean sKernelWakelockData = false;
    private boolean mShowAll;
    private CheckBox mShowAllSwitch;
    private StringBuffer mShareData;
    private static SparseArray<ArrayList<String>> sAppUidData;

    private static final int MENU_REFRESH = Menu.FIRST;
    private static final int MENU_SHARE = MENU_REFRESH + 1;

    private static final int[] WAKEUP_SOURCES_FORMAT = new int[] {
            Process.PROC_TAB_TERM | Process.PROC_OUT_STRING, // 0: name
            Process.PROC_TAB_TERM | Process.PROC_COMBINE
                    | Process.PROC_OUT_LONG, // 1: active count
            Process.PROC_TAB_TERM | Process.PROC_COMBINE, // 2: event count
            Process.PROC_TAB_TERM | Process.PROC_COMBINE, // 3: wakeup count
            Process.PROC_TAB_TERM | Process.PROC_COMBINE, // 4: expire count
            Process.PROC_TAB_TERM | Process.PROC_COMBINE, // 5: active since
            Process.PROC_TAB_TERM | Process.PROC_COMBINE
                    | Process.PROC_OUT_LONG,// 6: totalTime
            Process.PROC_TAB_TERM | Process.PROC_COMBINE, // 7: maxTime
            Process.PROC_TAB_TERM | Process.PROC_COMBINE, // 8: last change
            Process.PROC_TAB_TERM | Process.PROC_COMBINE
                    | Process.PROC_OUT_LONG, // 9: prevent suspend time
    };
    private static final String[] sProcWakelocksName = new String[3];
    private static final long[] sProcWakelocksData = new long[4];

    private static final String[] sSystemPackages = new String[] {
            "com.android.settings", "org.omnirom.torch",
            "com.android.location.fused", "com.android.providers.settings",
            "com.android.inputdevices", "android", "com.android.keychain",
            "com.android.systemui", "com.android.keyguard",
            "com.android.providers.downloads.ui",
            "com.android.providers.media", "com.android.providers.downloads" };
    private static final String[] sPhonePackages = new String[] {
            "com.android.phone", "com.android.providers.telephony",
            "com.android.stk" };
    private static final String[] sContactPackage = new String[] {
            "com.android.contacts", "com.android.providers.userdictionary",
            "com.android.providers.contacts" };

    static final class WakelockStats {
        final int mType; // 0 == kernel 1 == user
        final String mName;
        int mCount;
        long mTotalTime;
        long mPreventSuspendTime;
        int mUid;
        String mAppInfo;

        WakelockStats(int type, String name, int count, long totalTime,
                long preventSuspendTime, int uid, String appInfo) {
            mType = type;
            mName = name;
            mCount = count;
            mTotalTime = totalTime;
            mPreventSuspendTime = preventSuspendTime;
            mUid = uid;
            mAppInfo = appInfo;
        }

        @Override
        public String toString() {
            return mType + "||" + mName + "||" + mCount + "||" + mTotalTime
                    + "||" + mPreventSuspendTime;
        }
    }

    final Comparator<WakelockStats> wakelockStatsComparator = new Comparator<WakelockStats>() {
        @Override
        public int compare(WakelockStats lhs, WakelockStats rhs) {
            long lhsTime = lhs.mPreventSuspendTime;
            long rhsTime = rhs.mPreventSuspendTime;
            if (lhsTime < rhsTime) {
                return 1;
            }
            if (lhsTime > rhsTime) {
                return -1;
            }
            return 0;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (savedInstanceState != null) {
            mUpdatingData = savedInstanceState.getBoolean("updatingData");
            sWhich = savedInstanceState.getInt("which");
            mListType = savedInstanceState.getInt("listType");
        }

        setHasOptionsMenu(true);
        setRetainInstance(true);

        mProvider = new ShareActionProvider(mContext);

        buildUidAppsData();
        loadWakelockRef();
    }

    private static IBatteryStats getBatteryStats() {
        if (sBatteryStats == null) {
            sBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager
                    .getService(BatteryStats.SERVICE_NAME));
        }
        return sBatteryStats;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, root, savedInstanceState);

        View view = inflater.inflate(R.layout.wakelocks, root, false);

        mStatesView = (LinearLayout) view.findViewById(R.id.ui_states_view);
        mTotalStateTime = (TextView) view
                .findViewById(R.id.ui_total_state_time);
        mShowAllSwitch = (CheckBox) view.findViewById(R.id.ui_show_all_switch);

        mShowAll = mPreferences.getBoolean(PREF_WAKELOCKS_SHOW_ALL, false);
        mShowAllSwitch.setChecked(mShowAll);
        mShowAllSwitch
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        mShowAll = isChecked;
                        SharedPreferences.Editor editor = mPreferences.edit();
                        editor.putBoolean(PREF_WAKELOCKS_SHOW_ALL, mShowAll)
                                .commit();
                        updateView();
                    }
                });
        mPeriodTypeSelect = (Spinner) view
                .findViewById(R.id.period_type_select);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getActivity().getApplicationContext(),
                R.array.period_type_entries, R.layout.period_type_item);
        mPeriodTypeSelect.setAdapter(adapter);

        mPeriodTypeSelect
                .setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                            View view, int position, long id) {
                        if (position == 0) {
                            sWhich = TIME_PERIOD_BOOT;
                        } else if (position == 1) {
                            sWhich = TIME_PERIOD_RESET;
                        }
                        refreshData();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });

        mListTypeSelect = (Spinner) view.findViewById(R.id.list_type_select);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
                getActivity().getApplicationContext(),
                R.array.list_type_entries, R.layout.period_type_item);
        mListTypeSelect.setAdapter(adapter1);

        mListTypeSelect
                .setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                            View view, int position, long id) {
                        mListType = position;
                        refreshData();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });

        mKernelWakelockWarning = (TextView) view
                .findViewById(R.id.ui_kernel_wakelock_warning);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("updatingData", mUpdatingData);
        outState.putInt("which", sWhich);
        outState.putInt("listType", mListType);
    }

    @Override
    public void onResume() {
        refreshData();
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.wakelocks_menu, menu);

        menu.add(0, MENU_REFRESH, 0, R.string.mt_refresh)
                .setIcon(R.drawable.ic_menu_refresh)
                .setAlphabeticShortcut('r')
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(1, MENU_SHARE, 0, R.string.mt_share)
                .setAlphabeticShortcut('s')
                .setActionProvider(mProvider)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_REFRESH:
            refreshData();
            break;
        case R.id.reset:
            saveWakelockRef(mContext);
            refreshData();
            break;
        }

        return true;
    }

    public void updateView() {
        mStatesView.removeAllViews();

        if (!sKernelWakelockData) {
            mKernelWakelockWarning.setVisibility(View.VISIBLE);
            mTotalStateTime.setVisibility(View.GONE);
            mStatesView.setVisibility(View.GONE);
        } else {
            mKernelWakelockWarning.setVisibility(View.GONE);

            long totalTimeInSecs = microToMillis(batteryRealtime) / 1000;
            long totalUptimeInSecs = microToMillis(batteryUptime) / 1000;
            if (sWhich == TIME_PERIOD_BOOT) {
                totalTimeInSecs = rawRealtime / 1000;
                totalUptimeInSecs = rawUptime / 1000;
            } else if (sWhich == TIME_PERIOD_RESET) {
                totalTimeInSecs = (rawRealtime - sRefRealTimestamp) / 1000;
                totalUptimeInSecs = (rawUptime - sRefUpTimestamp) / 1000;
            }
            sleepTime = Math.max(totalTimeInSecs - totalUptimeInSecs, 0);

            mTotalStateTime.setText(toString(totalTimeInSecs));

            if (mListType == 0) {
                generateTimeRow(getResources().getString(R.string.awake_time),
                        totalUptimeInSecs, totalTimeInSecs, mStatesView);
                generateTimeRow(
                        getResources().getString(R.string.deep_sleep_time),
                        sleepTime, totalTimeInSecs, mStatesView);
            } else if (mListType == 1) {
                int i = 0;
                Iterator<WakelockStats> nextWakelock = mKernelWakelocks
                        .iterator();
                while (nextWakelock.hasNext()) {
                    WakelockStats entry = nextWakelock.next();
                    generateWakelockRow(entry.mName,
                            entry.mPreventSuspendTime / 1000, entry.mCount,
                            totalTimeInSecs, null, mStatesView);
                    i++;
                    if (!mShowAll && i > MAX_LIST_ITEMS) {
                        generateMoreRow(
                                String.valueOf(mKernelWakelocks.size()
                                        - MAX_LIST_ITEMS)
                                        + " "
                                        + getResources().getString(
                                                R.string.more_line_text),
                                mStatesView);
                        break;
                    }
                }

            } else if (mListType == 2) {
                int i = 0;
                Iterator<WakelockStats> nextWakelock = mUserWakelocks
                        .iterator();
                while (nextWakelock.hasNext()) {
                    WakelockStats entry = nextWakelock.next();
                    generateWakelockRow(entry.mName,
                            entry.mPreventSuspendTime / 1000, entry.mCount,
                            totalTimeInSecs, entry.mAppInfo, mStatesView);
                    i++;
                    if (!mShowAll && i > MAX_LIST_ITEMS) {
                        generateMoreRow(
                                String.valueOf(mUserWakelocks.size()
                                        - MAX_LIST_ITEMS)
                                        + " "
                                        + getResources().getString(
                                                R.string.more_line_text),
                                mStatesView);
                        break;
                    }
                }
            }
        }
        updateShareIntent(mShareData.toString());
    }

    public void refreshData() {
        if (!mUpdatingData) {
            new RefreshStateDataTask().execute((Void) null);
        }
    }

    private static String toString(long tSec) {
        long h = (long) Math.floor(tSec / (60 * 60));
        long m = (long) Math.floor((tSec - h * 60 * 60) / 60);
        long s = tSec % 60;
        String sDur;
        sDur = h + ":";
        if (m < 10)
            sDur += "0";
        sDur += m + ":";
        if (s < 10)
            sDur += "0";
        sDur += s;

        return sDur;
    }

    protected class RefreshStateDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... v) {
            load();
            return null;
        }

        @Override
        protected void onPreExecute() {
            mUpdatingData = true;
        }

        @Override
        protected void onPostExecute(Void v) {
            updateView();
            mUpdatingData = false;
        }
    }

    private void updateShareIntent(String data) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, data);
        mProvider.setShareIntent(shareIntent);
    }

    private static long computeWakeLock(BatteryStats.Timer timer,
            long realtime, int which) {
        if (timer != null) {
            // Convert from microseconds to milliseconds with rounding
            long totalTimeMicros = timer.getTotalTimeLocked(realtime, which);
            long totalTimeMillis = microToMillis(totalTimeMicros);
            return totalTimeMillis;
        }
        return 0;
    }

    private static long microToMillis(long microSecs) {
        return (microSecs + 500) / 1000;
    }

    private View generateTimeRow(String title, long duration, long totalTime,
            ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.time_row,
                parent, false);

        float per = 0f;
        String sPer = "";
        String sDur = "";
        long tSec = 0;

        if (duration != 0) {
            per = (float) duration * 100 / totalTime;
            if (per > 100f) {
                per = 0f;
            }
            tSec = duration;
        }
        sPer = String.format("%3d", (int) per) + "%";
        sDur = toString(tSec);

        TextView text = (TextView) view.findViewById(R.id.ui_text);
        TextView durText = (TextView) view.findViewById(R.id.ui_duration_text);
        TextView perText = (TextView) view
                .findViewById(R.id.ui_percentage_text);
        ProgressBar bar = (ProgressBar) view.findViewById(R.id.ui_bar);

        text.setText(title);
        perText.setText(sPer);
        durText.setText(sDur);
        bar.setProgress((int) per);

        parent.addView(view);
        return view;
    }

    private View generateWakelockRow(String title, long duration, int count,
            long totalTime, String appName, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.wakelock_row, parent, false);

        float per = 0f;
        String sPer = "";
        String sDur = "";
        long tSec = 0;

        if (duration != 0) {
            per = (float) duration * 100 / totalTime;
            if (per > 100f) {
                per = 0f;
            }
            tSec = duration;
        }
        sPer = String.format("%3d", (int) per) + "%";
        sDur = toString(tSec);

        TextView text = (TextView) view.findViewById(R.id.ui_text);
        TextView durText = (TextView) view.findViewById(R.id.ui_duration_text);
        TextView perText = (TextView) view
                .findViewById(R.id.ui_percentage_text);
        ProgressBar bar = (ProgressBar) view.findViewById(R.id.ui_bar);
        TextView countText = (TextView) view.findViewById(R.id.ui_count_text);
        TextView moreText = (TextView) view.findViewById(R.id.ui_more_text);

        text.setText(title);
        perText.setText(sPer);
        durText.setText(sDur);
        bar.setProgress((int) per);
        countText.setText(String.valueOf(count));

        if (appName != null) {
            moreText.setVisibility(View.VISIBLE);
            moreText.setText(appName);
        }
        parent.addView(view);
        return view;
    }

    private View generateMoreRow(String title, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.wakelock_row, parent, false);

        TextView text = (TextView) view.findViewById(R.id.ui_text);
        text.setText(title);
        TextView durText = (TextView) view.findViewById(R.id.ui_duration_text);
        durText.setVisibility(View.GONE);
        TextView perText = (TextView) view
                .findViewById(R.id.ui_percentage_text);
        perText.setVisibility(View.GONE);
        ProgressBar bar = (ProgressBar) view.findViewById(R.id.ui_bar);
        bar.setVisibility(View.GONE);

        parent.addView(view);
        return view;
    }

    public static void readKernelWakelockStats(
            ArrayList<WakelockStats> wakelockList) {

        FileInputStream is;
        byte[] buffer = new byte[8192];
        int len;

        try {
            try {
                is = new FileInputStream("/d/wakeup_sources");
            } catch (java.io.FileNotFoundException e) {
                return;
            }

            len = is.read(buffer);
            is.close();
        } catch (java.io.IOException e) {
            return;
        }

        if (len > 0) {
            int i;
            for (i = 0; i < len; i++) {
                if (buffer[i] == '\0') {
                    len = i;
                    break;
                }
            }
        }

        sKernelWakelockData = true;
        parseProcWakelocks(wakelockList, buffer, len);
    }

    private static void parseProcWakelocks(
            ArrayList<WakelockStats> wakelockList, byte[] wlBuffer, int len) {
        String name;
        int count;
        long totalTime;
        int startIndex;
        int endIndex;
        int numUpdatedWlNames = 0;
        long preventSuspendTime;

        // Advance past the first line.
        int i;
        for (i = 0; i < len && wlBuffer[i] != '\n' && wlBuffer[i] != '\0'; i++)
            ;
        startIndex = endIndex = i + 1;

        HashMap<String, WakelockStats> indexList = new HashMap<String, WakelockStats>();
        while (endIndex < len) {
            for (endIndex = startIndex; endIndex < len
                    && wlBuffer[endIndex] != '\n' && wlBuffer[endIndex] != '\0'; endIndex++)
                ;
            endIndex++; // endIndex is an exclusive upper bound.
            // Don't go over the end of the buffer, Process.parseProcLine might
            // write to wlBuffer[endIndex]
            if (endIndex >= (len - 1)) {
                return;
            }

            String[] nameStringArray = sProcWakelocksName;
            long[] wlData = sProcWakelocksData;
            // Stomp out any bad characters since this is from a circular buffer
            // A corruption is seen sometimes that results in the vm crashing
            // This should prevent crashes and the line will probably fail to
            // parse
            for (int j = startIndex; j < endIndex; j++) {
                if ((wlBuffer[j] & 0x80) != 0)
                    wlBuffer[j] = (byte) '?';
            }
            boolean parsed = Process.parseProcLine(wlBuffer, startIndex,
                    endIndex, WAKEUP_SOURCES_FORMAT, nameStringArray, wlData,
                    null);

            name = nameStringArray[0];
            count = (int) wlData[1];
            totalTime = wlData[2];
            preventSuspendTime = wlData[3];

            if (parsed && name.length() > 0 && count > 0) {
                WakelockStats foundEntry = indexList.get(name);
                if (foundEntry == null) {
                    foundEntry = new WakelockStats(0, name, count, totalTime,
                            preventSuspendTime, 0, null);
                    wakelockList.add(foundEntry);
                    indexList.put(name, foundEntry);
                } else {
                    foundEntry.mCount += count;
                    foundEntry.mTotalTime += totalTime;
                    foundEntry.mPreventSuspendTime += preventSuspendTime;
                }
            }
            startIndex = endIndex;
        }
    }

    private WakelockStats fromString(String line) {
        String[] parts = line.split("\\|\\|");
        try {
            return new WakelockStats(Integer.valueOf(parts[0]), parts[1],
                    Integer.valueOf(parts[2]), Long.valueOf(parts[3]),
                    Long.valueOf(parts[4]), -1, null);
        } catch (java.lang.NumberFormatException e) {
            return null;
        }
    }

    public static void clearKernelWakelockStatus(Context context) {
        sRefKernelWakelocks = new ArrayList<WakelockStats>();
        sRefUserWakelocks = new ArrayList<WakelockStats>();
        File file = new File(context.getFilesDir(), sRefFilename);
        file.delete();
        sRefRealTimestamp = 0;
        sRefUpTimestamp = 0;
    }

    private static void saveWakelockData(Context context, String fileName,
            ArrayList<WakelockStats> kernelData,
            ArrayList<WakelockStats> userData, boolean withRefTime) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            BufferedWriter buf = new BufferedWriter(new FileWriter(file));

            if (withRefTime) {
                buf.write(String.valueOf(sRefRealTimestamp));
                buf.newLine();
                buf.write(String.valueOf(sRefUpTimestamp));
                buf.newLine();
            }
            Iterator<WakelockStats> nextWakelock = kernelData.iterator();
            while (nextWakelock.hasNext()) {
                WakelockStats entry = nextWakelock.next();
                String wlLine = entry.toString();
                buf.write(wlLine);
                buf.newLine();
            }
            nextWakelock = userData.iterator();
            while (nextWakelock.hasNext()) {
                WakelockStats entry = nextWakelock.next();
                String wlLine = entry.toString();
                buf.write(wlLine);
                buf.newLine();
            }

            buf.flush();
            buf.close();
        } catch (java.io.IOException e) {
        }
    }

    private static void saveWakelockRef(Context context) {
        sRefKernelWakelocks = new ArrayList<WakelockStats>();
        sRefUserWakelocks = new ArrayList<WakelockStats>();
        readKernelWakelockStats(sRefKernelWakelocks);

        try {
            byte[] data = getBatteryStats().getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            BatteryStats batteryStats = com.android.internal.os.BatteryStatsImpl.CREATOR
                    .createFromParcel(parcel);

            readUserWakelockStats(batteryStats, sRefUserWakelocks);
        } catch (RemoteException e) {
        }

        sRefRealTimestamp = SystemClock.elapsedRealtime();
        sRefUpTimestamp = SystemClock.uptimeMillis();
        saveWakelockData(context, sRefFilename, sRefKernelWakelocks,
                sRefUserWakelocks, true);
    }

    private void loadWakelockData(String fileName,
            ArrayList<WakelockStats> kernelData,
            ArrayList<WakelockStats> userData, boolean withRefTime) {
        try {
            File file = new File(mContext.getFilesDir(), fileName);
            BufferedReader buf = new BufferedReader(new FileReader(file));

            String line = null;
            if (withRefTime) {
                line = buf.readLine();
                sRefRealTimestamp = Long.valueOf(line);
                line = buf.readLine();
                sRefUpTimestamp = Long.valueOf(line);
            }
            while ((line = buf.readLine()) != null) {
                WakelockStats wl = fromString(line);
                if (wl != null) {
                    if (wl.mType == 0) {
                        kernelData.add(wl);
                    } else {
                        userData.add(wl);
                    }
                }
            }
            buf.close();
        } catch (Exception e) {
            Log.d(TAG, "loadWakelockData:", e);
        }
    }

    private void loadWakelockRef() {
        sRefKernelWakelocks = new ArrayList<WakelockStats>();
        sRefUserWakelocks = new ArrayList<WakelockStats>();
        loadWakelockData(sRefFilename, sRefKernelWakelocks, sRefUserWakelocks,
                true);
    }

    private ArrayList<WakelockStats> diffToWakelockStatus(
            ArrayList<WakelockStats> refList, ArrayList<WakelockStats> list) {
        if (refList == null || refList.size() == 0) {
            return list;
        }
        HashMap<String, WakelockStats> indexList = new HashMap<String, WakelockStats>();
        ArrayList<WakelockStats> diffWakelocks = new ArrayList<WakelockStats>();

        Iterator<WakelockStats> nextWakelock = refList.iterator();
        while (nextWakelock.hasNext()) {
            WakelockStats entry = nextWakelock.next();
            indexList.put(entry.mName, entry);
        }
        nextWakelock = list.iterator();
        while (nextWakelock.hasNext()) {
            WakelockStats entry = nextWakelock.next();
            WakelockStats savedEntry = indexList.get(entry.mName);
            if (savedEntry != null) {
                int diffCount = entry.mCount - savedEntry.mCount;
                long diffTotalTime = entry.mTotalTime - savedEntry.mTotalTime;
                long diffPreventSuspendTime = entry.mPreventSuspendTime
                        - savedEntry.mPreventSuspendTime;
                if (diffCount > 0 && diffTotalTime > 0) {
                    WakelockStats newEntry = new WakelockStats(entry.mType,
                            entry.mName, diffCount, diffTotalTime,
                            diffPreventSuspendTime, entry.mUid, entry.mAppInfo);
                    diffWakelocks.add(newEntry);
                }
            } else {
                diffWakelocks.add(entry);
            }
        }
        return diffWakelocks;
    }

    private static void readUserWakelockStats(BatteryStats stats,
            ArrayList<WakelockStats> userData) {
        SparseArray<? extends BatteryStats.Uid> uidStats = stats.getUidStats();
        HashMap<String, WakelockStats> indexList = new HashMap<String, WakelockStats>();

        final int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            BatteryStats.Uid u = uidStats.valueAt(iu);
            int uid = u.getUid();
            Map<String, ? extends BatteryStats.Uid.Wakelock> wakelocks = u
                    .getWakelockStats();
            if (wakelocks.size() > 0) {
                for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> ent : wakelocks
                        .entrySet()) {
                    BatteryStats.Uid.Wakelock wl = ent.getValue();
                    // BatteryStats.Timer fullWakeTimer =
                    // wl.getWakeTime(WAKE_TYPE_FULL);

                    BatteryStats.Timer partialWakeTimer = wl
                            .getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
                    if (partialWakeTimer != null) {
                        long totalTimeMillis = computeWakeLock(
                                partialWakeTimer,
                                SystemClock.elapsedRealtime() * 1000, sWhich);
                        int count = partialWakeTimer.getCountLocked(sWhich);
                        if (totalTimeMillis > 0 && count > 0) {
                            WakelockStats foundEntry = indexList.get(ent
                                    .getKey());
                            if (foundEntry == null) {
                                String appInfo = null;
                                ArrayList<String> pkgs = sAppUidData.get(uid);
                                if (pkgs != null && pkgs.size() > 0) {
                                    appInfo = pkgs.get(0);
                                }
                                foundEntry = new WakelockStats(1, ent.getKey(),
                                        count, totalTimeMillis,
                                        totalTimeMillis, uid, appInfo);
                                userData.add(foundEntry);
                                indexList.put(ent.getKey(), foundEntry);
                            } else {
                                foundEntry.mCount += count;
                                foundEntry.mTotalTime += totalTimeMillis;
                                foundEntry.mPreventSuspendTime += totalTimeMillis;
                            }
                        }
                    }
                }
            }
        }
    }

    private void load() {
        try {
            mShareData = new StringBuffer();

            byte[] data = getBatteryStats().getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            BatteryStats batteryStats = com.android.internal.os.BatteryStatsImpl.CREATOR
                    .createFromParcel(parcel);

            rawUptime = SystemClock.uptimeMillis();
            rawRealtime = SystemClock.elapsedRealtime();
            batteryUptime = batteryStats.computeBatteryUptime(rawUptime * 1000,
                    sWhich);
            batteryRealtime = batteryStats.computeBatteryRealtime(
                    rawRealtime * 1000, sWhich);
            screenOnTime = batteryStats.getScreenOnTime(rawRealtime * 1000,
                    sWhich);

            mShareData.append("\n================\n");
            mShareData.append("Kernel wakelocks\n");
            mShareData.append("================\n");
            ArrayList<WakelockStats> allKernelWakelocks = new ArrayList<WakelockStats>();
            readKernelWakelockStats(allKernelWakelocks);

            mKernelWakelocks.clear();
            if (sWhich == TIME_PERIOD_RESET) {
                mKernelWakelocks.addAll(diffToWakelockStatus(
                        sRefKernelWakelocks, allKernelWakelocks));
            } else {
                mKernelWakelocks.addAll(allKernelWakelocks);
            }
            Collections.sort(mKernelWakelocks, wakelockStatsComparator);
            mShareData.append(mKernelWakelocks.toString());

            mShareData.append("\n================\n");
            mShareData.append("Wakelocks\n");
            mShareData.append("================\n");
            ArrayList<WakelockStats> allUserWakelocks = new ArrayList<WakelockStats>();
            readUserWakelockStats(batteryStats, allUserWakelocks);

            mUserWakelocks.clear();
            if (sWhich == TIME_PERIOD_RESET) {
                mUserWakelocks.addAll(diffToWakelockStatus(sRefUserWakelocks,
                        allUserWakelocks));
            } else {
                mUserWakelocks.addAll(allUserWakelocks);
            }
            Collections.sort(mUserWakelocks, wakelockStatsComparator);
            mShareData.append(mUserWakelocks.toString());

        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:", e);
        }
    }

    private void buildUidAppsData() {
        HashSet<String> systemPkgSet = new HashSet<String>(
                Arrays.asList(sSystemPackages));
        HashSet<String> phonePkgSet = new HashSet<String>(
                Arrays.asList(sPhonePackages));
        HashSet<String> contactPkgSet = new HashSet<String>(
                Arrays.asList(sContactPackage));

        List<ApplicationInfo> apps = mContext.getPackageManager()
                .getInstalledApplications(0);
        if (apps != null) {
            sAppUidData = new SparseArray<ArrayList<String>>();
            for (int i = 0; i < apps.size(); i++) {
                ApplicationInfo ai = apps.get(i);
                ArrayList<String> pkgs = sAppUidData.get(ai.uid);
                if (pkgs == null) {
                    pkgs = new ArrayList<String>();
                    sAppUidData.put(ai.uid, pkgs);
                }
                if (systemPkgSet.contains(ai.packageName)) {
                    pkgs.add("Android System");
                } else if (phonePkgSet.contains(ai.packageName)) {
                    pkgs.add("Phone");
                } else if (contactPkgSet.contains(ai.packageName)) {
                    pkgs.add("Contacts");
                }
                pkgs.add(ai.packageName);
            }
        }
    }
}
