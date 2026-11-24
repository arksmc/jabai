/*
 * this file hashes passwords with bcrypt and checks a typed password against a stored hash. the user dao and login screen use it so plain passwords are never saved.
 */

package com.jabai.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordUtil {
    private static final int COST = 12;

    private PasswordUtil() {
    }

    public static String hash(String password) {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray());
    }

    public static boolean verify(String password, String hash) {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}