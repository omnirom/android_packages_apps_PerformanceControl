package com.brewcrewfoo.performance.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

/**
 * Created by h0rn3t on 20.08.2013.
 */
public class DiskInfo extends Fragment implements Constants {

    private static final int NEW_MENU_ID=Menu.FIRST+1;
    private RelativeLayout lsys;
    private RelativeLayout ldata;
    private RelativeLayout lcache;
    private RelativeLayout lsd1;
    private RelativeLayout lsd2;

    private TextView sysname;
    private TextView systotal;
    private TextView sysused;
    private TextView sysfree;
    private ProgressBar sysbar;

    private TextView dataname;
    private TextView datatotal;
    private TextView dataused;
    private TextView datafree;
    private ProgressBar databar;

    private TextView cachename;
    private TextView cachetotal;
    private TextView cacheused;
    private TextView cachefree;
    private ProgressBar cachebar;

    private TextView sd1name;
    private TextView sd1total;
    private TextView sd1used;
    private TextView sd1free;
    private ProgressBar sd1bar;

    private TextView sd2name;
    private TextView sd2total;
    private TextView sd2used;
    private TextView sd2free;
    private ProgressBar sd2bar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.disk_info, root, false);

        lsys=(RelativeLayout) view.findViewById(R.id.system);
        ldata=(RelativeLayout) view.findViewById(R.id.data);
        lcache=(RelativeLayout) view.findViewById(R.id.cache);
        lsd1=(RelativeLayout) view.findViewById(R.id.sd1);
        lsd2=(RelativeLayout) view.findViewById(R.id.sd2);

        sysname = (TextView) view.findViewById(R.id.systemname);
        systotal = (TextView) view.findViewById(R.id.systemtotal);
        sysused = (TextView) view.findViewById(R.id.systemused);
        sysfree = (TextView) view.findViewById(R.id.systemfree);
        sysbar= (ProgressBar) view.findViewById(R.id.systemBar);

        dataname = (TextView) view.findViewById(R.id.dataname);
        datatotal = (TextView) view.findViewById(R.id.datatotal);
        dataused = (TextView) view.findViewById(R.id.dataused);
        datafree = (TextView) view.findViewById(R.id.datafree);
        databar= (ProgressBar) view.findViewById(R.id.dataBar);

        cachename = (TextView) view.findViewById(R.id.cachename);
        cachetotal = (TextView) view.findViewById(R.id.cachetotal);
        cacheused = (TextView) view.findViewById(R.id.cacheused);
        cachefree = (TextView) view.findViewById(R.id.cachefree);
        cachebar= (ProgressBar) view.findViewById(R.id.cacheBar);

        sd1name = (TextView) view.findViewById(R.id.sd1name);
        sd1total = (TextView) view.findViewById(R.id.sd1total);
        sd1used = (TextView) view.findViewById(R.id.sd1used);
        sd1free = (TextView) view.findViewById(R.id.sd1free);
        sd1bar= (ProgressBar) view.findViewById(R.id.sd1Bar);

        sd2name = (TextView) view.findViewById(R.id.sd2name);
        sd2total = (TextView) view.findViewById(R.id.sd2total);
        sd2used = (TextView) view.findViewById(R.id.sd2used);
        sd2free = (TextView) view.findViewById(R.id.sd2free);
        sd2bar= (ProgressBar) view.findViewById(R.id.sd2Bar);

        get_disk_info();

        return view;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.disk_info_menu, menu);
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


    public static long Freebytes(File f) {
        StatFs stat = new StatFs(f.getPath());
        long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
        return bytesAvailable;
    }
    public static long Totalbytes(File f) {
        StatFs stat = new StatFs(f.getPath());
        long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getBlockCount();
        return bytesAvailable;
    }

    public void get_disk_info(){
        if(new File("/system").exists()){
            final long v1=Totalbytes(new File("/system"));
            sysname.setText("System");
            systotal.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File("/system"));
            sysused.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            sysfree.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            sysbar.setProgress(Math.round(((v1-v2)*100)/v1));
            lsys.setVisibility(RelativeLayout.VISIBLE);
        }
        else{
            lsys.setVisibility(RelativeLayout.GONE);
        }
        if(new File("/data").exists()){
            final long v1=Totalbytes(new File("/data"));
            dataname.setText("Data");
            datatotal.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File("/data"));
            dataused.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            datafree.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            databar.setProgress(Math.round(((v1-v2)*100)/v1));
            ldata.setVisibility(RelativeLayout.VISIBLE);
        }
        else{
            ldata.setVisibility(RelativeLayout.GONE);
        }
        if(new File("/cache").exists()){
            final long v1=Totalbytes(new File("/cache"));
            cachename.setText("Cache");
            cachetotal.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File("/cache"));
            cacheused.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            cachefree.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            cachebar.setProgress(Math.round(((v1-v2)*100)/v1));
            lcache.setVisibility(RelativeLayout.VISIBLE);
        }
        else{
            lcache.setVisibility(RelativeLayout.GONE);
        }
        if(new File(Environment.getExternalStorageDirectory().getAbsolutePath()).exists()){
            final long v1=Totalbytes(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
            sd1name.setText("SD card 1");
            sd1total.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
            sd1used.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            sd1free.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            sd1bar.setProgress(Math.round(((v1-v2)*100)/v1));
            lsd1.setVisibility(RelativeLayout.VISIBLE);
        }
        else{
            lsd1.setVisibility(RelativeLayout.GONE);
        }
        if(Helpers.ExtsdExists()){
            final long v1=Totalbytes(new File(Helpers.getExtSD()));
            sd2name.setText("SD card 2");
            sd2total.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File(Helpers.getExtSD()));
            sd2used.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            sd2free.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            sd2bar.setProgress(Math.round(((v1-v2)*100)/v1));
            lsd2.setVisibility(RelativeLayout.VISIBLE);
        }
        else{
            lsd2.setVisibility(RelativeLayout.GONE);
        }
    }

}
