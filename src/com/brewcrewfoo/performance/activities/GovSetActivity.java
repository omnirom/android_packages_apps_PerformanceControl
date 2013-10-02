package com.brewcrewfoo.performance.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Prop;
import com.brewcrewfoo.performance.util.PropAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by h0rn3t on 21.09.2013.
 */
public class GovSetActivity extends Activity implements Constants, AdapterView.OnItemClickListener, ActivityThemeChangeInterface {
    private boolean mIsLightTheme;
    SharedPreferences mPreferences;
    private final Context context=this;
    Resources res;
    private ListView packList;
    private LinearLayout linlaHeaderProgress;
    private LinearLayout nofiles;
    private RelativeLayout tools;
    private PropAdapter adapter;
    private String curgov;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();
        setTheme();
        setContentView(R.layout.prop_view);

        packList = (ListView) findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        linlaHeaderProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
        nofiles = (LinearLayout) findViewById(R.id.nofiles);
        tools = (RelativeLayout) findViewById(R.id.tools);
        Button applyBtn = (Button) findViewById(R.id.applyBtn);
        final Switch setOnBoot = (Switch) findViewById(R.id.applyAtBoot);
        setOnBoot.setChecked(mPreferences.getBoolean(GOV_SOB, false));
        curgov=Helpers.readOneLine(GOVERNOR_PATH);
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final StringBuilder sb = new StringBuilder();
                final StringBuilder sbs = new StringBuilder();
                for(int i=0;i<adapter.getCount();i++){
                    Prop p=adapter.getItem(i);
                    if(i==0){
                        sb.append(p.getName()).append(":").append(p.getVal());
                    }
                    else{
                        sb.append(";").append(p.getName()).append(":").append(p.getVal());
                    }
                    sbs.append("busybox echo ").append(p.getVal()).append(" > ").append(GOV_SETTINGS_PATH).append(curgov).append("/").append(p.getName()).append(";\n");
                }
                mPreferences.edit().putString(GOV_NAME,curgov).putString(GOV_SETTINGS, sb.toString()).commit();
                Helpers.shExec(sbs,context,true);
            }
        });

        setOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    final StringBuilder sb = new StringBuilder();
                    for(int i=0;i<adapter.getCount();i++){
                        Prop p=adapter.getItem(i);
                        if (i == 0) {
                            sb.append(p.getName()).append(":").append(p.getVal());

                        } else {
                            sb.append(";").append(p.getName()).append(":").append(p.getVal());

                        }
                    }
                    mPreferences.edit().putString(GOV_NAME,curgov).putString(GOV_SETTINGS, sb.toString()).apply();
                }
                else{
                    mPreferences.edit().remove(GOV_SETTINGS).remove(GOV_NAME).apply();
                }
                mPreferences.edit().putBoolean(GOV_SOB, isChecked).apply();
            }
        });

        new GetPropOperation().execute();

    }

    private class GetPropOperation extends AsyncTask<String, Void, String> {
        private List<Prop> props = new ArrayList<Prop>();

        @Override
        protected String doInBackground(String... params) {
            new CMDProcessor().su.runWaitFor("busybox chmod 750 "+ context.getFilesDir()+"/utils" );
            CMDProcessor.CommandResult cr=new CMDProcessor().sh.runWaitFor(getFilesDir()+"/utils -govprop "+curgov);
            if(cr.success()){return cr.stdout;}
            else{Log.d(TAG,"read governor err: "+cr.stderr); return null; }
        }
        @Override
        protected void onPostExecute(String result) {
            if((result==null)||(result.length()<=0)) {
                finish();
            }
            else{
                String p[]=result.split(";");
                for (String aP : p) {
                        final String pn[]=aP.split(":");
                        if(pn[1]!=null && !pn[1].trim().equals(""))
                        props.add(new Prop(pn[0].substring(pn[0].lastIndexOf("/") + 1, pn[0].length()),pn[1]));
                }
                linlaHeaderProgress.setVisibility(View.GONE);
                if(props.isEmpty()){
                        nofiles.setVisibility(View.VISIBLE);
                        tools.setVisibility(View.GONE);
                }
                else{
                        nofiles.setVisibility(View.GONE);
                        tools.setVisibility(View.VISIBLE);
                        adapter = new PropAdapter(GovSetActivity.this, R.layout.prop_item, props);
                        packList.setAdapter(adapter);
                }
            }
        }
        @Override
        protected void onPreExecute() {
            linlaHeaderProgress.setVisibility(View.VISIBLE);
            nofiles.setVisibility(View.GONE);
            tools.setVisibility(View.GONE);
            Helpers.get_assetsScript("utils",context,"","");
        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long row) {
        final Prop p = adapter.getItem(position);
        editPropDialog(p);
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

    private void editPropDialog(Prop p) {
        final Prop pp=p;
        LayoutInflater factory = LayoutInflater.from(this);
        final View editDialog = factory.inflate(R.layout.prop_edit_dialog, null);
        final EditText tv = (EditText) editDialog.findViewById(R.id.vprop);
        final TextView tn = (TextView) editDialog.findViewById(R.id.nprop);
        tv.setText(pp.getVal());
        tn.setText(pp.getName());
        new AlertDialog.Builder(this)
                .setTitle(curgov)
                .setView(editDialog)
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (tv.getText().toString() != null && tv.getText().toString().length() > 0)
                            pp.setVal(tv.getText().toString().trim());
                        adapter.notifyDataSetChanged();
                    }
                }).create().show();
    }
}
