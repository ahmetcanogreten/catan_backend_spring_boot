package com.ogreten.catan.utils;

import java.util.Random;

public class RandomStringGenerator {
    static final String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static Random rnd = new Random();

    private RandomStringGenerator() {
    }

    protected static String generateRandomString(String chars, int length) {
        StringBuilder builder = new StringBuilder();

        while (builder.length() < length) { // length of the random string.
            int index = rnd.nextInt(length);
            builder.append(chars.charAt(index));
        }
        return builder.toString();
    }

    public static String generate(String chars, int length) {
        return generateRandomString(chars, length);

    }

    public static String generate(int length) {
        return generateRandomString(SALTCHARS, length);
    }

}
