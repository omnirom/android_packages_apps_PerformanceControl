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
 *
 * Modded by h0rn3t
 */

package com.brewcrewfoo.performance.fragments;

import android.app.AlertDialog;
import android.app.Dialog;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;

import android.os.AsyncTask;
import android.preference.Preference.OnPreferenceChangeListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;

import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.FlasherActivity;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.activities.ResidualsActivity;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;


public class Tools extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener, Constants {

    private static final int NEW_MENU_ID=Menu.FIRST+1;
    private byte tip;
    private SharedPreferences mPreferences;
    private EditText settingText;
    private Boolean isrun=false;
    private ProgressDialog progressDialog;
    private Preference mResidualFiles;
    private Preference mOptimDB;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

  	    mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.layout.tools);

        mResidualFiles=(Preference) findPreference(RESIDUAL_FILES);
        mOptimDB=(Preference) findPreference(PREF_OPTIM_DB);

        long mStartTime=mPreferences.getLong(RESIDUAL_FILES, 0);
        mResidualFiles.setSummary("");
        if (mStartTime>0)
            mResidualFiles.setSummary(DateUtils.getRelativeTimeSpanString(mStartTime));

        mStartTime=mPreferences.getLong(PREF_OPTIM_DB, 0);
        mOptimDB.setSummary("");
        if (mStartTime>0)
            mOptimDB.setSummary(DateUtils.getRelativeTimeSpanString(mStartTime));

        if(Helpers.binExist("dd").equals(NOT_FOUND)){
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("category_flash_img");
            getPreferenceScreen().removePreference(hideCat);
        }
        if(Helpers.binExist("sqlite3").equals(NOT_FOUND)){
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("category_optim_db");
            getPreferenceScreen().removePreference(hideCat);
        }
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (isrun) {
            switch (tip){
                case 0:
                    progressDialog = ProgressDialog.show(getActivity(), getString(R.string.wipe_cache_title),getString(R.string.wait));
                    break;
                case 1:
                    progressDialog = ProgressDialog.show(getActivity(), getString(R.string.fix_perms_title),getString(R.string.wait));
                    break;
                case 2:
                    progressDialog = ProgressDialog.show(getActivity(), getString(R.string.optim_db_title),getString(R.string.wait));
                    break;
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onDetach() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDetach();
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tools_menu, menu);
        final SubMenu smenu = menu.addSubMenu(0, NEW_MENU_ID, 0,getString(R.string.menu_tab));
        final ViewPager mViewPager = (ViewPager) getView().getParent();
        final int cur=mViewPager.getCurrentItem();
        for(int i=0;i< mViewPager.getAdapter().getCount();i++){
            if(i!=cur)
            smenu.add(0, NEW_MENU_ID +i+1, 0, mViewPager.getAdapter().getPageTitle(i));
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.app_settings) {
            Intent intent = new Intent(getActivity(), PCSettings.class);
            startActivity(intent);
        }
        final ViewPager mViewPager = (ViewPager) getView().getParent();
        for(int i=0;i< mViewPager.getAdapter().getCount();i++){
            if(item.getItemId() == NEW_MENU_ID+i+1) {
                mViewPager.setCurrentItem(i);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(RESIDUAL_FILES)) {
            mResidualFiles.setSummary("");
            final long mStartTime=sharedPreferences.getLong(key,0);
            if (mStartTime>0)
                mResidualFiles.setSummary(DateUtils.getRelativeTimeSpanString(mStartTime));

        }
        else if (key.equals(PREF_OPTIM_DB)) {
            mOptimDB.setSummary("");
            final long mStartTime=sharedPreferences.getLong(key,0);
            if (mStartTime>0)
                mOptimDB.setSummary(DateUtils.getRelativeTimeSpanString(mStartTime));
        }

    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        if (key.equals(PREF_SH)) {
            shEditDialog(key,getString(R.string.sh_title),getString(R.string.sh_msg));
        }
        else if(key.equals(PREF_WIPE_CACHE)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.wipe_cache_title))
                    .setMessage(getString(R.string.wipe_cache_msg))
                    .setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            })
                    .setPositiveButton(getString(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });
            ;
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            //alertDialog.setCancelable(false);
            Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            theButton.setOnClickListener(new WipeCacheListener(alertDialog));

        }
        else if(key.equals(FLASH_KERNEL)) {
            Intent flash = new Intent(getActivity(), FlasherActivity.class);
            flash.putExtra("mod","kernel");
            startActivity(flash);
        }
        else if(key.equals(FLASH_RECOVERY)) {
            Intent flash = new Intent(getActivity(), FlasherActivity.class);
            flash.putExtra("mod","recovery");
            startActivity(flash);
        }
        else if(key.equals(RESIDUAL_FILES)) {
            Intent intent = new Intent(getActivity(), ResidualsActivity.class);
            startActivity(intent);
        }
        else if(key.equals(PREF_FIX_PERMS)) {
            Helpers.get_assetsFile("fix_permissions",getActivity(),"#");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.fix_perms_title))
                    .setMessage(getString(R.string.fix_perms_msg))
                        .setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setPositiveButton(getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                    }
                                });
                ;

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                //alertDialog.setCancelable(false);

                Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                theButton.setOnClickListener(new fpListener(alertDialog));


        }
        else if(key.equals(PREF_OPTIM_DB)) {
            Helpers.get_assetsFile("sql_optimize",getActivity(),"#");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.optim_db_title))
                    .setMessage(getString(R.string.ps_optim_db)+"\n\n"+getString(R.string.fix_perms_msg))
                    .setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .setPositiveButton(getString(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });
            ;

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            //alertDialog.setCancelable(false);

            Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            theButton.setOnClickListener(new sqlListener(alertDialog));


        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    class fpListener implements View.OnClickListener {
        private final Dialog dialog;
        public fpListener(Dialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v) {
            dialog.cancel();
            new FixPermissionsOperation().execute();
        }

    }

    private class FixPermissionsOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            new CMDProcessor().su.runWaitFor(SH_PATH);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            isrun=false;
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            isrun=true;
            tip=1;
            progressDialog = ProgressDialog.show(getActivity(), getString(R.string.fix_perms_title),getString(R.string.wait));
            new CMDProcessor().su.runWaitFor("busybox cat "+ISTORAGE+"fix_permissions > " + SH_PATH );
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    class WipeCacheListener implements View.OnClickListener {
        private final Dialog dialog;
        public WipeCacheListener(Dialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v) {
            dialog.cancel();
            new WipeCacheOperation().execute();
        }
    }

    private class WipeCacheOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            final StringBuilder sb = new StringBuilder();
            sb.append("busybox rm -rf /data/dalvik-cache/*\n");
            sb.append("busybox rm -rf /cache/*\n");
            sb.append("reboot\n");
            Helpers.shExec(sb);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            isrun=false;
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            isrun=true;
            tip=0;
            progressDialog = ProgressDialog.show(getActivity(), getString(R.string.wipe_cache_title),getString(R.string.wait));
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    class sqlListener implements View.OnClickListener {
        private final Dialog dialog;
        public sqlListener(Dialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v) {
            dialog.cancel();
            new DBoptimOperation().execute();
        }
    }
    private class DBoptimOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            new CMDProcessor().su.runWaitFor(SH_PATH);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            isrun=false;
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            isrun=true;
            tip=2;
            progressDialog = ProgressDialog.show(getActivity(), getString(R.string.optim_db_title),getString(R.string.wait));
            mPreferences.edit().putLong(PREF_OPTIM_DB,System.currentTimeMillis()).commit();
            new CMDProcessor().su.runWaitFor("busybox cat "+ISTORAGE+"sql_optimize > " + SH_PATH );
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    public void shEditDialog(final String key,String title,String msg) {
        Resources res = getActivity().getResources();
        String cancel = res.getString(R.string.cancel);
        String ok = res.getString(R.string.ps_volt_save);

        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View alphaDialog = factory.inflate(R.layout.sh_dialog, null);


        settingText = (EditText) alphaDialog.findViewById(R.id.shText);
        settingText.setText(mPreferences.getString(key,""));
        settingText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return true;
            }
        });

        settingText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(msg)
                .setView(alphaDialog)
                .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        /* nothing */
                    }
                })
                .setPositiveButton(ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final SharedPreferences.Editor editor = mPreferences.edit();
                        editor.putString(key, settingText.getText().toString()).commit();

                    }
                })
                .create()
                .show();
    }



}
