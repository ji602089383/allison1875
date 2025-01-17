package com.spldeolin.allison1875.querytransformer.util;

/**
 * @author Deolin 2021-12-06
 */
public class CharUtils {

    public static int digitCharToInt(char c) {
        return Character.getNumericValue(c);
    }

    public static char intToDigitChar(int i) {
        return Character.forDigit(i, 10);
    }

}