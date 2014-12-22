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

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.CPUStateMonitor;
import com.brewcrewfoo.performance.util.CPUStateMonitor.CPUStateMonitorException;
import com.brewcrewfoo.performance.util.CPUStateMonitor.CpuState;
import com.brewcrewfoo.performance.util.Helpers;

import java.util.HashMap;
import java.util.Map;

import static com.brewcrewfoo.performance.util.Constants.*;

public class TimeInState extends Fragment {

    private LinearLayout mStatesView;
    private TextView mTotalStateTime;
    private TextView mStatesWarning;
    private CheckBox mStateMode;
    private boolean mUpdatingData = false;
    private CPUStateMonitor monitor = new CPUStateMonitor();
    private Context context;
    private SharedPreferences mPreferences;
    private boolean mOverallStats;
    private int mCpuNum;
    private boolean mActiveStateMode;
    private boolean mActiveCoreMode = true;
    private ShareActionProvider mProvider;
    private Spinner mPeriodTypeSelect;
    private LinearLayout mProgress;
    private CheckBox mCoreMode;
    private int mPeriodType = 1;
    private boolean sHasRefData;

    private static final int MENU_REFRESH = Menu.FIRST;
    private static final int MENU_SHARE = MENU_REFRESH + 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mOverallStats = monitor.hasOverallStats();
        mCpuNum = Helpers.getNumOfCpus();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPeriodType = mPreferences.getInt("which", 1);
        if (savedInstanceState != null) {
            mUpdatingData = savedInstanceState.getBoolean("updatingData");
            mPeriodType = savedInstanceState.getInt("which");
        }

        loadOffsets();

        setHasOptionsMenu(true);

        mProvider = new ShareActionProvider(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, root, savedInstanceState);

        View view = inflater.inflate(R.layout.time_in_state, root, false);

        mStatesView = (LinearLayout) view.findViewById(R.id.ui_states_view);
        mStatesWarning = (TextView) view.findViewById(R.id.ui_states_warning);
        mTotalStateTime = (TextView) view
                .findViewById(R.id.ui_total_state_time);

