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
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.FileArrayAdapter;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Item;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        Intent intent1=getIntent();
        tip=intent1.getStringExtra("mod");
        part=intent1.getStringExtra("part");

        currentDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        fill(currentDir);
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
                    if((tip.equalsIgnoreCase("kernel") && ff.getName().equalsIgnoreCase("boot.img"))||(tip.equalsIgnoreCase("recovery") && ff.getName().equalsIgnoreCase("recovery.img")))
                        fls.add(new Item(ff.getName(),ReadableByteCount(ff.length()), date_modify, ff.getAbsolutePath(),"file"));
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
    public static String ReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = String.valueOf("KMGTPE".charAt(exp-1));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
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
            String dtitlu;
            if(tip.equalsIgnoreCase("kernel")){
                dtitlu=getString(R.string.kernel_img_title);
            }
            else{
                dtitlu=getString(R.string.recovery_img_title);
            }
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
            theButton.setOnClickListener(new CustomListener(alertDialog));
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
            String dtitlu;
            if(tip.equalsIgnoreCase("kernel")){
                dtitlu=getString(R.string.kernel_img_title);
            }
            else{
                dtitlu=getString(R.string.recovery_img_title);
            }
            progressDialog = ProgressDialog.show(FileChooser.this, dtitlu, getString(R.string.wait));
            final StringBuilder sb = new StringBuilder();
            sb.append("dd if="+nFile+" of="+part+"\n");
            if(tip.equalsIgnoreCase("kernel")){
                sb.append("busybox rm -rf /data/dalvik-cache/*\n");
                sb.append("busybox rm -rf /cache/*\n");
                sb.append("reboot\n");
            }
            else{
                sb.append("reboot recovery\n");
            }

            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Helpers.shExec(sb);
                }
            };
            new Thread(runnable).start();

        }
    }



}
