package com.android305.lights.service;

import java.util.Random;

public class ServiceUtils {
    public static final int DEFAULT = 1;
    public static final int LAMP = 1001;
    public static final int GROUP = 2001;
    public static final int TIMER = 3001;
    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     *
     * @return Integer between min and max, inclusive.
     *
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int start) {
        int max = start + 999;
        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return rand.nextInt((max - start) + 1) + start;
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
