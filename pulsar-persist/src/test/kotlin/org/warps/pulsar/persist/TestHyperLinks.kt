package org.warps.pulsar.persist

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-6-29.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
class TestTypeLinks {

    private val links = arrayOf(
            "",
            "http://news.china.com/socialgd/10000169/20170629/1.html",
            "http://news.china.com/socialgd/10000169/20170629/2.html anchor2",
            "http://news.china.com/socialgd/10000169/20170629/3.html anchor3",
            "http://news.china.com/socialgd/10000169/20170629/4.html anchor4",
            "http://news.china.com/socialgd/10000169/20170629/5.html anchor5",
            "http://news.china.com/socialgd/10000169/20170629/10.html anchor10",
            "http://news.china.com/socialgd/10000169/20170629/10.html anchor10",
            "http://news.china.com/socialgd/10000169/20170629/10.html anchor10",
            "http://news.china.com/socialgd/10000169/20170629/10.html anchor10"
    )

    private val linksToRemove = arrayOf(
            "http://news.china.com/socialgd/10000169/20170629/5.html anchor5",
            "http://news.china.com/socialgd/10000169/20170629/10.html anchor10"
    )

    @Test
    fun testDistinct() {
        val hypeLinks = links.map { it -> HypeLink(it) }.filter { it -> it.url.isNotBlank() }.distinct().toList()
        assertTrue(hypeLinks.size < links.size)
    }

    @Test
    fun testRemove() {
        val hypeLinks = links.map { it -> HypeLink(it) }.filter { it -> it.url.isNotBlank() }.distinct().toMutableList()
        val hypeLinksToRemove = linksToRemove.map { it -> HypeLink(it) }.filter { it -> it.url.isNotBlank() }.distinct().toList()
        hypeLinks.removeAll(hypeLinksToRemove)
        assertTrue(hypeLinks.size < links.size)

        assertFalse(hypeLinks.contains(HypeLink.parse(linksToRemove[0])))
        assertFalse(hypeLinks.contains(HypeLink.parse(linksToRemove[1])))
    }
}
