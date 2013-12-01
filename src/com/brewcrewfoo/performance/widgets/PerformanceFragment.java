package com.brewcrewfoo.performance.widgets;

import android.app.Activity;
import android.app.Fragment;

import com.brewcrewfoo.performance.activities.MainActivity;
import com.brewcrewfoo.performance.util.Constants;

public class PerformanceFragment extends Fragment implements Constants {

    public void onAttach(Activity activity, int resId) {
        super.onAttach(activity);
        MainActivity mainActivity =
                ((MainActivity) activity.getFragmentManager().findFragmentByTag(TAG));
        if (mainActivity != null)
            mainActivity.onSectionAttached(resId);
    }

}
