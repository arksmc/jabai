package com.jabai.util;

public final class Barcode {
    private static final String PREFIX = "480";   // GS1 prefix for the Philippines
    private static final String COMPANY = "0001"; // hypothetical company code

    private Barcode() {
    }

    /** 13-digit EAN-13: 3-digit prefix + 4-digit company + 5-digit item + check digit. */
    public static String ean13(int itemNumber) {
        if (itemNumber < 0 || itemNumber > 99999) {
            throw new IllegalArgumentException("Item number must be 0-99999.");
        }
        String body = PREFIX + COMPANY + String.format("%05d", itemNumber);
        return body + checkDigit(body);
    }

    /** EAN-13 check digit for the first 12 digits. */
    public static int checkDigit(String first12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = first12.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        return (10 - sum % 10) % 10;
    }

    public static boolean isValidEan13(String code) {
        if (code == null || !code.matches("\\d{13}")) return false;
        return checkDigit(code.substring(0, 12)) == code.charAt(12) - '0';
    }
}