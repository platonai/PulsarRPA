package org.warps.pulsar.common;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Warpspeed Information. All rights reserved
 */
public class TestCollection {

    @Test
    public void testDistinct() {
        List<Integer> integers = Stream.of(1, 2, 3, 4, 4, 5, 5, 5, 6, 5, 7, 3, 19).distinct().collect(Collectors.toList());
        assertEquals(8, integers.size());

        List<String> strings = Stream.of("a", "b", "c", "a", "b", "a", "abc")
                .distinct().collect(Collectors.toList());
        assertEquals(4, strings.size());
    }

    @Test
    public void testTreeSet() {
        TreeSet<Integer> integers = IntStream.range(0, 30).boxed().collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
        integers.forEach(System.out::print);
        integers.pollLast();
        System.out.println();
        integers.forEach(System.out::print);
    }

    @Test
    public void testOrderedStream() {
        int[] counter = {0, 0};

        TreeSet<Integer> orderedIntegers = IntStream.range(0, 1000000).boxed().collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
        long startTime = System.currentTimeMillis();
        int result = orderedIntegers.stream().filter(i -> {
            counter[0]++;
            return i < 1000;
        }).map(i -> i * 2).reduce(0, (x, y) -> x + 2 * y);
        long endTime = System.currentTimeMillis();
        System.out.println("Compute over ordered integers, time elapsed : " + (endTime - startTime) / 1000.0 + "s");
        System.out.println("Result : " + result);

        startTime = System.currentTimeMillis();
        result = 0;
        int a = 0;
        int b = 0;
        for (Integer i : orderedIntegers) {
            if (i < 1000) {
                b = i;
                b *= 2;
                result += a + 2 * b;
            } else {
                break;
            }
        }
        endTime = System.currentTimeMillis();
        System.out.println("Compute over ordered integers, handy code, time elapsed : " + (endTime - startTime) / 1000.0 + "s");
        System.out.println("Result : " + result);

        List<Integer> unorderedIntegers = IntStream.range(0, 1000000).boxed().collect(Collectors.toList());
        startTime = System.currentTimeMillis();
        result = unorderedIntegers.stream().filter(i -> {
            counter[1]++;
            return i < 1000;
        }).map(i -> i * 2).reduce(0, (x, y) -> x + 2 * y);
        endTime = System.currentTimeMillis();
        System.out.println("Compute over unordered integers, time elapsed : " + (endTime - startTime) / 1000.0 + "s");
        System.out.println("Result : " + result);

        System.out.println("Filter loops : " + counter[0] + ", " + counter[1]);
    }

    @Test
    public void testPartitionList() {
        final int batchSize = 3;
        List<String> urls = IntStream.range(1, 20).mapToObj(i -> "http://example.com/" + i).collect(Collectors.toList());
        List<List<String>> partitions = Lists.partition(urls, batchSize);
        for (List<String> partition : partitions) {
            System.out.println(partition.size());
            System.out.println(partition);
        }
    }
}
