package ai.platon.pulsar.common.urls

import ai.platon.pulsar.common.config.AppConstants

/**
 * A [hyperlink](https://en.wikipedia.org/wiki/Hyperlink), or simply a link, is a reference to data that the user can
 * follow by clicking or tapping.
 *
 * A hyperlink points to a whole document or to a specific element within a document.
 * Hypertext is text with hyperlinks. The text that is linked from is called anchor text.
 *
 * The [anchor text](https://en.wikipedia.org/wiki/Anchor_text), link label or link text is the visible,
 * clickable text in an HTML hyperlink.
 * */
open class Hyperlink(
    /**
     * The url specification of the hyperlink, it is usually normalized, and can contain load arguments.
     * */
    url: String,
    /**
     * The anchor text
     * */
    text: String = "",
    /**
     * The order of this hyperlink in it referrer page
     * */
    order: Int = 0,
    /**
     * The url of the referrer page
     * */
    referrer: String? = null,
    /**
     * The additional url arguments
     * */
    args: String? = null,
    /**
     * The hypertext reference, It defines the address of the document, which this time is linked from
     * */
    href: String? = null,
    /**
     * The priority of this hyperlink
     * */
    priority: Int = 0,
    /**
     * The language of this hyperlink
     * */
    lang: String = "*",
    /**
     * The country of this hyperlink
     * */
    country: String = "*",
    /**
     * The district of this hyperlink
     * */
    district: String = "*",
    /**
     * The maximum number of retries
     * */
    nMaxRetry: Int = 3,
    /**
     * The depth of this hyperlink
     * */
    depth: Int = 0
) : AbstractUrl(url, text, order, referrer, args, href, priority, lang, country, district, nMaxRetry, depth) {
    /**
     * Construct a hyperlink.
     *
     * This method is compatible with Java.
     * */
    constructor(url: String) : this(url, "", 0)
    
    constructor(url: UrlAware) : this(
        url.url, "", 0, url.referrer, url.args, href = url.href,
        priority = url.priority, lang = url.lang, country = url.country, district = url.district,
        nMaxRetry = url.nMaxRetry, depth = url.depth
    )
    constructor(url: Hyperlink) : this(
        url.url, url.text, url.order, url.referrer, url.args, href = url.href,
        priority = url.priority, lang = url.lang, country = url.country, district = url.district, nMaxRetry = url.nMaxRetry, depth = url.depth
    )
    constructor(url: HyperlinkDatum) : this(
        url.url, url.text, url.order, url.referrer, url.args, href = url.href,
        priority = url.priority, lang = url.lang, country = url.country, district = url.district, nMaxRetry = url.nMaxRetry, depth = url.depth
    )
    
    fun data() = HyperlinkDatum(
        url = url, text = text, order = order, referrer = referrer, args = args, href = href,
        priority = priority, lang = lang, country = country, district = district, nMaxRetry = nMaxRetry, depth = depth
    )
    
    override fun serializeTo(sb: StringBuilder): StringBuilder {
        sb.append(url)
        
        args?.takeIf { it.isNotBlank() }?.replace("\"", "\\\"")
            ?.let { sb.append(" -args ").append(it) }
        text.takeUnless { it.isEmpty() }?.let { sb.append(" -text ").append(it) }
        order.takeUnless { it == 0 }?.let { sb.append(" -order ").append(it) }
        href?.let { sb.append(" -href ").append(it) }
        referrer?.let { sb.append(" -referrer ").append(it) }
        priority.takeIf { it != 0 }?.let { sb.append(" -priority ").append(it) }
        lang.takeIf { it != "*" }?.let { sb.append(" -lang ").append(it) }
        country.takeIf { it != "*" }?.let { sb.append(" -country ").append(it) }
        district.takeIf { it != "*" }?.let { sb.append(" -district ").append(it) }
        nMaxRetry.takeIf { it != 3 }?.let { sb.append(" -nMaxRetry ").append(it) }
        depth.takeUnless { it == 0 }?.let { sb.append(" -depth ").append(it) }
        
        return sb
    }
    
    /**
     * Check if the option value is the default.
     * */
    open fun isDefault(fieldName: String): Boolean {
        // get value by field name
        return when (fieldName) {
            "url" -> url == EXAMPLE.url
            "text" -> text == EXAMPLE.text
            "order" -> order == EXAMPLE.order
            "referrer" -> referrer == EXAMPLE.referrer
            "args" -> args == EXAMPLE.args
            "href" -> href == EXAMPLE.href
            "priority" -> priority == EXAMPLE.priority
            "lang" -> lang == EXAMPLE.lang
            "country" -> country == EXAMPLE.country
            "district" -> district == EXAMPLE.district
            "nMaxRetry" -> nMaxRetry == EXAMPLE.nMaxRetry
            "depth" -> depth == EXAMPLE.depth
            else -> throw IllegalArgumentException("Unknown field name: $fieldName")
        }
    }

    companion object {
        val EXPECTED_FIELDS = listOf(
            "url", "text", "order", "referrer", "args", "href", "priority", "lang", "country", "district", "nMaxRetry", "depth"
        )
        
        val EXAMPLE = Hyperlink(AppConstants.EXAMPLE_URL)
        
        fun parse(linkText: String): Hyperlink {
            var url = ""
            var text = EXAMPLE.text
            var args: String? = EXAMPLE.args
            var href: String? = EXAMPLE.href
            var referrer: String? = EXAMPLE.referrer
            var order = EXAMPLE.order
            var priority = EXAMPLE.priority
            var lang = EXAMPLE.lang
            var country = EXAMPLE.country
            var district = EXAMPLE.district
            var nMaxRetry = EXAMPLE.nMaxRetry
            var depth = EXAMPLE.depth

            val parts = linkText.split("\\s+".toRegex())
            url = parts[0]

            var i = 0
            while (i < parts.size - 1) {
                when(parts[i]) {
                    "-text" -> text = parts[i + 1]
                    "-args" -> args = parts[i + 1]
                    "-href" -> href = parts[i + 1]
                    "-referrer" -> referrer = parts[i + 1]
                    "-order" -> order = parts[i + 1].toIntOrNull()?: order
                    "-priority" -> priority = parts[i + 1].toIntOrNull()?: priority
                    "-lang" -> lang = parts[i + 1]
                    "-country" -> country = parts[i + 1]
                    "-district" -> district = parts[i + 1]
                    "-nMaxRetry" -> nMaxRetry = parts[i + 1].toIntOrNull()?: nMaxRetry
                    "-depth" -> depth = parts[i + 1].toIntOrNull()?: depth
                }
                
                i++
            }

            return Hyperlink(
                url,
                text,
                order,
                referrer,
                args,
                href,
                priority,
                lang,
                country,
                district,
                nMaxRetry,
                depth
            )
        }
    }
}