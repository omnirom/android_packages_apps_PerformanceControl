package com.brewcrewfoo.performance.util;

/**
 * Created by h0rn3t on 17.07.2013.
 */
import java.util.List;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;

public class PackAdapter extends BaseAdapter {

    List<PackageInfo> packageList;
    Activity context;
    PackageManager packageManager;
    String[] pList;


    public PackAdapter(Activity context,String[] pmList, PackageManager packageManager) {
        super();
        this.context = context;
        this.packageManager = packageManager;
        this.pList=pmList;
    }

    private class ViewHolder {
        TextView packRaw;
        TextView packName;
        ImageView imageView;
    }

    public int getCount() {
        return pList.length;
    }

    public String getItem(int position) {
        return pList[position];
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater inflater = context.getLayoutInflater();

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.pack_item, null);

            holder = new ViewHolder();

            holder.packRaw = (TextView) convertView.findViewById(R.id.packraw);
            holder.packName = (TextView) convertView.findViewById(R.id.packname);
            holder.imageView = (ImageView) convertView.findViewById(R.id.icon);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        PackageInfo packageInfo = null;// (PackageInfo) getItem(position);
        try {
            packageInfo = context.getPackageManager().getPackageInfo(getItem(position), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.packRaw.setText(packageInfo.packageName);
        holder.packName.setText(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString());
        holder.imageView.setImageDrawable(packageManager.getApplicationIcon(packageInfo.applicationInfo));

        return convertView;
    }
}