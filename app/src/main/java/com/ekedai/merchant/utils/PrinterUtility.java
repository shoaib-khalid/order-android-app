package com.ekedai.merchant.utils;

import com.ekedai.merchant.enums.ServiceType;
import com.ekedai.merchant.models.item.Item;
import com.ekedai.merchant.models.item.ItemAddOn;
import com.ekedai.merchant.models.item.SubItem;
import com.ekedai.merchant.models.order.Order;

import java.text.DecimalFormat;
import java.util.List;
import java.util.TimeZone;

public final class PrinterUtility {

    public static String generateItemPrintText(
            List<Item> items,
            String currency,
            DecimalFormat formatter
    ) {
        StringBuilder itemText = new StringBuilder();
        for (Item item : items) {
            itemText.append("\n").append(item.quantity).append(" x ").append(item.productName);
            String spacing = Integer.toString(item.quantity).replaceAll("\\d", " ") + " * ";

            if (!Utilities.isBlank(item.productVariant)) {
                itemText.append("\n").append(spacing).append(item.productVariant);
            }

            for (SubItem subItem : item.orderSubItem) {
                itemText.append("\n").append(spacing).append(subItem.productName);
            }

            for (ItemAddOn itemAddOn : item.orderItemAddOn) {
                itemText.append("\n").append(spacing).append(itemAddOn.productAddOn.addOnTemplateItem.name);
            }

            if (!Utilities.isBlank(item.specialInstruction)) {
                itemText.append("\nInstructions: ").append(item.specialInstruction);
            }

            itemText.append("\nPrice: ")
                    .append(currency)
                    .append(" ")
                    .append(formatter.format(item.price))
                    .append("\n");
        }

        return String.valueOf(itemText);
    }

    public static String generateReceiptText(
            Order order,
            List<Item> items,
            String currency
    ) {
        DecimalFormat formatter = Utilities.getMonetaryAmountFormat();
        String divider = "\n----------------------------";
        String divider2 = "\n****************************";
        StringBuilder receiptText = new StringBuilder();

        String customerNotes = order.customerNotes != null ? order.customerNotes : "";
        switch (customerNotes.toUpperCase()) {
            case "TAKEAWAY":
                customerNotes = "Take Away";
                break;
            case "SELFCOLLECT":
                customerNotes = "Self Collect";
                break;
        }

        receiptText.append("\n")
                .append((order.serviceType == ServiceType.DINEIN
                        && order.store.verticalCode.equalsIgnoreCase("fnb")
                        ? customerNotes
                        : order.store != null ? order.store.name : "Deliverin.MY Order Chit"))
                .append(divider)
                .append("\nOrder Id: ").append(order.invoiceId);

        if (order.created != null) {
            TimeZone storeTimeZone = order.store != null
                    && order.store.regionCountry != null
                    && order.store.regionCountry.timezone != null
                    ? TimeZone.getTimeZone(order.store.regionCountry.timezone)
                    : TimeZone.getDefault();
            receiptText.append("\n").append(
                    Utilities.convertUtcTimeToLocalTimezone(order.created, storeTimeZone)
            );
        }

        receiptText.append("\nOrder Type: ")
                .append(order.serviceType == ServiceType.DINEIN
                        ? (!order.store.verticalCode.equalsIgnoreCase("fnb") ? "In-store" : "Dine In")
                        : order.orderShipmentDetail.storePickup ? "Self-Pickup" : "Delivery");

        if (order.orderPaymentDetail != null &&
                order.orderPaymentDetail.paymentChannel != null) {
            receiptText.append("\nPayment Type: ")
                    .append(order.orderPaymentDetail.paymentChannel);
        }

        if (order.orderShipmentDetail != null
                && !Utilities.isBlank(order.orderShipmentDetail.phoneNumber)) {
            receiptText.append("\nCustomer no.: ").append(order.orderShipmentDetail.phoneNumber);
        }

        if (!Utilities.isBlank(customerNotes)
                && (order.serviceType != ServiceType.DINEIN
                || !order.store.verticalCode.equalsIgnoreCase("fnb"))) {

            if (order.serviceType != ServiceType.DINEIN) {
                receiptText.append("\nNotes: ");
            }
            receiptText.append(customerNotes);
        }

        receiptText.append(divider).append("\n")
                .append(generateItemPrintText(items, currency, formatter))
                .append(divider)
                .append("\nSub-total           ").append(currency).append(" ").append(formatter.format(order.subTotal))
                .append("\nService Charges     ").append(currency).append(" ")
                .append(order.storeServiceCharges != null
                        ? formatter.format(order.storeServiceCharges)
                        : " 0.00");

        if (order.serviceType != ServiceType.DINEIN) {
            receiptText.append("\nDelivery Charges    ").append(currency).append(" ")
                    .append(order.deliveryCharges != null
                            ? formatter.format(order.deliveryCharges)
                            : " 0.00");
        }

        receiptText.append(divider)
                .append("\nTotal               ")
                .append(currency).append(" ").append(formatter.format(order.total))
                .append(divider2)
                .append("\n\n\n\n\n");

        return String.valueOf(receiptText);
    }
}
