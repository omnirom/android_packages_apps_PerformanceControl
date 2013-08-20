package com.brewcrewfoo.performance.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

/**
 * Created by h0rn3t on 20.08.2013.
 */
public class DiskInfo extends Fragment implements Constants {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.disk_info, root, false);

        final RelativeLayout lsys=(RelativeLayout) view.findViewById(R.id.system);
        final RelativeLayout ldata=(RelativeLayout) view.findViewById(R.id.data);
        final RelativeLayout lcache=(RelativeLayout) view.findViewById(R.id.cache);
        final RelativeLayout lsd1=(RelativeLayout) view.findViewById(R.id.sd1);
        final RelativeLayout lsd2=(RelativeLayout) view.findViewById(R.id.sd2);


        if(new File("/system").exists()){
            final TextView sysname = (TextView) view.findViewById(R.id.systemname);
            final TextView systotal = (TextView) view.findViewById(R.id.systemtotal);
            final TextView sysused = (TextView) view.findViewById(R.id.systemused);
            final TextView sysfree = (TextView) view.findViewById(R.id.systemfree);
            final ProgressBar sysbar= (ProgressBar) view.findViewById(R.id.systemBar);
            final long v1=Totalbytes(new File("/system"));
            sysname.setText("System");
            systotal.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File("/system"));
            sysused.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            sysfree.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            sysbar.setProgress(Math.round(((v1-v2)*100)/v1));
            lsys.setVisibility(RelativeLayout.VISIBLE);
        }

        if(new File("/data").exists()){
            final TextView dataname = (TextView) view.findViewById(R.id.dataname);
            final TextView datatotal = (TextView) view.findViewById(R.id.datatotal);
            final TextView dataused = (TextView) view.findViewById(R.id.dataused);
            final TextView datafree = (TextView) view.findViewById(R.id.datafree);
            final ProgressBar databar= (ProgressBar) view.findViewById(R.id.dataBar);
            final long v1=Totalbytes(new File("/data"));
            dataname.setText("Data");
            datatotal.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File("/data"));
            dataused.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            datafree.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            databar.setProgress(Math.round(((v1-v2)*100)/v1));
            ldata.setVisibility(RelativeLayout.VISIBLE);
        }

        if(new File("/cache").exists()){
            final TextView cachename = (TextView) view.findViewById(R.id.cachename);
            final TextView cachetotal = (TextView) view.findViewById(R.id.cachetotal);
            final TextView cacheused = (TextView) view.findViewById(R.id.cacheused);
            final TextView cachefree = (TextView) view.findViewById(R.id.cachefree);
            final ProgressBar cachebar= (ProgressBar) view.findViewById(R.id.cacheBar);
            final long v1=Totalbytes(new File("/cache"));
            cachename.setText("Cache");
            cachetotal.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File("/cache"));
            cacheused.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            cachefree.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            cachebar.setProgress(Math.round(((v1-v2)*100)/v1));
            lcache.setVisibility(RelativeLayout.VISIBLE);
        }

        if(new File(Environment.getExternalStorageDirectory().getAbsolutePath()).exists()){
            final TextView sd1name = (TextView) view.findViewById(R.id.sd1name);
            final TextView sd1total = (TextView) view.findViewById(R.id.sd1total);
            final TextView sd1used = (TextView) view.findViewById(R.id.sd1used);
            final TextView sd1free = (TextView) view.findViewById(R.id.sd1free);
            final ProgressBar sd1bar= (ProgressBar) view.findViewById(R.id.sd1Bar);
            final long v1=Totalbytes(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
            sd1name.setText("SD card 1");
            sd1total.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
            sd1used.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            sd1free.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            sd1bar.setProgress(Math.round(((v1-v2)*100)/v1));
            lsd1.setVisibility(RelativeLayout.VISIBLE);
        }

        if(Helpers.ExtsdExists()){
            final TextView sd2name = (TextView) view.findViewById(R.id.sd2name);
            final TextView sd2total = (TextView) view.findViewById(R.id.sd2total);
            final TextView sd2used = (TextView) view.findViewById(R.id.sd2used);
            final TextView sd2free = (TextView) view.findViewById(R.id.sd2free);
            final ProgressBar sd2bar= (ProgressBar) view.findViewById(R.id.sd2Bar);
            final long v1=Totalbytes(new File(Helpers.getExtSD()));
            sd2name.setText("SD card 2");
            sd2total.setText(Helpers.ReadableByteCount(v1));
            final long v2=Freebytes(new File(Helpers.getExtSD()));
            sd2used.setText(getString(R.string.used,Helpers.ReadableByteCount(v1-v2)));
            sd2free.setText(getString(R.string.free,Helpers.ReadableByteCount(v2)));
            sd2bar.setProgress(Math.round(((v1-v2)*100)/v1));
            lsd2.setVisibility(RelativeLayout.VISIBLE);
        }
        return view;

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

}
