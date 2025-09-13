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
open class Hyperlink constructor(
    /**
     * The url specification of the hyperlink, it is usually normalized, and can contain load arguments.
     * */
    url: String,
    /**
     * The anchor text is not always available and is not a required field.
     * It can easily be filled by args by mistake, so we require you to fill this field in the current version.
     * We plan to move this field to a later position in future versions.
     * */
    text: String,
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
     * The language of this hyperlink, reserved
     * */
    lang: String = "*",
    /**
     * The country of this hyperlink, reserved
     * */
    country: String = "*",
    /**
     * The district of this hyperlink, reserved
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
        url.url, text = "", order = 0, referrer = url.referrer, args = url.args, href = url.href,
        priority = url.priority, lang = url.lang, country = url.country, district = url.district,
        nMaxRetry = url.nMaxRetry, depth = url.depth
    )
    constructor(link: Hyperlink) : this(
        link.url, text = link.text, order = link.order, referrer = link.referrer, args = link.args, href = link.href,
        priority = link.priority, lang = link.lang, country = link.country, district = link.district,
        nMaxRetry = link.nMaxRetry, depth = link.depth
    )
    constructor(datum: HyperlinkDatum) : this(
        datum.url, text = datum.text, order = datum.order, referrer = datum.referrer, args = datum.args, href = datum.href,
        priority = datum.priority, lang = datum.lang, country = datum.country, district = datum.district,
        nMaxRetry = datum.nMaxRetry, depth = datum.depth
    )
    
    fun data() = toDatum()
    
    fun toDatum() = HyperlinkDatum(
        url = url, text = text, order = order, referrer = referrer, args = args, href = href,
        priority = priority, lang = lang, country = country, district = district, nMaxRetry = nMaxRetry, depth = depth
    )
    
    /**
     * Serialize the hyperlink to a string in command line style.
     *
     * TODO: can handle only simple string values, need to be fixed
     *
     * @return the serialized string
     * */
    override fun serializeTo(sb: StringBuilder): StringBuilder {
        sb.append(url)
        
        if (!isDefault("text")) sb.append(" -text $text")
        if (!isDefault("order")) sb.append(" -order $order")
        if (!isDefault("referrer")) sb.append(" -referrer $referrer")
        if (!isDefault("args")) sb.append(" -args $args")
        if (!isDefault("href")) sb.append(" -href $href")
        if (!isDefault("priority")) sb.append(" -priority $priority")
        if (!isDefault("lang")) sb.append(" -lang $lang")
        if (!isDefault("country")) sb.append(" -country $country")
        if (!isDefault("district")) sb.append(" -district $district")
        if (!isDefault("nMaxRetry")) sb.append(" -nMaxRetry $nMaxRetry")
        if (!isDefault("depth")) sb.append(" -depth $depth")
        
        return sb
    }
    
    /**
     * Check if the option value is the default.
     * */
    open fun isDefault(fieldName: String): Boolean {
        // get value by field name
        return when (fieldName) {
            "url" -> url == DEFAULT.url
            "text" -> text == DEFAULT.text
            "order" -> order == DEFAULT.order
            "referrer" -> referrer == DEFAULT.referrer
            "args" -> args == DEFAULT.args
            "href" -> href == DEFAULT.href
            "priority" -> priority == DEFAULT.priority
            "lang" -> lang == DEFAULT.lang
            "country" -> country == DEFAULT.country
            "district" -> district == DEFAULT.district
            "nMaxRetry" -> nMaxRetry == DEFAULT.nMaxRetry
            "depth" -> depth == DEFAULT.depth
            else -> throw IllegalArgumentException("Unknown field name: $fieldName")
        }
    }

    companion object {
        val EXPECTED_FIELDS = listOf(
            "url", "text", "order", "referrer", "args", "href", "priority", "lang", "country", "district", "nMaxRetry", "depth"
        )

        val DEFAULT = Hyperlink("")

        val EXAMPLE = Hyperlink(AppConstants.EXAMPLE_URL)
        
        fun create(url: String?): Hyperlink? {
            return when {
                url == null -> null
                !URLUtils.isStandard(url) -> null
                else -> Hyperlink(url)
            }
        }
        
        /**
         * Parse a hyperlink from a string in command line style.
         *
         * TODO: can handle only simple string values, need to be fixed
         *
         * * @param linkText the string to parse
         * @return the parsed hyperlink
         * */
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