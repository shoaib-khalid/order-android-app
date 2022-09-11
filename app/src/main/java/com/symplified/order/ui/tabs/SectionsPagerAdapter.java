package com.symplified.order.ui.tabs;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.symplified.order.R;
import com.symplified.order.models.order.Order;
import com.symplified.order.observers.OrderMediator;
import com.symplified.order.observers.OrderObserver;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter implements OrderMediator {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.new_orders, R.string.ongoing_orders, R.string.past_orders};
    private final Context mContext;
    private OrderObserver ongoingOrderFragment;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        Fragment fragment = PlaceholderFragment.newInstance("new");
        switch (position) {
            case 0: {
                fragment = PlaceholderFragment.newInstance("new");
                break;
            }
            case 1: {
                fragment = PlaceholderFragment.newInstance("ongoing");
                break;
            }
            case 2: {
                fragment = PlaceholderFragment.newInstance("past");
                break;
            }
        }

        return fragment;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);

        switch (position) {
            case 0:
                PlaceholderFragment newOrderFragment = (PlaceholderFragment) createdFragment;
                newOrderFragment.setOrderMediator(this);
                break;
            case 1:
                ongoingOrderFragment = (OrderObserver) createdFragment;
                break;

        }

        return createdFragment;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() { return TAB_TITLES.length; }

    @Override
    public void addOrderToOngoingTab(Order.OrderDetails orderDetails) {
        ongoingOrderFragment.onOrderReceived(orderDetails);
    }
}