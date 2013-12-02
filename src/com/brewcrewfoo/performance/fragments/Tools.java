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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.FlasherActivity;
import com.brewcrewfoo.performance.activities.FreezerActivity;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.activities.ResidualsActivity;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

public class Tools extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, Constants {

    private byte tip;
    private SharedPreferences mPreferences;
    private EditText settingText;
    private Boolean isrun = false;
    private ProgressDialog progressDialog;
    private Preference mResidualFiles;
    private Preference mOptimDB;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.xml.tools);

        mResidualFiles = findPreference(RESIDUAL_FILES);
        mOptimDB = findPreference(PREF_OPTIM_DB);

        long mStartTime = mPreferences.getLong(RESIDUAL_FILES, 0);
        mResidualFiles.setSummary("");
        if (mStartTime > 0)
            mResidualFiles.setSummary(DateUtils.getRelativeTimeSpanString(mStartTime));

        mStartTime = mPreferences.getLong(PREF_OPTIM_DB, 0);
        mOptimDB.setSummary("");
        if (mStartTime > 0)
            mOptimDB.setSummary(DateUtils.getRelativeTimeSpanString(mStartTime));

        if (Helpers.binExist("dd").equals(NOT_FOUND) || NO_FLASH) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("category_flash_img");
            getPreferenceScreen().removePreference(hideCat);
        }
        if (Helpers.binExist("pm").equals(NOT_FOUND)) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("category_freezer");
            getPreferenceScreen().removePreference(hideCat);
        }
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (isrun) {
            switch (tip) {
                case 0:
                    progressDialog = ProgressDialog.show(
                            context, getString(R.string.wipe_cache_title), getString(R.string.wait));
                    break;
                case 1:
                    progressDialog = ProgressDialog.show(
                            context, getString(R.string.fix_perms_title), getString(R.string.wait));
                    break;
                case 2:
                    progressDialog = ProgressDialog.show(
                            context, getString(R.string.optim_db_title), getString(R.string.wait));
                    break;
            }
        }
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_settings:
                Intent intent = new Intent(context, PCSettings.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(RESIDUAL_FILES)) {
            mResidualFiles.setSummary("");
            final long mStartTime = sharedPreferences.getLong(key, 0);
            if (mStartTime > 0)
                mResidualFiles.setSummary(DateUtils.getRelativeTimeSpanString(mStartTime));

        } else if (key.equals(PREF_OPTIM_DB)) {
            mOptimDB.setSummary("");
            final long mStartTime = sharedPreferences.getLong(key, 0);
            if (mStartTime > 0)
                mOptimDB.setSummary(DateUtils.getRelativeTimeSpanString(mStartTime));
        }

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        if (key.equals(PREF_SH)) {
            shEditDialog(key, getString(R.string.sh_title), R.string.sh_msg);
        } else if (key.equals(PREF_WIPE_CACHE)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.wipe_cache_title))
                    .setMessage(getString(R.string.wipe_cache_msg))
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

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            //alertDialog.setCancelable(false);
            Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            theButton.setOnClickListener(new WipeCacheListener(alertDialog));

        } else if (key.equals(FLASH_KERNEL)) {
            Intent flash = new Intent(context, FlasherActivity.class);
            flash.putExtra("mod", "kernel");
            startActivity(flash);
        } else if (key.equals(FLASH_RECOVERY)) {
            Intent flash = new Intent(context, FlasherActivity.class);
            flash.putExtra("mod", "recovery");
            startActivity(flash);
        } else if (key.equals(RESIDUAL_FILES)) {
            Intent intent = new Intent(context, ResidualsActivity.class);
            startActivity(intent);
        } else if (key.equals(PREF_FIX_PERMS)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            //alertDialog.setCancelable(false);

            Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            theButton.setOnClickListener(new fpListener(alertDialog));


        } else if (key.equals(PREF_OPTIM_DB)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.optim_db_title))
                    .setMessage(getString(R.string.ps_optim_db) + "\n\n" +
                            getString(R.string.fix_perms_msg))
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

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            //alertDialog.setCancelable(false);
            Button theButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            theButton.setOnClickListener(new sqlListener(alertDialog));
        } else if (key.equals(PREF_FRREZE)) {
            Intent getpacks = new Intent(context, FreezerActivity.class);
            getpacks.putExtra("freeze", true);
            getpacks.putExtra("packs", "usr");
            startActivity(getpacks);
        } else if (key.equals(PREF_UNFRREZE)) {
            Intent getpacks = new Intent(context, FreezerActivity.class);
            getpacks.putExtra("freeze", false);
            startActivity(getpacks);
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
            if (Helpers.isSystemApp(getActivity())) {
                new CMDProcessor().sh.runWaitFor(context.getFilesDir() + "/fix_permissions");
            } else {
                new CMDProcessor().su.runWaitFor(context.getFilesDir() + "/fix_permissions");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            isrun = false;
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            isrun = true;
            tip = 1;
            progressDialog = ProgressDialog.show(context,
                    getString(R.string.fix_perms_title), getString(R.string.wait));
            Helpers.get_assetsScript("fix_permissions", context, "#", "");
            if (Helpers.isSystemApp(getActivity())) {
                new CMDProcessor().sh.runWaitFor(
                        "busybox chmod 750 " + context.getFilesDir() + "/fix_permissions");
            } else {
                new CMDProcessor().su.runWaitFor(
                        "busybox chmod 750 " + context.getFilesDir() + "/fix_permissions");
            }
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
            Helpers.shExec(sb, context, true);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            isrun = false;
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            isrun = true;
            tip = 0;
            progressDialog = ProgressDialog.show(
                    context, getString(R.string.wipe_cache_title), getString(R.string.wait));
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
            if (Helpers.isSystemApp(getActivity())) {
                new CMDProcessor().sh.runWaitFor(context.getFilesDir() + "/sql_optimize");
            } else {
                new CMDProcessor().su.runWaitFor(context.getFilesDir() + "/sql_optimize");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            isrun = false;
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            isrun = true;
            tip = 2;
            progressDialog = ProgressDialog.show(
                    context, getString(R.string.optim_db_title), getString(R.string.wait));
            mPreferences.edit().putLong(PREF_OPTIM_DB, System.currentTimeMillis()).commit();
            Helpers.get_assetsBinary("sqlite3", context);
            Helpers.get_assetsScript("sql_optimize", context, "busybox chmod 750 " +
                    context.getFilesDir() + "/sqlite3", "");
            if (Helpers.isSystemApp(getActivity())) {
                new CMDProcessor().sh.runWaitFor(
                        "busybox chmod 750 " + context.getFilesDir() + "/sql_optimize");
            } else {
                new CMDProcessor().su.runWaitFor(
                        "busybox chmod 750 " + context.getFilesDir() + "/sql_optimize");
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    public void shEditDialog(final String key, String title, int msg) {
        Resources res = context.getResources();
        String cancel = res.getString(R.string.cancel);
        String ok = res.getString(R.string.ps_volt_save);

        LayoutInflater factory = LayoutInflater.from(context);
        final View alphaDialog = factory.inflate(R.layout.sh_dialog, null);


        settingText = (EditText) alphaDialog.findViewById(R.id.shText);
        settingText.setHint(msg);
        settingText.setText(mPreferences.getString(key, ""));
        settingText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return true;
            }
        });

        settingText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(alphaDialog)
                .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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
