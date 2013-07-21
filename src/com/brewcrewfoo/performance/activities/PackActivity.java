package com.brewcrewfoo.performance.activities;

/**
 * Created by h0rn3t on 17.07.2013.
 */
import java.util.Arrays;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.pm.PackageManager;

import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.widget.Toast;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.PackAdapter;
import com.brewcrewfoo.performance.util.Constants;


public class PackActivity extends Activity implements Constants, OnItemClickListener,ActivityThemeChangeInterface {

    PackageManager packageManager;

    ListView packList;
    TextView packNames;
    Button applyBtn;
    SharedPreferences mPreferences;
    LinearLayout linlaHeaderProgress;
    LinearLayout linTools;
    LinearLayout linNopack;
    private boolean mIsLightTheme;
    private String pack_path;
    private String pack_pref;
    private String pmList[];
    private PackAdapter adapter;
    private Boolean tip;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.pack_list);
        pmList=new String[] {};
        Intent i=getIntent();
        tip=i.getBooleanExtra("mod",false);
        packageManager = getPackageManager();

        packNames=(TextView)  findViewById(R.id.procNames);
        if(tip){
            pack_path=USER_SYS_NAMES_PATH;
            pack_pref=PREF_SYS_NAMES;
            packNames.setHint(R.string.ps_sys_proc);
        }
        else{
            pack_path=USER_PROC_NAMES_PATH;
            pack_pref=PREF_USER_NAMES;
            packNames.setHint(R.string.ps_user_proc);
        }
        packNames.setText(mPreferences.getString(pack_pref,""));
        linlaHeaderProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
        linTools = (LinearLayout) findViewById(R.id.tools);
        linNopack = (LinearLayout) findViewById(R.id.noproc);

        packList = (ListView) findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        new LongOperation().execute();

        applyBtn=(Button) findViewById(R.id.applyBtn);
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mPreferences.edit().putString(pack_pref, packNames.getText().toString()).commit();
                new CMDProcessor().su.runWaitFor("busybox echo "+mPreferences.getString(pack_pref, Helpers.readOneLine(pack_path))+" > " + pack_path);
                finish();
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if(tip){
                CMDProcessor.CommandResult cr = null;
                cr=new CMDProcessor().sh.runWaitFor("busybox echo `pm list packages -s | cut -d':' -f2`");
                if(cr.success()&& !cr.stdout.equals("")){pmList =cr.stdout.split(" ");}
            }
            else{
                CMDProcessor.CommandResult cr = null;
                cr=new CMDProcessor().sh.runWaitFor("busybox echo `pm list packages -3 | cut -d':' -f2`");
                if(cr.success()&& !cr.stdout.equals("")){pmList =cr.stdout.split(" ");}
            }
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            adapter=new PackAdapter(PackActivity.this, pmList, packageManager );
            packList.setAdapter(adapter);
            linlaHeaderProgress.setVisibility(View.GONE);
            linTools.setVisibility(View.VISIBLE);
            if(adapter.getCount()<=0){
                linNopack.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPreExecute() {
            linlaHeaderProgress.setVisibility(View.VISIBLE);
            linTools.setVisibility(View.GONE);
            linNopack.setVisibility(View.GONE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long row) {
        final String told=packNames.getText().toString();
        final String pn= (String) parent.getItemAtPosition(position);
        if(told.equals("")){
            packNames.setText(pn);
        }
        else{
            String[] packlist=told.split(",");
            if(! Arrays.asList(packlist).contains(pn)){
                packNames.setText(told+","+pn);
            }
        }
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

}