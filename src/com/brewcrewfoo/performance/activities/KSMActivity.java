package com.brewcrewfoo.performance.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

/**
 * Created by h0rn3t on 11.09.2013.
 */
public class KSMActivity extends Activity implements Constants, SeekBar.OnSeekBarChangeListener, ActivityThemeChangeInterface {
    SharedPreferences mPreferences;
    private boolean mIsLightTheme;
    private TextView t1;
    private TextView t2;
    private TextView t3;
    private TextView t4;
    private TextView t5;
    private TextView tval1;
    private TextView tval2;
    private SeekBar mSleep;
    private SeekBar mPage2Scan;
    private CurThread mCurThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.ksm_settings);
        t1=(TextView)findViewById(R.id.t2);
        t2=(TextView)findViewById(R.id.t4);
        t3=(TextView)findViewById(R.id.t6);
        t4=(TextView)findViewById(R.id.t8);
        t5=(TextView)findViewById(R.id.t10);
        t1.setText(Helpers.readOneLine(KSM_PAGESSHARED_PATH));
        t2.setText(Helpers.readOneLine(KSM_PAGESUNSHERED_PATH));
        t3.setText(Helpers.readOneLine(KSM_PAGESSHARING_PATH));
        t4.setText(Helpers.readOneLine(KSM_PAGESVOLATILE_PATH));
        t5.setText(Helpers.readOneLine(KSM_FULLSCANS_PATH));

        final int v1=Integer.parseInt(Helpers.readOneLine(KSM_PAGESTOSCAN_PATH));
        final int v2=Integer.parseInt(Helpers.readOneLine(KSM_SLEEP_PATH));

        tval1=(TextView)findViewById(R.id.tval1);
        tval1.setText(getString(R.string.ksm_pagtoscan,v1));

        tval2=(TextView)findViewById(R.id.tval2);
        tval2.setText(getString(R.string.ksm_sleep,v2));

        mPage2Scan = (SeekBar) findViewById(R.id.val1);
        mPage2Scan.setOnSeekBarChangeListener(this);
        mPage2Scan.setMax(128);
        mPage2Scan.setProgress(v1/16);

        mSleep = (SeekBar) findViewById(R.id.val2);
        mSleep.setOnSeekBarChangeListener(this);
        mSleep.setMax(30);
        mSleep.setProgress(v2/100);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.val1) {
                tval1.setText(getString(R.string.ksm_pagtoscan,progress*16));
            }
            else if (seekBar.getId() == R.id.val2) {
                tval2.setText(getString(R.string.ksm_sleep,progress*100));
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
    @Override
    public void onResume() {
        if (mCurThread == null) {
            mCurThread = new CurThread();
            mCurThread.start();
        }
        super.onResume();
    }
    @Override
    public void onDestroy() {
        if (mCurThread != null) {
            if (mCurThread.isAlive()) {
                mCurThread.interrupt();
                try {
                    mCurThread.join();
                }
                catch (InterruptedException e) {
                }
            }
        }
        super.onDestroy();
    }
    @Override
    public boolean isThemeChanged() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        return is_light_theme != mIsLightTheme;
    }

    @Override
    public void setTheme() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        mIsLightTheme = is_light_theme;
        setTheme(is_light_theme ? R.style.Theme_Light : R.style.Theme_Dark);
    }

    protected class CurThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(800);
                    mCurHandler.sendMessage(mCurHandler.obtainMessage(0,null));
                }
            } catch (InterruptedException e) {
                //return;
            }
        }
    }
    protected Handler mCurHandler = new Handler() {
        public void handleMessage(Message msg) {
            t1.setText(Helpers.readOneLine(KSM_PAGESSHARED_PATH));
            t2.setText(Helpers.readOneLine(KSM_PAGESUNSHERED_PATH));
            t3.setText(Helpers.readOneLine(KSM_PAGESSHARING_PATH));
            t4.setText(Helpers.readOneLine(KSM_PAGESVOLATILE_PATH));
            t5.setText(Helpers.readOneLine(KSM_FULLSCANS_PATH));
        }
    };
}
