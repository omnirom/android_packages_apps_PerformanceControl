package com.brewcrewfoo.performance.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;

import java.util.List;

/**
 * Created by h0rn3t on 22.09.2013.
 */
public class PropAdapter extends ArrayAdapter<Prop> {
    private Context c;
    private int id;
    private List<Prop> props;

    public PropAdapter(Context context, int textViewResourceId,List<Prop> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        props = objects;
    }
    public Prop getItem(int i){
        return props.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        final Prop p = props.get(position);
        if (p != null) {
            TextView pp = (TextView) v.findViewById(R.id.prop);
            TextView pv = (TextView) v.findViewById(R.id.pval);

            if(pp!=null){
                pp.setText(p.getName());
            }
            if(pv!=null){
                pv.setText(p.getVal());
            }

        }
        return v;
    }

}
