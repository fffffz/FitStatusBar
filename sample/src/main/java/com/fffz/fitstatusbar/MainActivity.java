package com.fffz.fitstatusbar;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

public class MainActivity extends FragmentActivity implements View.OnClickListener {

    private Fragment mHomeFragment;
    private Fragment mDiscoverFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);
        findViewById(R.id.tab_home).setOnClickListener(this);
        findViewById(R.id.tab_discover).setOnClickListener(this);
        mHomeFragment = getSupportFragmentManager().findFragmentByTag("Home");
        if (mHomeFragment == null) {
            mHomeFragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.content, mHomeFragment, "Home").commit();
        }
        mDiscoverFragment = getSupportFragmentManager().findFragmentByTag("Discover");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tab_home) {
            if (!mHomeFragment.isHidden() || mDiscoverFragment == null) {
                return;
            }
            getSupportFragmentManager().beginTransaction().show(mHomeFragment).hide(mDiscoverFragment).commit();
        } else if (id == R.id.tab_discover) {
            if (mDiscoverFragment != null && !mDiscoverFragment.isHidden()) {
                return;
            }
            if (mDiscoverFragment == null) {
                mDiscoverFragment = new DiscoverFragment();
                getSupportFragmentManager().beginTransaction().add(R.id.content, mDiscoverFragment, "Discover").show(mDiscoverFragment).hide(mHomeFragment).commit();
            } else {
                getSupportFragmentManager().beginTransaction().show(mDiscoverFragment).hide(mHomeFragment).commit();
            }
        }
    }

}
