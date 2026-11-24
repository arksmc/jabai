/*
 * this file turns dates, quantities and other values into clean text for display. screens and tables use it so numbers and dates look the same everywhere.
 */

package com.jabai.util;

public final class Format {
    private Format() {
    }

    /** 60.0 -> "60", 2.5 -> "2.50" */
    public static String qty(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }
}