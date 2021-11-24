package com.symplified.order.ui.tabs;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.symplified.order.R;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.new_orders, R.string.processed_orders, R.string.pickup_orders, R.string.sent_orders};
    private final Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        Fragment fragment = PlaceholderFragment.newInstance("new");
        switch (position){
            case 0:{
                fragment = PlaceholderFragment.newInstance("new");
                break;
            }
            case 1: {
                fragment = PlaceholderFragment.newInstance("processed");
                break;
            }
            case 2: {
                fragment = PlaceholderFragment.newInstance("pickup");
                break;
            }
            case 3:{
                fragment = PlaceholderFragment.newInstance("sent");
                break;
            }

        }

        return fragment;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount(){
        return 4;
    }
}