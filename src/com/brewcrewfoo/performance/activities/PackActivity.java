package com.brewcrewfoo.performance.activities;

/**
 * Created by h0rn3t on 17.07.2013.
 */
import java.util.Arrays;

import android.os.Bundle;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.widget.Button;
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
    private boolean mIsLightTheme;
    private String pack_path;
    private String pack_pref;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.pack_list);


        String[] pmList;

        if(mPreferences.getInt("MOD",0)==1){
            pack_path=USER_SYS_NAMES_PATH;
            pack_pref=PREF_SYS_NAMES;
            CMDProcessor.CommandResult cr = null;
            cr=new CMDProcessor().sh.runWaitFor("busybox echo `pm list packages -s | cut -d':' -f2`");
            pmList =cr.stdout.split(" ");
        }
        else{
            pack_path=USER_PROC_NAMES_PATH;
            pack_pref=PREF_USER_NAMES;
            CMDProcessor.CommandResult cr = null;
            cr=new CMDProcessor().sh.runWaitFor("busybox echo `pm list packages -3 | cut -d':' -f2`");
            pmList =cr.stdout.split(" ");
        }

        packList = (ListView) findViewById(R.id.applist);
        packageManager = getPackageManager();
        packList.setAdapter(new PackAdapter(this, pmList, packageManager ));
        packList.setOnItemClickListener(this);

        packNames=(TextView)  findViewById(R.id.procNames);
        packNames.setText(mPreferences.getString(pack_pref,""));

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