        mStateMode = (CheckBox) view.findViewById(R.id.ui_mode_switch);
        mActiveStateMode = mPreferences.getBoolean(PREF_STATE_MODE, false);
        mStateMode.setChecked(mActiveStateMode);
        mStateMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                mActiveStateMode = isChecked;
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(PREF_STATE_MODE, mActiveStateMode).commit();
                updateView();
            }
        });

        mCoreMode = (CheckBox) view.findViewById(R.id.ui_core_switch);
        if (mOverallStats) {
            mActiveCoreMode = mPreferences.getBoolean(PREF_CORE_MODE, true);
            mCoreMode.setChecked(mActiveCoreMode);
            mCoreMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                        boolean isChecked) {
                    mActiveCoreMode = isChecked;
                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putBoolean(PREF_CORE_MODE, mActiveCoreMode).commit();
                    updateView();
                }
            });
        } else {
            mCoreMode.setVisibility(View.GONE);
            mActiveCoreMode = false;
        }

        mPeriodTypeSelect = (Spinner) view
                .findViewById(R.id.period_type_select);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.period_type_entries, R.layout.period_type_item);
        mPeriodTypeSelect.setAdapter(adapter);

        mPeriodTypeSelect
                .setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                            View view, int position, long id) {
                        mPeriodType = position;
                        if (position == 0) {
                            loadOffsets();
                        } else if (position == 1) {
                            monitor.removeOffsets();
                        }
                        refreshData();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
        mPeriodTypeSelect.setSelection(mPeriodType);
        mProgress = (LinearLayout) view.findViewById(R.id.ui_progress);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("updatingData", mUpdatingData);
        outState.putInt("which", mPeriodType);
    }

    @Override
    public void onResume() {
        refreshData();
        super.onResume();
    }

    @Override
    public void onPause() {
        mPreferences.edit().putInt("which", mPeriodType).commit();
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.time_in_state_menu, menu);

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
            try {
                monitor.setOffsets();
            } catch (Exception e) {
                // not good
            }
            saveOffsets();
            if (mPeriodType == 1) {
                monitor.removeOffsets();
            }
            refreshData();
            break;
        }

        return true;
    }

    public void updateView() {
        Log.d(TAG, "updateView " + mUpdatingData);
        if (mUpdatingData) {
            return;
        }

        StringBuffer data = new StringBuffer();
        mStatesView.removeAllViews();

        if (monitor.getStates(0).size() == 0) {
            mStatesWarning.setVisibility(View.VISIBLE);
            mTotalStateTime.setVisibility(View.GONE);
            mStatesView.setVisibility(View.GONE);
        } else {
            if (mPeriodType == 0 && !sHasRefData) {
                mTotalStateTime.setText(getResources().getString(R.string.no_stat_because_reset));
            } else {
                long totTime = getStateTime(mActiveStateMode);
                data.append(totTime + "\n");
                totTime = totTime / 100;
                if (mActiveCoreMode) {
                    int cpu = 0;
                    for (CpuState state : monitor.getStates(0)) {
                        if (state.freq == 0) {
                            continue;
                        }
                        data.append(state.mCpu + " " + state.freq + " "
                                + state.getDuration() + "\n");
                        generateStateRowHeader(state, mStatesView);
                        generateStateRow(state, mStatesView);
                        for (cpu = 1; cpu < mCpuNum; cpu++) {
                            state = monitor.getFreqState(cpu, state.freq);
                            generateStateRow(state, mStatesView);
                            data.append(state.mCpu + " " + state.freq + " "
                                    + state.getDuration() + "\n");
                        }
                    }
                } else {
                    for (CpuState state : monitor.getStates(0)) {
                        if (state.freq == 0) {
                            continue;
                        }
                        generateStateRowHeader(state, mStatesView);
                        generateStateRow(state, mStatesView);
                        data.append(state.freq + " " + state.getDuration() + "\n");
                    }
                }

                if (!mActiveStateMode) {
                    CpuState deepSleepState = monitor.getDeepSleepState();
                    if (deepSleepState != null) {
                        generateStateRowHeader(deepSleepState, mStatesView);
                        generateStateRow(deepSleepState, mStatesView);
                        data.append(deepSleepState.freq + " "
                                + deepSleepState.getDuration() + "\n");
                    }
                }
                mTotalStateTime.setText(getResources().getString(R.string.total_time)
                        + " " + toString(totTime));
            }
        }
        updateShareIntent(data.toString());
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

    private View generateStateRow(CpuState state, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.state_row_line, parent, false);

        float per = 0f;
        String sPer = "";
        String sDur = "";
        String sCpu = " ";
        long tSec = 0;

        if (state != null) {
            long duration = state.getDuration();
            if (duration != 0) {
                per = (float) duration * 100 / getStateTime(mActiveStateMode);
                if (per > 100f) {
                    per = 0f;
                }
                tSec = duration / 100;
            }
            sPer = String.format("%3d", (int) per) + "%";
            sDur = toString(tSec);
            if (state.freq != 0 && mActiveCoreMode) {
                sCpu = String.valueOf(state.mCpu);
            }
        }

        TextView cpuText = (TextView) view.findViewById(R.id.ui_cpu_text);
        TextView durText = (TextView) view.findViewById(R.id.ui_duration_text);
        TextView perText = (TextView) view
                .findViewById(R.id.ui_percentage_text);
        ProgressBar bar = (ProgressBar) view.findViewById(R.id.ui_bar);

        cpuText.setText(sCpu);
        perText.setText(sPer);
        durText.setText(sDur);
        bar.setProgress((int) per);

        parent.addView(view);
        return view;
    }

    private View generateStateRowHeader(CpuState state, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.state_row_header, parent, false);

        String sFreq;
        if (state.freq == 0) {
            sFreq = getString(R.string.deep_sleep);
        } else {
            sFreq = state.freq / 1000 + " MHz";
        }

        TextView freqText = (TextView) view.findViewById(R.id.ui_freq_text);
        freqText.setText(sFreq);

        parent.addView(view);
        return view;
    }

    protected class RefreshStateDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... v) {
            try {
                monitor.updateStates();
            } catch (CPUStateMonitorException e) {
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
            mStatesView.setVisibility(View.GONE);
            mUpdatingData = true;
        }

        @Override
        protected void onPostExecute(Void v) {
            try {
                mProgress.setVisibility(View.GONE);
                mStatesView.setVisibility(View.VISIBLE);
                mUpdatingData = false;
                updateView();
            } catch(Exception e) {
            }
        }
    }

    public void loadOffsets() {
        String prefs = mPreferences.getString(PREF_OFFSETS, "");
        if (prefs == null || prefs.length() < 1) {
            return;
        }
        String[] cpus = prefs.split(":");
        if (cpus.length != mCpuNum) {
            return;
        }
        for (int cpu = 0; cpu < mCpuNum; cpu++) {
            String cpuData = cpus[cpu];
            Map<Integer, Long> offsets = new HashMap<Integer, Long>();
            String[] sOffsets = cpuData.split(",");
            for (String offset : sOffsets) {
                String[] parts = offset.split(" ");
                offsets.put(Integer.parseInt(parts[0]),
                        Long.parseLong(parts[1]));
            }
            monitor.setOffsets(cpu, offsets);
        }
        sHasRefData = true;
    }

    public void saveOffsets() {
        SharedPreferences.Editor editor = mPreferences.edit();
        String str = "";
        for (int cpu = 0; cpu < mCpuNum; cpu++) {
            for (Map.Entry<Integer, Long> entry : monitor.getOffsets(cpu)
                    .entrySet()) {
                str += entry.getKey() + " " + entry.getValue() + ",";
            }
            str += ":";
        }
        editor.putString(PREF_OFFSETS, str).commit();
        sHasRefData = true;
    }

    public void clarOffsets() {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_OFFSETS, "").commit();
        sHasRefData = false;
    }

    private long getStateTime(boolean activeMode) {
        long total = monitor.getTotalStateTime(0, true);
        if (activeMode) {
            CpuState deepSleepState = monitor.getDeepSleepState();
            return total - deepSleepState.getDuration();
        }
        return total;
    }

    public void clearOffsets() {
        monitor.removeOffsets();
        saveOffsets();
    }

    private void updateShareIntent(String data) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, data);
        mProvider.setShareIntent(shareIntent);
    }
}
