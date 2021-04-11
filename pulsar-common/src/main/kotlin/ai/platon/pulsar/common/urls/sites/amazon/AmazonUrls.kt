package ai.platon.pulsar.common.urls.sites.amazon

import ai.platon.pulsar.common.urls.preprocess.AbstractUrlNormalizer
import ai.platon.pulsar.common.urls.Urls
import org.apache.commons.lang3.StringUtils
import java.net.URLEncoder
import java.nio.charset.Charset

object AmazonUrls {
    val charset = Charset.defaultCharset()

    fun isAmazon(url: String): Boolean {
        return url.contains(".amazon.")
    }

    /**
     * Site specific, should be moved to a better place
     * */
    fun isAmazonIndexPage(url: String): Boolean {
        val indexPagePatterns = arrayOf("/zgbs/", "/most-wished-for/", "/new-releases/", "/movers-and-shakers/")
        return isAmazon(url) && (indexPagePatterns.any { url.contains(it) })
    }

    /**
     * Site specific, should be moved to a better place
     * */
    fun isAmazonItemPage(url: String): Boolean {
        return isAmazon(url) && url.contains("/dp/")
    }

    fun findAsin(url: String, open: String = "/dp/", close: String = "/ref="): String? {
        var open0 = open
        var close0 = close

        var asin: String? = url.substringAfterLast(open0, "")
        if (asin != null && asin.isNotBlank() && asin.indexOf('/') == -1) {
            return asin
        }

        asin = StringUtils.substringBetween(url, open0, close0)
        if (asin != null) {
            return asin
        }

        close0 = "?ref="
        asin = StringUtils.substringBetween(url, open0, close0)
        if (asin != null) {
            return asin
        }

        open0 = URLEncoder.encode(open0, charset)
        close0 = URLEncoder.encode("/ref=", charset)
        asin = StringUtils.substringBetween(url, open0, close0)
        if (asin != null) {
            return asin
        }

        close0 = URLEncoder.encode("?ref=", charset)
        asin = StringUtils.substringBetween(url, open0, close0)
        if (asin != null) {
            return asin
        }

        return asin
    }
}

class AsinUrlNormalizer: AbstractUrlNormalizer() {
    override fun invoke(url: String?): String? {
        if (url == null) return null

        if (!AmazonUrls.isAmazon(url)) {
            return url
        }

        val u = Urls.getURLOrNull(url) ?: return null
        val asin = AmazonUrls.findAsin(url) ?: return null
        return u.protocol + "://" + u.host + "/dp/" + asin
    }
}
