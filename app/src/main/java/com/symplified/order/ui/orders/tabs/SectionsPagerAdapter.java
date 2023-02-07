package com.symplified.order.ui.orders.tabs;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.interfaces.OrderManager;
import com.symplified.order.interfaces.OrderObserver;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.networking.apis.StoreApi;
import com.symplified.order.utils.SharedPrefsKey;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter implements OrderManager {

    private final List<String> tabTitles;
    private final Context mContext;
    private OrdersFragment newOrderFragment, ongoingOrderFragment;
    private OrderObserver historyOrderFragment, unpaidOrderFragment;
    private OrderManager orderManager;

    public SectionsPagerAdapter(Context context, FragmentManager fm, List<String> tabTitles) {
        super(fm);
        mContext = context;
        this.tabTitles = tabTitles;
    }

    boolean isOrderConsolidationEnabled = false;
    private void queryStoresForConsolidateOption() {

        StoreApi storeApiService = ServiceGenerator.createStoreService(mContext.getApplicationContext());
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        isOrderConsolidationEnabled = sharedPrefs.getBoolean(SharedPrefsKey.IS_ORDER_CONSOLIDATION_ENABLED, false);

        for (String storeId : sharedPrefs.getString(SharedPrefsKey.STORE_ID_LIST, "")
                .split(" ")) {
            storeApiService.getStoreById(storeId).clone().enqueue(new Callback<StoreResponse.SingleStoreResponse>() {
                @Override
                public void onResponse(
                        @NonNull Call<StoreResponse.SingleStoreResponse> call,
                        @NonNull Response<StoreResponse.SingleStoreResponse> response
                ) {

                    if (response.isSuccessful()
                            && response.body() != null
                            && response.body().data.dineInConsolidatedOrder
                            && !isOrderConsolidationEnabled) {
                        isOrderConsolidationEnabled = true;
                        sharedPrefs.edit()
                                .putBoolean(SharedPrefsKey.IS_ORDER_CONSOLIDATION_ENABLED, true)
                                .apply();

                        showUnpaidTab();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<StoreResponse.SingleStoreResponse> call, @NonNull Throwable t) {}
            });
        }
    }

    public void showUnpaidTab() {
        if (tabTitles.size() == 3) {
            tabTitles.add(mContext.getString(R.string.unpaid_orders));
        }
    }

    @NonNull
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
            case 3: {
                fragment = new UnpaidOrdersFragment();
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
            case 3:
                unpaidOrderFragment = (OrderObserver) createdFragment;
                break;
        }

        return createdFragment;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }

    @Override
    public int getCount() {
        return tabTitles.size();
    }

    @Override
    public void addOrderToOngoingTab(Order.OrderDetails orderDetails) {
        if (ongoingOrderFragment != null) {
            ongoingOrderFragment.onOrderReceived(orderDetails);
        }

        if (unpaidOrderFragment != null) {
            unpaidOrderFragment.onOrderReceived(orderDetails);
        }
    }

    @Override
    public void addOrderToHistoryTab(Order.OrderDetails orderDetails) {
        if (historyOrderFragment != null) {
            historyOrderFragment.onOrderReceived(orderDetails);
        }

        if (unpaidOrderFragment != null) {
            unpaidOrderFragment.onOrderReceived(orderDetails);
        }
    }

    @Override
    public void editOrder(Order order) {
    }
}