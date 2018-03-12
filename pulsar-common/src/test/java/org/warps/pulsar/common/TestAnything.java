package org.warps.pulsar.common;

import com.google.common.collect.Lists;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.warps.pulsar.common.TestAnything.E.a;

/**
 * Created by vincent on 17-1-14.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
public class TestAnything {

    @Nonnull
    public static String g() {
        return "";
    }

    @Nonnull
    public String f() {
        return "";
    }

    @Test
    public void test() {
        System.out.println(Integer.valueOf(" ".charAt(0)));
        System.out.println(Integer.valueOf(StringUtil.NBSP.charAt(0)));
    }

    @Test
    public void testReflection() {
        Map<String, String> counter = new TreeMap<>();

        counter.put("a", "1");
        counter.put("b", "2");
        counter.put("c", "3");

        // System.out.println(counter);

        Object obj = counter;
        assertTrue(obj.getClass().isAssignableFrom(TreeMap.class));
        assertTrue(obj instanceof Map);
        assertFalse(obj.getClass().isAssignableFrom(Map.class));

        if (obj.getClass().isAssignableFrom(Map.class)) {
            System.out.println(obj.getClass() + " isAssignableFrom " + counter.getClass());
        }

        for (Method method : this.getClass().getMethods()) {
            System.out.println(method);
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                System.out.println(annotation);
            }
        }
    }

    @Test
    public void testEnum() {
        assertEquals(10, a.getValue());
        assertEquals("a", a.toString());
        assertEquals(a.name(), a.toString());
    }

    @Test
    public void testList() {
        List<Integer> l = IntStream.range(0, 200).boxed().collect(Collectors.toList());
        l = l.subList(l.size() - 10, l.size());
        System.out.println(l);

        Collections.reverse(l);
        l.addAll(0, Lists.newArrayList(-1, -2, -3, -4));
        System.out.println(l);
    }

    @Test
    public void testRemoveCommentsInSQL() {
        String sql = "-- This is a comment\nSELECT DISTINCT DOM_absHref(`dom`) /** absolute href */ AS href FROM DOMT_links(DOM_load('https://www.baidu.com/s?tn=baidurt&rtt=4&wd=***/&bsst=/***&ie=utf-8/***/'));";
        System.out.println(sql.replaceAll("('(''|[^'])*')|[\\t\\r\\n]|(--[^\\r\\n]*)|(/\\*[\\w\\W]*?(?=\\*/)\\*/)", ""));
    }

    enum E {
        a(10), b(20), c(30);
        private int value;

        E(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
