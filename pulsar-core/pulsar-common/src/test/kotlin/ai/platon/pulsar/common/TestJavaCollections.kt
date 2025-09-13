package ai.platon.pulsar.common

import com.google.common.collect.Lists
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestJavaCollections {
    @Test
    fun testDistinct() {
        val integers = Stream.of(1, 2, 3, 4, 4, 5, 5, 5, 6, 5, 7, 3, 19).distinct().collect(Collectors.toList())
        Assertions.assertEquals(8, integers.size)
        
        val strings = Stream.of("a", "b", "c", "a", "b", "a", "abc")
            .distinct().collect(Collectors.toList())
        Assertions.assertEquals(4, strings.size)
    }
    
    @Test
    fun testTreeSet() {
        val integers = IntStream.range(0, 30).boxed().collect(
            Supplier<TreeSet<Int?>> { TreeSet() },
            BiConsumer<TreeSet<Int?>, Int> { obj: TreeSet<Int?>, e: Int? -> obj.add(e) },
            BiConsumer<TreeSet<Int?>, TreeSet<Int?>> { obj: TreeSet<Int?>, c: TreeSet<Int?>? ->
                obj.addAll(
                    c!!
                )
            })
        Assertions.assertTrue(integers.contains(0))
        Assertions.assertTrue(integers.contains(29))
        Assertions.assertFalse(integers.contains(30))
        integers.pollLast()
        Assertions.assertFalse(integers.contains(29))
    }
    
    @Test
    fun testOrderedStream() {
        val counter = intArrayOf(0, 0)
        
        val orderedIntegers = IntStream.range(0, 1000000).boxed().collect(
            Supplier<TreeSet<Int>> { TreeSet() },
            BiConsumer<TreeSet<Int>, Int> { obj: TreeSet<Int>, e: Int -> obj.add(e) },
            BiConsumer<TreeSet<Int>, TreeSet<Int>> { obj: TreeSet<Int>, c: TreeSet<Int>? ->
                obj.addAll(
                    c!!
                )
            })
        var startTime = System.currentTimeMillis()
        var result = orderedIntegers.stream().filter { i: Int ->
            counter[0]++
            i < 1000
        }.map<Int> { i: Int -> i * 2 }.reduce(0) { x: Int, y: Int -> x + 2 * y }
        var endTime = System.currentTimeMillis()
        println("Compute over ordered integers, time elapsed : " + (endTime - startTime) / 1000.0 + "s")
        println("Result : $result")
        
        startTime = System.currentTimeMillis()
        result = 0
        val a = 0
        var b = 0
        for (i in orderedIntegers) {
            if (i < 1000) {
                b = i
                b *= 2
                result += a + 2 * b
            } else {
                break
            }
        }
        endTime = System.currentTimeMillis()
        println("Compute over ordered integers, handy code, time elapsed : " + (endTime - startTime) / 1000.0 + "s")
        println("Result : $result")
        
        val unorderedIntegers = IntStream.range(0, 1000000).boxed().collect(Collectors.toList())
        startTime = System.currentTimeMillis()
        result = unorderedIntegers.stream().filter { i: Int ->
            counter[1]++
            i < 1000
        }.map<Int> { i: Int -> i * 2 }.reduce(0) { x: Int, y: Int -> x + 2 * y }
        endTime = System.currentTimeMillis()
        println("Compute over unordered integers, time elapsed : " + (endTime - startTime) / 1000.0 + "s")
        println("Result : $result")
        
        println("Filter loops : " + counter[0] + ", " + counter[1])
    }
    
    @Test
    fun testPartitionList() {
        val batchSize = 3
        val urls = IntStream.range(1, 20).mapToObj { i: Int -> "http://example.com/$i" }
            .collect(Collectors.toList())
        val partitions = Lists.partition(urls, batchSize)
        
        for (i in 0 until partitions.size - 1) {
            Assertions.assertEquals(3, partitions[i].size)
        }
    }
}
