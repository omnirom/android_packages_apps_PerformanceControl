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

import static com.brewcrewfoo.performance.util.Constants.*;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.GovSetActivity;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CPUSettings extends Fragment implements SeekBar.OnSeekBarChangeListener {

    private SeekBar mMaxSlider;
    private SeekBar mMinSlider;
    private Spinner mGovernor;
    private Spinner mIo;
    private TextView mMaxSpeedText;
    private TextView mMinSpeedText;
    private String[] mAvailableFrequencies;
    private String mMaxFreqSetting;
    private String mMinFreqSetting;
    private CurCPUThread mCurCPUThread;
    private SharedPreferences mPreferences;
    private boolean mIsTegra3 = false;
    private boolean mIsDynFreq = false;

    private Context context;
    private int mCpuNum = 1;
    private CpuInfoListAdapter mCpuInfoListAdapter;
    private List<String> mCpuInfoListData;
    private LayoutInflater mInflater;

    public class CpuInfoListAdapter extends ArrayAdapter<String> {

        public CpuInfoListAdapter(Context context, int resource, List<String> values) {
            super(context, R.layout.cpu_info_item, resource, values);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = mInflater.inflate(R.layout.cpu_info_item, parent, false);
            TextView cpuInfoCore = (TextView) rowView.findViewById(R.id.cpu_info_core);
            TextView cpuInfoFreq = (TextView) rowView.findViewById(R.id.cpu_info_freq);
            cpuInfoCore.setText(getString(R.string.core) + " " + String.valueOf(position) + ": ");
            cpuInfoFreq.setText(mCpuInfoListData.get(position));
            return rowView;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        mInflater = inflater;
        View view = mInflater.inflate(R.layout.cpu_settings, root, false);
        final float density = getResources().getDisplayMetrics().density;

        mCpuNum = Helpers.getNumOfCpus();

        mCpuInfoListData = new ArrayList<String>(mCpuNum);
        for (int i = 0; i < mCpuNum; i++) {
            mCpuInfoListData.add("Core " + String.valueOf(i) + ": ");
        }

        mCpuInfoListAdapter = new CpuInfoListAdapter(
                context, android.R.layout.simple_list_item_1, mCpuInfoListData);

        ListView mCpuInfoList = (ListView) view.findViewById(R.id.cpu_info_list);
        mCpuInfoList.setAdapter(mCpuInfoListAdapter);
        
        ViewGroup.LayoutParams lstViewParams = (LayoutParams) mCpuInfoList.getLayoutParams();               
        int listHeigt = Math.round(25 * density)  * mCpuNum;       
        lstViewParams.height = listHeigt;

        mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();
        mIsDynFreq = new File(DYN_MAX_FREQ_PATH).exists() && new File(DYN_MIN_FREQ_PATH).exists();
        mAvailableFrequencies = new String[0];

        String availableFrequenciesLine = Helpers.readOneLine(STEPS_PATH);
        if (availableFrequenciesLine != null) {
            mAvailableFrequencies = availableFrequenciesLine.split(" ");
            Arrays.sort(mAvailableFrequencies, new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                    return Integer.valueOf(object1).compareTo(Integer.valueOf(object2));
                }
            });
        }

        int mFrequenciesNum = mAvailableFrequencies.length - 1;
        String[] mAvailableGovernors = Helpers.readOneLine(GOVERNORS_LIST_PATH).split(" ");
        String[] mAvailableIo = Helpers.getAvailableIOSchedulers();

        String mCurrentGovernor = Helpers.readOneLine(GOVERNOR_PATH);
        String mCurrentIo = Helpers.getIOScheduler();
        String mCurMaxSpeed;
        String mCurMinSpeed;
        if (new File(DYN_MAX_FREQ_PATH).exists()) {
            mCurMaxSpeed = Helpers.readOneLine(DYN_MAX_FREQ_PATH);
        } else {
            mCurMaxSpeed = Helpers.readOneLine(MAX_FREQ_PATH);
        }
        if (new File(DYN_MIN_FREQ_PATH).exists()) {
            mCurMinSpeed = Helpers.readOneLine(DYN_MIN_FREQ_PATH);
        } else {
            mCurMinSpeed = Helpers.readOneLine(MIN_FREQ_PATH);
        }

        if (mIsTegra3) {
            String curTegraMaxSpeed = Helpers.readOneLine(TEGRA_MAX_FREQ_PATH);
            int curTegraMax;
            try {
                curTegraMax = Integer.parseInt(curTegraMaxSpeed);
                if (curTegraMax > 0) {
                    mCurMaxSpeed = Integer.toString(curTegraMax);
                }
            } catch (NumberFormatException ignored) {
                // Nothing to do
            }
        }

        mMaxSlider = (SeekBar) view.findViewById(R.id.max_slider);
        mMaxSlider.setMax(mFrequenciesNum);
        mMaxSpeedText = (TextView) view.findViewById(R.id.max_speed_text);
        mMaxSpeedText.setText(Helpers.toMHz(mCurMaxSpeed));
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mCurMaxSpeed));
        mMaxFreqSetting = mCurMaxSpeed;
        mMaxSlider.setOnSeekBarChangeListener(this);

        mMinSlider = (SeekBar) view.findViewById(R.id.min_slider);
        mMinSlider.setMax(mFrequenciesNum);
        mMinSpeedText = (TextView) view.findViewById(R.id.min_speed_text);
        mMinSpeedText.setText(Helpers.toMHz(mCurMinSpeed));
        mMinSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mCurMinSpeed));
        mMinFreqSetting = mCurMinSpeed;
        mMinSlider.setOnSeekBarChangeListener(this);


        mGovernor = (Spinner) view.findViewById(R.id.pref_governor);
        ArrayAdapter<CharSequence> governorAdapter = new ArrayAdapter<CharSequence>(
                context, android.R.layout.simple_spinner_item);
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String mAvailableGovernor : mAvailableGovernors) {
            governorAdapter.add(mAvailableGovernor);
        }
        mGovernor.setAdapter(governorAdapter);
        mGovernor.setSelection(Arrays.asList(mAvailableGovernors).indexOf(mCurrentGovernor));
        mGovernor.post(new Runnable() {
            public void run() {
                mGovernor.setOnItemSelectedListener(new GovListener());
            }
        });

        mIo = (Spinner) view.findViewById(R.id.pref_io);
        ArrayAdapter<CharSequence> ioAdapter = new ArrayAdapter<CharSequence>(
                context, android.R.layout.simple_spinner_item);
        ioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String aMAvailableIo : mAvailableIo) {
            ioAdapter.add(aMAvailableIo);
        }
        mIo.setAdapter(ioAdapter);
        mIo.setSelection(Arrays.asList(mAvailableIo).indexOf(mCurrentIo));
        mIo.post(new Runnable() {
            public void run() {
                mIo.setOnItemSelectedListener(new IOListener());
            }
        });

        CheckBox mSetOnBoot = (CheckBox) view.findViewById(R.id.cpu_sob);
        mSetOnBoot.setChecked(mPreferences.getBoolean(CPU_SOB, false));
        mSetOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(CPU_SOB, checked);
                if (checked) {
                    editor.putString(PREF_MIN_CPU, Helpers.readOneLine(MIN_FREQ_PATH));
                    editor.putString(PREF_MAX_CPU, Helpers.readOneLine(MAX_FREQ_PATH));
                    editor.putString(PREF_GOV, Helpers.readOneLine(GOVERNOR_PATH));
                    editor.putString(PREF_IO, Helpers.getIOScheduler());
                }
                editor.commit();
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
            inflater.inflate(R.menu.cpu_settings_menu, menu);
        } else {
            //inflater.inflate(R.menu.cpu_settings_menu_simple, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_settings:
                Intent intent = new Intent(context, PCSettings.class);
                startActivity(intent);
                break;
            case R.id.gov_settings:
                intent = new Intent(context, GovSetActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.max_slider) {
                setMaxSpeed(seekBar, progress);
            } else if (seekBar.getId() == R.id.min_slider) {
                setMinSpeed(seekBar, progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (Helpers.isSystemApp(getActivity())) {
            for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
                Helpers.writeOneLine(MAX_FREQ_PATH.replace("cpu0", "cpu" + i), mMaxFreqSetting);
                Helpers.writeOneLine(MIN_FREQ_PATH.replace("cpu0", "cpu" + i), mMinFreqSetting);
            }
            if (mIsTegra3) {
                Helpers.writeOneLine(TEGRA_MAX_FREQ_PATH, mMaxFreqSetting);
            }
            if (mIsDynFreq) {
                Helpers.writeOneLine(DYN_MAX_FREQ_PATH, mMaxFreqSetting);
                Helpers.writeOneLine(DYN_MIN_FREQ_PATH, mMinFreqSetting);
            }
        } else {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
                sb.append("busybox echo ").append(mMaxFreqSetting).append(" > ")
                        .append(MAX_FREQ_PATH.replace("cpu0", "cpu" + i)).append(";\n");
                sb.append("busybox echo ").append(mMinFreqSetting).append(" > ")
                        .append(MIN_FREQ_PATH.replace("cpu0", "cpu" + i)).append(";\n");
            }
            if (mIsTegra3) {
                sb.append("busybox echo ").append(mMaxFreqSetting).append(" > ")
                        .append(TEGRA_MAX_FREQ_PATH).append(";\n");
            }
            if (mIsDynFreq) {
                sb.append("busybox echo ").append(mMaxFreqSetting).append(" > ")
                        .append(DYN_MAX_FREQ_PATH).append(";\n");
                sb.append("busybox echo ").append(mMinFreqSetting).append(" > ")
                        .append(DYN_MIN_FREQ_PATH).append(";\n");
            }
            Helpers.shExec(sb, context, true);
        }
    }

    public class GovListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            final StringBuilder sb = new StringBuilder();
            String selected = parent.getItemAtPosition(pos).toString();
            for (int i = 0; i < Helpers.getNumOfCpus(); i++) {
                if (Helpers.isSystemApp(getActivity())) {
                    Helpers.writeOneLine(GOVERNOR_PATH.replace("cpu0", "cpu" + i), selected);
                } else {
                    sb.append("busybox echo ").append(selected).append(" > ")
                            .append(GOVERNOR_PATH.replace("cpu0", "cpu" + i)).append(";\n");
                }
            }
            updateSharedPrefs(PREF_GOV, selected);
            mPreferences.edit().remove(GOV_SETTINGS).remove(GOV_NAME).apply();
            if (!Helpers.isSystemApp(getActivity())) {
                Helpers.shExec(sb, context, true);
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    public class IOListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String selected = parent.getItemAtPosition(pos).toString();
            final StringBuilder sb = new StringBuilder();
            for (String aIO_SCHEDULER_PATH : IO_SCHEDULER_PATH) {
                if (new File(aIO_SCHEDULER_PATH).exists()) {
                    if (Helpers.isSystemApp(getActivity())) {
                        Helpers.writeOneLine(aIO_SCHEDULER_PATH, selected);
                    } else {
                        sb.append("busybox echo ").append(selected).append(" > ")
                                .append(aIO_SCHEDULER_PATH).append(";\n");
                    }
                }
            }
            if (!Helpers.isSystemApp(getActivity())) {
                Helpers.shExec(sb, context, true);
            }
            updateSharedPrefs(PREF_IO, selected);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    @Override
    public void onResume() {
        if (mCurCPUThread == null) {
            mCurCPUThread = new CurCPUThread();
            mCurCPUThread.start();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        Helpers.updateAppWidget(context);
        super.onPause();

        if (mCurCPUThread != null) {
            if (mCurCPUThread.isAlive()) {
                mCurCPUThread.interrupt();
                try {
                    mCurCPUThread.join();
                } catch (InterruptedException e) {
                }
            }

            mCurCPUThread = null;
        }
    }

    public void setMaxSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int minSliderProgress = mMinSlider.getProgress();
        if (progress <= minSliderProgress) {
            mMinSlider.setProgress(progress);
            mMinSpeedText.setText(Helpers.toMHz(current));
            mMinFreqSetting = current;
        }
        mMaxSpeedText.setText(Helpers.toMHz(current));
        mMaxFreqSetting = current;
        updateSharedPrefs(PREF_MAX_CPU, current);
    }

    public void setMinSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int maxSliderProgress = mMaxSlider.getProgress();
        if (progress >= maxSliderProgress) {
            mMaxSlider.setProgress(progress);
            mMaxSpeedText.setText(Helpers.toMHz(current));
            mMaxFreqSetting = current;
        }
        mMinSpeedText.setText(Helpers.toMHz(current));
        mMinFreqSetting = current;
        updateSharedPrefs(PREF_MIN_CPU, current);
    }

    protected class CurCPUThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    List<String> freqs = new ArrayList<String>();
                    for (int i = 0; i < mCpuNum; i++) {
                        String cpuFreq = CPU_PATH + String.valueOf(i) + CPU_FREQ_TAIL;
                        String curFreq = "0";
                        if (Helpers.fileExists(cpuFreq)) {
                            curFreq = Helpers.readOneLine(cpuFreq);
                        }
                        freqs.add(curFreq);
                    }
                    String[] freqArray = freqs.toArray(new String[freqs.size()]);
                    mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0, freqArray));
                }
            } catch (InterruptedException e) {
                //return;
            }
        }
    }


    protected Handler mCurCPUHandler = new Handler() {
        public void handleMessage(Message msg) {
            String[] freqArray = (String[]) msg.obj;
            for (int i = 0; i < freqArray.length; i++) {
                // Convert freq in MHz
                try {
                    int freqHz = Integer.parseInt(freqArray[i]);

                    if (freqHz == 0) {
                        mCpuInfoListData.set(i, getString(R.string.core_offline));
                    } else {
                        mCpuInfoListData.set(i, Integer.toString(freqHz / 1000) + " MHz");
                    }
                } catch (Exception e) {
                    // Do nothing
                }
            }
            mCpuInfoListAdapter.notifyDataSetChanged();
        }
    };

    private void updateSharedPrefs(String var, String value) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(var, value).commit();
    }
}

