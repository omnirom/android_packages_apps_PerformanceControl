package com.brewcrewfoo.performance.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

public class DiskInfo extends Fragment implements Constants {

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

    private String internalsd = "";
    private String externalsd = "";
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        //setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.disk_info, root, false);

        lsys = (RelativeLayout) view.findViewById(R.id.system);
        lsys.setOnClickListener(new View.OnClickListener() {
            private byte ck = 0;

            @Override
            public void onClick(View v) {
                if (++ck % 2 == 1) {
                    set_ex_info("/system", systotal, sysused, sysfree);
                } else {
                    set_part_info("/system", "System", sysname, systotal, sysused,
                            sysfree, sysbar, lsys);
                }
            }
        });

        ldata = (RelativeLayout) view.findViewById(R.id.data);
        ldata.setOnClickListener(new View.OnClickListener() {
            private byte ck = 0;

            @Override
            public void onClick(View v) {
                if (++ck % 2 == 1) {
                    set_ex_info("/data", datatotal, dataused, datafree);
                } else {
                    set_part_info("/data", "Data", dataname, datatotal, dataused,
                            datafree, databar, ldata);
                }
            }
        });
        lcache = (RelativeLayout) view.findViewById(R.id.cache);
        lcache.setOnClickListener(new View.OnClickListener() {
            private byte ck = 0;

            @Override
            public void onClick(View v) {
                if (++ck % 2 == 1) {
                    set_ex_info("/cache", cachetotal, cacheused, cachefree);
                } else {
                    set_part_info("/cache", "Cache", cachename, cachetotal, cacheused,
                            cachefree, cachebar, lcache);
                }
            }
        });
        lsd1 = (RelativeLayout) view.findViewById(R.id.sd1);
        lsd1.setOnClickListener(new View.OnClickListener() {
            private byte ck = 0;

            @Override
            public void onClick(View v) {
                if (++ck % 2 == 1) {
                    set_ex_info(internalsd, sd1total, sd1used, sd1free);
                } else {
                    set_part_info(internalsd, "SD card 1", sd1name, sd1total, sd1used,
                            sd1free, sd1bar, lsd1);
                }
            }
        });
        lsd2 = (RelativeLayout) view.findViewById(R.id.sd2);
        lsd2.setOnClickListener(new View.OnClickListener() {
            private byte ck = 0;

            @Override
            public void onClick(View v) {
                if (++ck % 2 == 1) {
                    set_ex_info(externalsd, sd2total, sd2used, sd2free);
                } else {
                    set_part_info(externalsd, "SD card 2", sd2name, sd2total, sd2used,
                            sd2free, sd2bar, lsd2);
                }
            }
        });

        sysname = (TextView) view.findViewById(R.id.systemname);
        systotal = (TextView) view.findViewById(R.id.systemtotal);
        sysused = (TextView) view.findViewById(R.id.systemused);
        sysfree = (TextView) view.findViewById(R.id.systemfree);
        sysbar = (ProgressBar) view.findViewById(R.id.systemBar);

        dataname = (TextView) view.findViewById(R.id.dataname);
        datatotal = (TextView) view.findViewById(R.id.datatotal);
        dataused = (TextView) view.findViewById(R.id.dataused);
        datafree = (TextView) view.findViewById(R.id.datafree);
        databar = (ProgressBar) view.findViewById(R.id.dataBar);

        cachename = (TextView) view.findViewById(R.id.cachename);
        cachetotal = (TextView) view.findViewById(R.id.cachetotal);
        cacheused = (TextView) view.findViewById(R.id.cacheused);
        cachefree = (TextView) view.findViewById(R.id.cachefree);
        cachebar = (ProgressBar) view.findViewById(R.id.cacheBar);

        sd1name = (TextView) view.findViewById(R.id.sd1name);
        sd1total = (TextView) view.findViewById(R.id.sd1total);
        sd1used = (TextView) view.findViewById(R.id.sd1used);
        sd1free = (TextView) view.findViewById(R.id.sd1free);
        sd1bar = (ProgressBar) view.findViewById(R.id.sd1Bar);

