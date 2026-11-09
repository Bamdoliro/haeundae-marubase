package com.bamdoliro.maru.shared.util;

import java.util.Random;

public class RandomUtil {

    public static String randomPhoneNumber() {
        return "010" + randomNumber(1000, 9999) + randomNumber(1000, 9999);
    }

    public static int randomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    /**
     * 시드를 고정한 Random을 넘기면 같은 값이 재현된다.
     */
    public static int randomNumber(Random random, int min, int max) {
        return (int) ((random.nextDouble() * (max - min)) + min);
    }

    public static double randomDouble(double min, double max) {
        return Math.round(((Math.random() * (max - min)) + min) * 1000.0) / 1000.0;
    }
}
