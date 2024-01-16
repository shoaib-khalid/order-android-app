package com.ekedai.merchant.data;

import com.ekedai.merchant.models.order.Order;
import com.ekedai.merchant.models.product.Product;
import com.ekedai.merchant.models.store.Store;

import java.util.ArrayList;
import java.util.List;

public class SampleData {

    private static final SampleData instance = new SampleData();

    public final List<Store> stores = new ArrayList<>();
    public final List<Product> products = new ArrayList<>();
    public final List<Order> orders = new ArrayList<>();

    private SampleData() {

        (new Thread(() -> {
            stores.addAll(generateStores());
        })).start();

    }

    public static List<Store> generateStores() {
        List<Store> storesList = new ArrayList<>();

        Store store = new Store();
        store.id = "Karachi Chaman Biryani";
        store.name = "Karachi Chaman Biryani";
        Store.StoreAsset asset = new Store.StoreAsset();
        asset.assetUrl = "https://images.deliveryhero.io/image/fd-pk/LH/omwm-listing.jpg?width=60&height=60";
        store.storeAssets.add(asset);
        storesList.add(store);

        store = new Store();
        store.id = "Subway";
        store.name = "Subway";
        asset = new Store.StoreAsset();
        asset.assetUrl = "https://images.deliveryhero.io/image/fd-pk/pk-logos/cs7rd-logo.jpg?width=60&height=60";
        store.storeAssets.add(asset);
        storesList.add(store);

        store = new Store();
        store.id = "Savour Foods";
        store.name = "Savour Foods";
        asset = new Store.StoreAsset();
        asset.assetUrl = "https://images.deliveryhero.io/image/fd-pk/pk-logos/cw2tn-logo.jpg?width=60&height=60";
        store.storeAssets.add(asset);
        storesList.add(store);

        store = new Store();
        store.id = "Burger Lab";
        store.name = "Burger Lab";
        asset = new Store.StoreAsset();
        asset.assetUrl = "https://www.franchisepk.com/public/user_img/Picsart_22-09-29_11-02-26-43131.png";
        store.storeAssets.add(asset);
        storesList.add(store);

        store = new Store();
        store.id = "Cheezious";
        store.name = "Cheezious";
        asset = new Store.StoreAsset();
        asset.assetUrl = "https://em-cdn.eatmubarak.pk/54946/logo/1649325481.png";
        store.storeAssets.add(asset);
        storesList.add(store);

        store = new Store();
        store.id = "Syrian Shawarma";
        store.name = "Syrian Shawarma";
        asset = new Store.StoreAsset();
        asset.assetUrl = "https://img.freepik.com/premium-vector/shawarma-logo-design_9845-541.jpg?size=360&ext=jpg";
        store.storeAssets.add(asset);
        storesList.add(store);

        store = new Store();
        store.id = "Fresco Sweets";
        store.name = "Fresco Sweets";
        asset = new Store.StoreAsset();
        asset.assetUrl = "https://scontent.fisb13-1.fna.fbcdn.net/v/t39.30808-1/352717023_187621857591512_8748190275079805223_n.jpg?stp=cp0_dst-jpg_e15_p120x120_q65&_nc_cat=109&ccb=1-7&_nc_sid=4da83f&_nc_ohc=26J74bngskAAX9ARuD-&_nc_ht=scontent.fisb13-1.fna&oh=00_AfCjiDHIIH_b82zSCBpe9An10NNj3GlBj0T8_fjv-zoc6g&oe=659C54D6";
        store.storeAssets.add(asset);
        storesList.add(store);

        store = new Store();
        store.id = "Jamil Sweets";
        store.name = "Jamil Sweets";
        asset = new Store.StoreAsset();
        asset.assetUrl = "https://images.deliveryhero.io/image/fd-pk/pk-logos/cc4aj-logo.jpg?width=60&height=60";
        store.storeAssets.add(asset);
        storesList.add(store);

        return storesList;
    }

    public static SampleData getInstance() { return instance; }
}
