package ai.platon.pulsar.common

import com.google.common.collect.Lists
import kotlin.test.Test
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.annotation.Nonnull
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-1-14.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
class TestJavaFeatures {
    @Nonnull
    fun f(): String {
        return ""
    }
    
    @Test
    fun test() {
        println(" "[0])
        println(Strings.NBSP[0])
    }
    
    @Test
    fun testReflection() {
        val counter: MutableMap<String, String> = TreeMap()
        
        counter["a"] = "1"
        counter["b"] = "2"
        counter["c"] = "3"
        
        // System.out.println(counter);
        val obj: Any = counter
        assertTrue(obj.javaClass.isAssignableFrom(TreeMap::class.java))
        assertTrue(obj is Map<*, *>)
        assertFalse(obj.javaClass.isAssignableFrom(MutableMap::class.java))
        
        if (obj.javaClass.isAssignableFrom(MutableMap::class.java)) {
            println(obj.javaClass.toString() + " isAssignableFrom " + counter.javaClass)
        }
        
        for (method in this.javaClass.methods) {
            println(method)
            for (annotation in method.declaredAnnotations) {
                println(annotation)
            }
        }
    }
    
    @Test
    fun testEnum() {
        assertEquals(10, E.a.value.toLong())
        assertEquals("a", E.a.toString())
        assertEquals(E.a.name, E.a.toString())
    }
    
    @Test
    fun testList() {
        var l = IntStream.range(0, 200).boxed().collect(Collectors.toList())
        l = l.subList(l.size - 10, l.size)
        println(l)
        
        Collections.reverse(l)
        l.addAll(0, Lists.newArrayList(-1, -2, -3, -4))
        println(l)
    }
    
    @Test
    fun testRemoveCommentsInSQL() {
        val sql =
            "-- This is a comment\nSELECT DISTINCT DOM_absHref(`dom`) /** absolute href */ AS href FROM DOMT_links(DOM_load('https://www.baidu.com/s?tn=baidurt&rtt=4&wd=***/&bsst=/***&ie=utf-8/***/'));"
        println(sql.replace("('(''|[^'])*')|[\\t\\r\\n]|(--[^\\r\\n]*)|(/\\*[\\w\\W]*?(?=\\*/)\\*/)".toRegex(), ""))
    }
    
    internal enum class E(val value: Int) {
        a(10), b(20), c(30)
    }
    
    companion object {
        @Nonnull
        fun g(): String {
            return ""
        }
    }
}
