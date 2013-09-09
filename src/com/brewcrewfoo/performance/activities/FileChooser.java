package com.brewcrewfoo.performance.activities;

/**
 * Created by h0rn3t on 22.07.2013.
 */

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.DateFormat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.FileArrayAdapter;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Item;
import com.brewcrewfoo.performance.util.UnzipUtility;

public class FileChooser extends ListActivity implements Constants, ActivityThemeChangeInterface {
    final Context context = this;
    private File currentDir;
    SharedPreferences mPreferences;
    private boolean mIsLightTheme;
    private FileArrayAdapter adapter;
    private ProgressDialog progressDialog;
    private String tip;
    private String part;
    private String nFile;
    private int nbk=1;
    private boolean iszip=false;
    private String dtitlu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        Intent intent1=getIntent();
        tip=intent1.getStringExtra("mod");
        part=intent1.getStringExtra("part");
        if(tip.equalsIgnoreCase("kernel")){ dtitlu=getString(R.string.kernel_img_title);}
        else{ dtitlu=getString(R.string.recovery_img_title);}
        currentDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        fill(currentDir);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.filechooser_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.close) {
            finish();
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fill(currentDir);
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

    private void fill(File f){
        File[]dirs = f.listFiles();
        List<Item>dir = new ArrayList<Item>();
        List<Item>fls = new ArrayList<Item>();
        try{
            for(File ff: dirs){
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if(ff.isDirectory()){
                    dir.add(new Item(ff.getName(),getString(R.string.dir),date_modify,ff.getAbsolutePath(),"dir"));
                }
                else{
                    if((tip.equalsIgnoreCase("kernel") && ff.getName().equalsIgnoreCase("boot.img"))||(tip.equalsIgnoreCase("recovery") && ff.getName().equalsIgnoreCase("recovery.img")) || ff.getName().toLowerCase().endsWith(".zip"))
                        fls.add(new Item(ff.getName(),Helpers.ReadableByteCount(ff.length()), date_modify, ff.getAbsolutePath(),"file"));
                }
            }
        }
        catch(Exception e){
        }
        Collections.sort(dir);
        Collections.sort(fls);
        dir.addAll(fls);
        if(!f.getName().equalsIgnoreCase(""))
        dir.add(0,new Item("..",getString(R.string.dir_parent),"",f.getParent(),"dir"));
        adapter = new FileArrayAdapter(this,R.layout.file_view, dir);
        this.setListAdapter(adapter);

    }

    @Override
    public void onBackPressed(){
        if(adapter.getItem(0).getName().equalsIgnoreCase("..")){
            currentDir=currentDir.getParentFile();
            fill(currentDir);
            nbk=1;
        }
        else{
            if(nbk==2){finish();}
            else{
                nbk++;
                Toast.makeText(getApplicationContext(),getString(R.string.bkexit), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Item o = adapter.getItem(position);
        if(o.getImage().equalsIgnoreCase("dir")){
            currentDir = new File(o.getPath());
            fill(currentDir);
        }
        else{
            nFile=currentDir+"/"+o.getName();
            iszip=o.getName().toLowerCase().endsWith(".zip");
            if(iszip){
                new TestZipOperation().execute();
            }
            else{
                makedialog();
            }
        }
    }

    private class TestZipOperation extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            final UnzipUtility unzipper = new UnzipUtility();
            try{
                return unzipper.testZip(nFile,tip);
            }
            catch (Exception e) {
                Log.d(TAG,"ZIP error: "+nFile);
                e.printStackTrace();
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if(!result){
                if(tip.equalsIgnoreCase("kernel")){
                    Toast.makeText(context, getString(R.string.bad_zip,"boot.img"), Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(context, getString(R.string.bad_zip,"recovery.img"), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            makedialog();
        }
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(FileChooser.this, null, getString(R.string.verify));
        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class FlashOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            final StringBuilder sb = new StringBuilder();
            final String dn=Environment.getExternalStorageDirectory().getAbsolutePath()+"/PerformanceControl/tmp";

            if(tip.equalsIgnoreCase("kernel")){
                if(iszip){
                    try{
                        new UnzipUtility().unzipfile(nFile,dn,"boot.img");
                    }
                    catch (Exception e) {
                        Log.d(TAG,"unzip error: "+nFile);
                        e.printStackTrace();
                        return null;
                    }
                    nFile=dn+"/boot.img";
                    if(part.contains("/dev/block/bml") && !Helpers.binExist("flash_image").equals(NOT_FOUND)){
                        sb.append("flash_image boot "+nFile+"\n");
                    }
                    else{
                        sb.append("dd if="+nFile+" of="+part+"\n");
                    }
                    sb.append("busybox rm -rf "+dn+"/*\n");
                }
                else{
                    if(part.contains("/dev/block/bml") && !Helpers.binExist("flash_image").equals(NOT_FOUND)){
                        sb.append("flash_image boot "+nFile+"\n");
                    }
                    else{
                        sb.append("dd if="+nFile+" of="+part+"\n");
                    }
                    //sb.append("dd if="+nFile+" of="+part+"\n");
                }
                sb.append("busybox rm -rf /data/dalvik-cache/*\n");
                sb.append("busybox rm -rf /cache/*\n");
                sb.append("reboot\n");
                //Log.d(TAG,sb.toString());
            }
            else{
                if(iszip){
                    try{
                        new UnzipUtility().unzipfile(nFile,dn,"recovery.img");
                    }
                    catch (Exception e) {
                        Log.d(TAG,"unzip error: "+nFile);
                        e.printStackTrace();
                        return null;
                    }
                    nFile=dn+"/recovery.img";
                    if(part.contains("/dev/block/bml") && !Helpers.binExist("flash_image").equals(NOT_FOUND)){
                        sb.append("flash_image recovery "+nFile+"\n");
                    }
                    else{
                        sb.append("dd if="+nFile+" of="+part+"\n");
                    }
                    sb.append("busybox rm -rf "+dn+"/*\n");
                }
                else{
                    if(part.contains("/dev/block/bml") && !Helpers.binExist("flash_image").equals(NOT_FOUND)){
                        sb.append("flash_image recovery "+nFile+"\n");
                    }
                    else{
                        sb.append("dd if="+nFile+" of="+part+"\n");
                    }
                    //sb.append("dd if="+nFile+" of="+part+"\n");
                }

                sb.append("reboot recovery\n");
                //Log.d(TAG,sb.toString());
            }
            Helpers.shExec(sb);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(FileChooser.this, dtitlu, getString(R.string.wait));
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    class CustomListener implements View.OnClickListener {
        private final Dialog dialog;
        public CustomListener(Dialog dialog) {
            this.dialog = dialog;
        }
        @Override
        public void onClick(View v) {
            dialog.cancel();
            new FlashOperation().execute();
        }
    }

    private void makedialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(dtitlu)
                .setMessage(nFile+" "+getString(R.string.flash_info,part)+" "+tip.toUpperCase()+"\n\n"+getString(R.string.wipe_cache_msg))
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                                //finish();
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
        if (theButton != null) {
            theButton.setOnClickListener(new CustomListener(alertDialog));
        }
    }

}
