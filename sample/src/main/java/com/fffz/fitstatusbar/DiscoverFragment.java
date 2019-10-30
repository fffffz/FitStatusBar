package com.fffz.fitstatusbar;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DiscoverFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fra_discover, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        onHiddenChanged(false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            StatusBarManager.setStatusBarColor(getActivity(), Color.RED);
        }
    }
}
