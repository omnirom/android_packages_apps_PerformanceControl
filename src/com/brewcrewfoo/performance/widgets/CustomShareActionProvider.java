package com.brewcrewfoo.performance.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ActivityChooserView;
import android.widget.ShareActionProvider;

import com.brewcrewfoo.performance.R;

public class CustomShareActionProvider extends ShareActionProvider {

    private final Context mContext;

    public CustomShareActionProvider(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public View onCreateActionView() {
        ActivityChooserView chooserView =
            (ActivityChooserView) super.onCreateActionView();
        Drawable icon =
            mContext.getResources().getDrawable(R.drawable.ic_menu_share_material);
        chooserView.setExpandActivityOverflowButtonDrawable(icon);
        return chooserView;
    }
}
