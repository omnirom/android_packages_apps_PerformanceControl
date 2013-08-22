package com.brewcrewfoo.performance.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.FileArrayAdapter;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Item;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by h0rn3t on 31.07.2013.
 */
public class ResidualsActivity extends Activity implements Constants, AdapterView.OnItemClickListener, ActivityThemeChangeInterface {
    SharedPreferences mPreferences;
    private boolean mIsLightTheme;
    private FileArrayAdapter adapter;

    Resources res;
    Context context;
    ListView packList;
    LinearLayout linlaHeaderProgress;
    LinearLayout nofiles;
    LinearLayout tools;
    Button applyBtn;
    private int poz;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();
        setTheme();
        setContentView(R.layout.residual_list);
        packList = (ListView) findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        linlaHeaderProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
        nofiles = (LinearLayout) findViewById(R.id.nofiles);
        tools = (LinearLayout) findViewById(R.id.tools);
        applyBtn=(Button) findViewById(R.id.applyBtn);
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final StringBuilder sb = new StringBuilder();
                for(int i=0;i<adapter.getCount();i++){
                    final Item o = adapter.getItem(i);
                    sb.append("busybox rm -f "+o.getName()+"/*;\n");
                }
                adapter.clear();

                linlaHeaderProgress.setVisibility(View.VISIBLE);
                tools.setVisibility(View.GONE);
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Helpers.shExec(sb);
                        finish();
                    }
                };
                new Thread(runnable).start();

            }
        });
        new LongOperation().execute();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        packList.setAdapter(adapter);
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
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long row) {
        final Item o = adapter.getItem(position);
        poz=position;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.residual_files_title))
                .setMessage(getString(R.string.clean_files_msg,o.getName()))
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                .setPositiveButton(getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        ;
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        //alertDialog.setCancelable(false);
        Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        theButton.setOnClickListener(new CustomListener(alertDialog));
    }

    class CustomListener implements View.OnClickListener {
        private final Dialog dialog;
        public CustomListener(Dialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v) {
            final Item o = adapter.getItem(poz);
            ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
            CMDProcessor.CommandResult cr = null;
            cr=new CMDProcessor().su.runWaitFor("busybox rm -f "+o.getName()+"/*");
            if(cr.success()){
                adapter.remove(o);
                adapter.notifyDataSetChanged();
                if(adapter.isEmpty()){
                    nofiles.setVisibility(View.VISIBLE);
                    tools.setVisibility(View.GONE);
                }
            }
            dialog.cancel();
        }
    }


    private class LongOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            CMDProcessor.CommandResult cr = null;
            cr=new CMDProcessor().su.runWaitFor(SH_PATH);
            if(cr.success()){return cr.stdout;}
            else{ return null;}
        }

        @Override
        protected void onPostExecute(String result) {
            final List<Item> dir = new ArrayList<Item>();
            final String[] rinfos = res.getStringArray(R.array.residual_info);
            if(result!=null){
                final String fls[]=result.split(":");

                for(int i=0;i< residualfiles.length;i++){
                        if(!fls[i].equals("0")){
                            dir.add(new Item(residualfiles[i],rinfos[i],fls[i]+" "+getString(R.string.filesstr),"","dir"));
                        }
                }
            }
            linlaHeaderProgress.setVisibility(View.GONE);
            if(dir.isEmpty()){
                nofiles.setVisibility(View.VISIBLE);
                tools.setVisibility(View.GONE);
            }
            else{
                nofiles.setVisibility(View.GONE);
                tools.setVisibility(View.VISIBLE);
                adapter = new FileArrayAdapter(ResidualsActivity.this,R.layout.file_view, dir);
                packList.setAdapter(adapter);
            }

        }

        @Override
        protected void onPreExecute() {
            linlaHeaderProgress.setVisibility(View.VISIBLE);
            nofiles.setVisibility(View.GONE);
            tools.setVisibility(View.GONE);
            final StringBuffer t=new StringBuffer();
            for(int i=0;i<residualfiles.length;i++){
                t.append(residualfiles[i]);
                t.append(" ");
            }
            Helpers.get_assetsFile("count_files",context,"DIRS=\""+t.toString()+"\";\n\n");
            new CMDProcessor().su.runWaitFor("busybox cat "+ISTORAGE+"count_files > " + SH_PATH );
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


}
