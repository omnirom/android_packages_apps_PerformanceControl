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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.FileArrayAdapter;
import com.brewcrewfoo.performance.util.Item;

public class FileChooser extends ListActivity implements Constants, ActivityThemeChangeInterface {

    private File currentDir;
    SharedPreferences mPreferences;
    private boolean mIsLightTheme;
    private FileArrayAdapter adapter;

    private String tip;
    private String part;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();

        Intent intent1=getIntent();
        tip=intent1.getStringExtra("mod");
        part=intent1.getStringExtra("part");

        currentDir = new File(Environment.getExternalStorageDirectory().getPath());
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
        //this.setTitle("Current Dir: "+f.getName());
        List<Item>dir = new ArrayList<Item>();
        List<Item>fls = new ArrayList<Item>();
        try{
            for(File ff: dirs){
                Date lastModDate = new Date(ff.lastModified());
                DateFormat formater = DateFormat.getDateTimeInstance();
                String date_modify = formater.format(lastModDate);
                if(ff.isDirectory()){
                    File[] fbuf = ff.listFiles();
                    int buf = 0;
                    if(fbuf != null){ buf = fbuf.length;}
                    else buf = 0;
                    String num_item = getString(R.string.nitem)+": "+String.valueOf(buf);
                    dir.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(),"dir"));
                }
                else{
                    int dot = ff.getName().lastIndexOf(".");
                    String ext = ff.getName().substring(dot + 1);
                    if(ext.equalsIgnoreCase("img"))
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
        super.onListItemClick(l, v, position, id);
        Item o = adapter.getItem(position);
        if(o.getImage().equalsIgnoreCase("dir")){
            currentDir = new File(o.getPath());
            fill(currentDir);
        }
        else{
            onFileClick(o);
        }
    }

    private void onFileClick(Item o){
        Toast.makeText(this, "Selected " + currentDir+"/"+o.getName()+" flash as "+tip+" as "+part, Toast.LENGTH_SHORT).show();

        /*Intent intent = new Intent();
        intent.putExtra("GetPath",currentDir.toString());
        intent.putExtra("GetFileName",o.getName());
        setResult(RESULT_OK, intent);
        finish();*/
    }

}
