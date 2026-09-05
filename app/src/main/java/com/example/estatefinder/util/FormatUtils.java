package com.example.estatefinder.util;

import com.example.estatefinder.model.Property;

import java.text.DecimalFormat;

/** Indian-format currency helpers (₹75,00,000 style grouping). */
public final class FormatUtils {

    private static final DecimalFormat INDIAN_PRICE = new DecimalFormat("#,##,##,##0");

    private FormatUtils() { /* no instances */ }

    /** Formats a sale price, e.g. 7500000 -> "₹75,00,000". */
    public static String formatPrice(double price) {
        return "₹" + INDIAN_PRICE.format(price);
    }

    /** Formats a monthly rent, e.g. 45000 -> "₹45,000/month". */
    public static String formatRent(double price) {
        return "₹" + INDIAN_PRICE.format(price) + "/month";
    }

    /** Formats according to listing type. */
    public static String formatListingPrice(Property property) {
        return "Rent".equals(property.getListingType())
                ? formatRent(property.getPrice())
                : formatPrice(property.getPrice());
    }
}
