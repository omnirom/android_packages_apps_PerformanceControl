/*
 * Performance Control - An Android CPU Control application Copyright (C)
 * Brandon Valosek, 2011 <bvalosek@gmail.com> Copyright (C) Modified by 2012
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

package com.brewcrewfoo.performance.util;

import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@SuppressLint("UseSparseArrays")
public class CPUStateMonitor implements Constants {

    private List<CpuState> mStates = new ArrayList<CpuState>();
    private Map<Integer, Long> mOffsets = new HashMap<Integer, Long>();

    @SuppressWarnings("serial")
    public class CPUStateMonitorException extends Exception {
        public CPUStateMonitorException(String s) {
            super(s);
        }
    }

    //@SuppressLint({"UseValueOf", "UseValueOf"})
    public class CpuState implements Comparable<CpuState> {
        public CpuState(int a, long b) {
            freq = a;
            duration = b;
        }

        public int freq = 0;
        public long duration = 0;

        public int compareTo(CpuState state) {
            Integer a = freq;
            Integer b = state.freq;
            return a.compareTo(b);
        }
    }

    public List<CpuState> getStates() {
        List<CpuState> states = new ArrayList<CpuState>();

        for (CpuState state : mStates) {
            long duration = state.duration;
            if (mOffsets.containsKey(state.freq)) {
                long offset = mOffsets.get(state.freq);
                if (offset <= duration) {
                    duration -= offset;
                } else {
                    mOffsets.clear();
                    return getStates();
                }
            }
            states.add(new CpuState(state.freq, duration));
        }
        return states;
    }

    public long getTotalStateTime() {
        long sum = 0;
        long offset = 0;

        for (CpuState state : mStates) {
            sum += state.duration;
        }

        for (Map.Entry<Integer, Long> entry : mOffsets.entrySet()) {
            offset += entry.getValue();
        }
        return sum - offset;
    }

    public Map<Integer, Long> getOffsets() {
        return mOffsets;
    }

    public void setOffsets(Map<Integer, Long> offsets) {
        mOffsets = offsets;
    }

    public void setOffsets() throws CPUStateMonitorException {
        mOffsets.clear();
        updateStates();

        for (CpuState state : mStates) {
            mOffsets.put(state.freq, state.duration);
        }
    }

    public void removeOffsets() {
        mOffsets.clear();
    }

    public List<CpuState> updateStates() throws CPUStateMonitorException {
        try {
            InputStream is = new FileInputStream(TIME_IN_STATE_PATH);
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);
            mStates.clear();
            readInStates(br);
            is.close();
        } catch (IOException e) {
            throw new CPUStateMonitorException(
                    "Problem opening time-in-states file");
        }

        long sleepTime = (SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()) / 10;
        mStates.add(new CpuState(0, sleepTime));

        Collections.sort(mStates, Collections.reverseOrder());

        return mStates;
    }

    private void readInStates(BufferedReader br)
            throws CPUStateMonitorException {
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nums = line.split(" ");
                mStates.add(new CpuState(Integer.parseInt(nums[0]), Long.parseLong(nums[1])));
            }
        } catch (IOException e) {
            throw new CPUStateMonitorException(
                    "Problem processing time-in-states file");
        }
    }
}
