package ai.platon.pulsar.common

import com.google.common.collect.Sets
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-1-14.
 */
class TestJson {
    var urls = arrayOf(
            "http://sz.sxrb.com/sxxww/dspd/szpd/bwch/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/fcjjjc/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/hydt/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/jykj_0/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/qcjt/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/wsjk/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/wyss/",
            "http://sz.sxrb.com/sxxww/dspd/szpd/zjaq/"
    )

    @Test
    fun testCollection() {
        val gson = GsonBuilder().create()
        val json = gson.toJson(Sets.newHashSet(*urls))
        urls.forEach { url ->
            assertTrue(url) { json.contains(url) }
        }
    }

    @Test
    fun testRawString() {
        val seed = "http://www.sxrb.com/sxxww/\t-i pt1s -p"
        val gson = GsonBuilder().create()
        assertEquals("\"http://www.sxrb.com/sxxww/\\t-i pt1s -p\"", gson.toJson(seed))
    }

    @Test
    fun testToArrayArray() {
        val rules = """
            [
                ["/dp/",  500_000, 20, "x-asin.sql", "asin_sync_utf8mb4"],
                ["/seller/",  100_000, 8, "x-sellers.sql", "seller_sync"],
                ["/product-reviews/",  100_000, 10, "x-product-reviews.sql", "asin_review_sync"],
                ["/best-sellers/",  100_000, 5, "x-asin-best-sellers.sql", "asin_best_sellers_sync"],
                ["/new-releases/",  100_000, 5, "x-asin-new-releases.sql", "asin_new_releases_sync"],
                ["/movers-and-shakers/",  100_000, 5, "x-asin-movers-and-shakers.sql", "asin_movers_and_shakers_sync"],
                ["/most-wished-for/",  100_000, 5, "x-asin-most-wished-for.sql", "asin_most_wished_for_sync"]
            ]
        """.trimIndent()
        val array = Gson().fromJson(rules, Array<Array<Any>>::class.java)
        assertTrue { array.size == 7 }
        assertEquals("500_000", array[0][1])
    }
}
