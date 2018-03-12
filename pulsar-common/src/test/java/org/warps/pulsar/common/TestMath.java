package org.warps.pulsar.common;

import org.junit.Test;

import java.util.Random;

import static junit.framework.TestCase.*;

/**
 * Created by vincent on 17-1-14.
 */
public class TestMath {
    @Test
    public void testFloatHash() {
        float a = 0, b = 1000, c = 200000, d = 300000;
        Random r = new Random();

        for (int i = 0; i < 1000; ++i) {
            float x = a + r.nextInt((int) (b - a));
            float y = MathUtils.hashFloat(x, a, b, c, d);
            assertTrue(String.valueOf(y), y >= c && y <= d);
        }

        for (int i = 0; i < 1000; ++i) {
            float x = c + r.nextInt(1000);
            float y = MathUtils.hashFloat(x, a, b, c, d);
            assertFalse(y >= c && y <= d);
        }
    }

    @Test
    public void testIntHash() {
        int a = 0, b = 1000, c = 200000, d = 300000;
        Random r = new Random();

        for (int i = 0; i < 1000; ++i) {
            int x = a + r.nextInt(b - a);
            int y = MathUtils.hashInt(x, a, b, c, d);
            assertTrue(String.valueOf(y), y >= c && y <= d);
        }

        for (int i = 0; i < 1000; ++i) {
            int x = c + r.nextInt(1000);
            int y = MathUtils.hashInt(x, a, b, c, d);
            assertFalse(String.valueOf(y), y >= c && y <= d);
        }
    }

    @Test
    public void testRound() {
        float a = 10, b = 3;

        assertEquals(3, Math.round(a / b));
        assertEquals(3.33, Math.round(100 * a / b) / 100.0);

        a = 17;
        b = 3;
        // System.out.println(17f/3f);
        assertEquals(6, Math.round(a / b));
        assertEquals(5.67, Math.round(100 * a / b) / 100.0);
    }
}
