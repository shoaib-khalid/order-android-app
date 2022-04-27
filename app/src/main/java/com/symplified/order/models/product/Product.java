package com.symplified.order.models.product;

import java.io.Serializable;
import java.util.List;

public class Product  implements Serializable {

    public String id;
    public String name;
    public String description;
    public String storeId;
    public String categoryId;
    public String status;
    public String thumbnailUrl;
    public String vendor;
    public String region;
    public String seoUrl;
    public String seoName;
    public boolean trackQuantity;
    public boolean allowOutOfStockPurchases;
    public int minQuantityForAlarm;
    public String packingSize;
    public boolean isPackage;
    public boolean isNoteOptional;
    public String customNote;
    public String created;
    public String updated;
    public String vehicleType;
    public List<ProductVariant> productVariants;
    public List<ProductInventory> productInventories;
    public List<ProductAsset> productAssets;
    public ProductDeliveryDetail productDeliveryDetail;

    public static class ProductVariant {
        public String id;
        public String name;
        public List<ProductVariantAvailable> productVariantsAvailable;
        public String sequenceNumber;

        @Override
        public String toString() {
            return "ProductVariant{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", productVariantsAvailable=" + productVariantsAvailable +
                    ", sequenceNumber='" + sequenceNumber + '\'' +
                    '}';
        }
    }

    public static class ProductVariantAvailable {
        public String id;
        public String value;
        public String productId;
        public String productVariantId;
        public int sequenceNumber;

        @Override
        public String toString() {
            return "ProductVariantAvailable{" +
                    "id='" + id + '\'' +
                    ", value='" + value + '\'' +
                    ", productId='" + productId + '\'' +
                    ", productVariantId='" + productVariantId + '\'' +
                    ", sequenceNumber=" + sequenceNumber +
                    '}';
        }
    }

    public class ProductInventory {
        public String itemCode;
        public float price;
        public int quantity;
        public String productId;
        public String status;
        public List<ProductInventoryItem> productInventoryItems;
        public String sku;

        @Override
        public String toString() {
            return "ProductInventory{" +
                    "itemCode='" + itemCode + '\'' +
                    ", price=" + price +
                    ", quantity=" + quantity +
                    ", productId='" + productId + '\'' +
                    ", status='" + status + '\'' +
                    ", productInventoryItems=" + productInventoryItems +
                    ", sku='" + sku + '\'' +
                    '}';
        }
    }

    public static class ProductInventoryItem {
        public String itemCode;
        public String productVariantAvailableId;
        public String productId;
        public int sequenceNumber;
        public ProductVariantAvailable productVariantAvailable;

        @Override
        public String toString() {
            return "ProductInventoryItem{" +
                    "itemCode='" + itemCode + '\'' +
                    ", productVariantAvailableId='" + productVariantAvailableId + '\'' +
                    ", productId='" + productId + '\'' +
                    ", sequenceNumber=" + sequenceNumber +
                    ", productVariantAvailable=" + productVariantAvailable +
                    '}';
        }
    }

    public static class ProductAsset {
        public String id;
        public String itemCode;
        public String name;
        public String url;
        public String productId;
        public boolean isThumbnail;

        @Override
        public String toString() {
            return "ProductAsset{" +
                    "id='" + id + '\'' +
                    ", itemCode='" + itemCode + '\'' +
                    ", name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    ", productId='" + productId + '\'' +
                    ", isThumbnail=" + isThumbnail +
                    '}';
        }
    }

    public static class ProductDeliveryDetail {
        public String productId;
        public String type;
        public String itemType;

        @Override
        public String toString() {
            return "ProductDeliveryDetail{" +
                    "productId='" + productId + '\'' +
                    ", type='" + type + '\'' +
                    ", itemType='" + itemType + '\'' +
                    '}';
        }
    }

    public static class ProductList {
        public List<Product> content;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", storeId='" + storeId + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", status='" + status + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", vendor='" + vendor + '\'' +
                ", region='" + region + '\'' +
                ", seoUrl='" + seoUrl + '\'' +
                ", seoName='" + seoName + '\'' +
                ", trackQuantity=" + trackQuantity +
                ", allowOutOfStockPurchases=" + allowOutOfStockPurchases +
                ", minQuantityForAlarm=" + minQuantityForAlarm +
                ", packingSize='" + packingSize + '\'' +
                ", isPackage=" + isPackage +
                ", isNoteOptional=" + isNoteOptional +
                ", customNote='" + customNote + '\'' +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", productVariants=" + productVariants.toString() +
                ", productInventories=" + productInventories.toString() +
                ", productAssets=" + productAssets.toString() +
                ", productDeliveryDetail=" + productDeliveryDetail.toString() +
                '}';
    }
}
