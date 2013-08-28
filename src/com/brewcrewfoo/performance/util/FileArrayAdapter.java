package com.brewcrewfoo.performance.util;

/**
 * Created by h0rn3t on 22.07.2013.
 */

import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;


public class FileArrayAdapter extends ArrayAdapter<Item>{

    private Context c;
    private int id;
    private List<Item>items;

    public FileArrayAdapter(Context context, int textViewResourceId,List<Item> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }
    public Item getItem(int i){
        return items.get(i);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        LinearLayout linlaHeaderProgress;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        final Item o = items.get(position);
        if (o != null) {
            TextView t1 = (TextView) v.findViewById(R.id.TextView01);
            TextView t2 = (TextView) v.findViewById(R.id.TextView02);
            TextView t3 = (TextView) v.findViewById(R.id.TextViewDate);

            if(t1!=null){
                if(o.getImage().equalsIgnoreCase("dir")){t1.setTypeface(null, Typeface.BOLD);}
                else{t1.setTypeface(null, Typeface.NORMAL);}
                t1.setText(o.getName());
            }
            if(t2!=null){
                if(o.getData()==null){
                    t2.setVisibility(View.GONE);
                }
                else{
                    t2.setText(o.getData());
                    t2.setVisibility(View.VISIBLE);
                }
            }
            if(t3!=null){
                if(o.getDate()==null){
                    t3.setVisibility(View.GONE);
                }
                else{
                    t3.setText(o.getDate());
                    t3.setVisibility(View.VISIBLE);
                }
            }
        }
        return v;
    }

}
