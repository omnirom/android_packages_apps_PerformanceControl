package com.brewcrewfoo.performance.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private Item curItem;


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
                    sb.append("busybox rm -f ").append(o.getName()).append("/*;\n");
                }
                adapter.clear();
                linlaHeaderProgress.setVisibility(View.VISIBLE);
                tools.setVisibility(View.GONE);
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Helpers.shExec(sb,context);
                        mPreferences.edit().putLong(RESIDUAL_FILES,System.currentTimeMillis()).commit();
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
        packList.setAdapter(adapter);
        super.onConfigurationChanged(newConfig);
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
        curItem = adapter.getItem(position);
        try{
            Intent intent2 = new Intent(ResidualsActivity.this, iResidualsActivity.class);
            intent2.putExtra("dir",curItem.getName());
            startActivityForResult(intent2,1);
        }
        catch(Exception e){
            Log.e(TAG,"Error launching iResidualActivity activity");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                    int n= data.getIntExtra("result",0);
                    if(n>0){
                        String r[]=curItem.getDate().split(" ");
                        n=Integer.parseInt(r[0])-n;
                        if(n<=0){
                            adapter.remove(curItem);
                            adapter.notifyDataSetChanged();
                            if(adapter.isEmpty()){
                                nofiles.setVisibility(View.VISIBLE);
                                tools.setVisibility(View.GONE);
                            }
                        }
                        else{
                            adapter.setItem(curItem,n+" "+r[1]);
                        }
                        mPreferences.edit().putLong(RESIDUAL_FILES,System.currentTimeMillis()).commit();
                    }
            }
            //if (resultCode == RESULT_CANCELED) {}
        }
    }

    private class LongOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            CMDProcessor.CommandResult cr = null;
            cr=new CMDProcessor().su.runWaitFor(getFilesDir()+"/residual_files -c");
            if(cr.success()){return cr.stdout;}
            else{Log.d(TAG,"residual files err: "+cr.stderr); return null; }
        }

        @Override
        protected void onPostExecute(String result) {
            final List<Item> dir = new ArrayList<Item>();
            final String[] rinfos = res.getStringArray(R.array.residual_info);
            Log.d(TAG,"residual files: "+result);
            if(result!=null){
                final String fls[]=result.split(":");

                for(int i=0;i<fls.length;i++){
                    if(!fls[i].equals("0")){
                        dir.add(new Item(residualfiles[i],rinfos[i],fls[i]+" "+getString(R.string.filesstr),null,"dir"));
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

            final StringBuilder t = new StringBuilder();
            for (String residualfile : residualfiles) {
                t.append(residualfile);
                t.append(" ");
            }
            Helpers.get_assetsScript("residual_files",context,"DIRS=\""+t.toString()+"\";","");
            new CMDProcessor().su.runWaitFor("busybox chmod 750 "+getFilesDir()+"/residual_files" );
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


}