        sd2name = (TextView) view.findViewById(R.id.sd2name);
        sd2total = (TextView) view.findViewById(R.id.sd2total);
        sd2used = (TextView) view.findViewById(R.id.sd2used);
        sd2free = (TextView) view.findViewById(R.id.sd2free);
        sd2bar = (ProgressBar) view.findViewById(R.id.sd2Bar);

        loadData();

        return view;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!getResources().getBoolean(R.bool.config_showPerformanceOnly)) {
            inflater.inflate(R.menu.disk_info_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                loadData();
                break;
            case R.id.app_settings:
                Intent intent = new Intent(context, PCSettings.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    public static long Freebytes(File f) {
        StatFs stat = new StatFs(f.getPath());
        return stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
    }

    public static long Totalbytes(File f) {
        StatFs stat = new StatFs(f.getPath());
        return stat.getBlockSizeLong() * stat.getBlockCountLong();
    }

    public Boolean set_part_info(String part, String titlu, TextView t1, TextView t2,
                                 TextView t3, TextView t4, ProgressBar b, RelativeLayout l) {
        if (new File(part).exists()) {
            final long v1 = Totalbytes(new File(part));
            t1.setText(titlu);
            t2.setText(Helpers.ReadableByteCount(v1));
            final long v2 = Freebytes(new File(part));
            t3.setText(getString(R.string.used, Helpers.ReadableByteCount(v1 - v2)));
            t4.setText(getString(R.string.free, Helpers.ReadableByteCount(v2)));
            b.setProgress(Math.round(((v1 - v2) * 100) / v1));
            l.setVisibility(RelativeLayout.VISIBLE);
            return true;
        } else {
            l.setVisibility(RelativeLayout.GONE);
            return false;
        }
    }

    public void set_ex_info(String part, TextView t2, TextView t3, TextView t4) {
        CMDProcessor.CommandResult cr = null;
        cr = new CMDProcessor().sh.runWaitFor("busybox echo `mount | busybox grep " + part +
                " | busybox awk '{print $1,$3,$4}'`");
        if (cr.success()) {
            t2.setText(cr.stdout.split(" ")[2].split(",")[0].toUpperCase());
            t3.setText(cr.stdout.split(" ")[0]);
            t4.setText(cr.stdout.split(" ")[1].toUpperCase());
        }
    }

    public void loadData() {
        CMDProcessor.CommandResult cr = null;
        set_part_info("/system", "System", sysname, systotal, sysused, sysfree, sysbar, lsys);
        set_part_info("/data", "Data", dataname, datatotal, dataused, datafree, databar, ldata);
        set_part_info("/cache", "Cache", cachename, cachetotal, cacheused,
                cachefree, cachebar, lcache);

        cr = new CMDProcessor().sh.runWaitFor("busybox echo `busybox mount | " +
                "busybox egrep -v \"asec|android_secure|sdcard1|external_sd|sd-ext\" | " +
                "busybox egrep -i \"(sdcard|sdcard0)\" | busybox awk '{print $3}'`");

        if (cr.success() && set_part_info(cr.stdout, "SD card 1", sd1name, sd1total,
                sd1used, sd1free, sd1bar, lsd1)) {
            internalsd = cr.stdout;
        }

        String sep = "";
        if (!internalsd.equals("")) sep = "|";

        cr = new CMDProcessor().sh.runWaitFor("busybox echo `busybox mount | busybox egrep -v " +
                "\"asec|android_secure" + sep + internalsd + "\" | busybox egrep -i \"" +
                "(external_sd|sdcard1|sd-ext)\" | busybox awk '{print $3}'`");

        if (cr.success() && set_part_info(cr.stdout, "SD card 2", sd2name, sd2total, sd2used,
                sd2free, sd2bar, lsd2)) {
            externalsd = cr.stdout;
        }

    }
}
