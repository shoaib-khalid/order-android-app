package com.symplified.order.ui.orders.tabs;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.symplified.order.R;
import com.symplified.order.interfaces.OrderManager;
import com.symplified.order.interfaces.OrderObserver;
import com.symplified.order.models.order.Order;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter implements OrderManager {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.new_orders, R.string.ongoing_orders, R.string.past_orders};
    private final Context mContext;
    private OrdersFragment newOrderFragment, ongoingOrderFragment;
    private OrderObserver historyOrderFragment;
    private OrderManager orderManager;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        Fragment fragment = OrdersFragment.newInstance("new");
        switch (position) {
            case 0: {
                fragment = OrdersFragment.newInstance("new");
                break;
            }
            case 1: {
                fragment = OrdersFragment.newInstance("ongoing");
                break;
            }
            case 2: {
                fragment = OrdersFragment.newInstance("past");
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
                newOrderFragment = (OrdersFragment) createdFragment;
                newOrderFragment.setOrderManager(this);
                break;
            case 1:
                ongoingOrderFragment = (OrdersFragment) createdFragment;
                ongoingOrderFragment.setOrderManager(this);
                break;
            case 2:
                historyOrderFragment = (OrderObserver) createdFragment;
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
        if (ongoingOrderFragment != null) {
            ongoingOrderFragment.onOrderReceived(orderDetails);
        }
    }

    @Override
    public void addOrderToHistoryTab(Order.OrderDetails orderDetails) {
        if (historyOrderFragment != null) {
            historyOrderFragment.onOrderReceived(orderDetails);
        }
    }

    @Override
    public void editOrder(Order order) {

    }
}