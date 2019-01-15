package `fun`.platonic.pulsar.dom.model

import `fun`.platonic.pulsar.dom.nodes.height
import `fun`.platonic.pulsar.dom.nodes.width
import org.apache.commons.lang3.StringUtils
import org.jsoup.nodes.Element
import java.util.*

val imageSuffixes = arrayOf("jpg", "jpeg", "png", "gif", "webp")

const val imageLoading = "data:image/gif;base64,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"
const val imageDot = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAANSURBVBhXYzh8+PB/AAffA0nNPuCLAAAAAElFTkSuQmCC"

data class Image(val attributes: TreeMap<String, String> = TreeMap()) {
    override fun toString(): String {
        return attributes.entries.joinToString(" ", "<img ", " />") { "${it.key}='${it.value}'" }
    }
}

data class Link(
        var text: String = "",
        val attributes: TreeMap<String, String> = TreeMap(),
        var image: Image? = null
) {
    override fun toString(): String {
        val imageString = image?.toString()?:""
        return attributes.entries
                .joinToString(" ", "<a ", ">$imageString$text</a>") { "${it.key}='${it.value}'" }
    }
}

data class Website(val domain: String, val name: String)

/**
 * Create an anchor with an optional image
 *
 * Lazy attributes are compatible with https://appelsiini.net/projects/lazyload/v1/
 * */
fun createLink(ele: Element, keepMetadata: Boolean = true, lazy: Boolean = false): Link {
    val link = Link()

    if (!ele.tagName().equals("a", ignoreCase = true))
        return link

    val image = ele.getElementsByTag("img").first()
    if (image != null) {
        link.image = createImage(image, keepMetadata = keepMetadata, lazy = lazy)
    }

    link.text = sniffLinkText(ele, image)
    val href = ele.absUrl("href")
    if (!href.isNullOrBlank()) {
        link.attributes["href"] = href
    }

    if (keepMetadata) {
        buildAttributes(ele, link)
    }

    return link
}

/**
 * Create a image
 *
 * Lazy attributes are compatible with https://appelsiini.net/projects/lazyload/v1/
 * */
fun createImage(ele: Element, keepMetadata: Boolean = true, lazy: Boolean = false): Image {
    val image = Image()

    if (ele.tagName() != "img") {
        return image
    }

    val ignoredAttrs = Arrays.asList("id", "class", "style")

    // calculate attributes
    var lazySrc: String? = null
    for (attr in ele.attributes()) {
        val name = attr.key
        var value = attr.value

        if (ignoredAttrs.contains(name)) {
            continue
        }

        if (name.startsWith("data-") && value == "0") {
            continue
        }

        // TODO : this is tricky to found out lazy and actual image src
        if (maybeUrl(name, value)) {
            val absUrl = ele.absUrl(name)

            if (name.contains("lazy")) {
                lazySrc = absUrl
            }

            // data-src is a better candidate than lazy
            if (name.contains("data-src")) {
                lazySrc = absUrl
            }

            value = absUrl
        }

        if (value.endsWith(".js")) {
            value += ".rename"
        }

        if (keepMetadata && name.isNotEmpty() && value.isNotEmpty()) {
            image.attributes[name] = value
        }
    }

    if (lazy) {
        // compatible with https://appelsiini.net/projects/lazyload/v1/
        val w = ele.width
        val h = ele.height
        image.attributes["alt"] = "img[$w x $h]"
        image.attributes["class"] = image.attributes["class"]?.let { "$it lazy" }?:"lazy"
        var src = image.attributes["src"]?:""
        src = if (lazySrc == null || lazySrc.isBlank()) src else lazySrc
        if (src.isNotBlank()) {
            image.attributes["data-original"] = src
        }
        image.attributes["src"] = imageDot
    } else {
        image.attributes["src"] = lazySrc ?: ""
    }

    return image
}

private fun buildAttributes(ele: Element, link: Link) {
    val ignoredAttrs = Arrays.asList("id", "class", "style", "_target", "target", "title")

    for (attr in ele.attributes()) {
        val name = attr.key
        var value = attr.value

        if (ignoredAttrs.contains(name)) {
            continue
        }

        // TODO : tricky? site specified?
        if (name.startsWith("data-") && value == "0") {
            continue
        }

        if (maybeUrl(name, value)) {
            // TODO : better sniff strategy
            value = ele.absUrl(name)
        }

        if (value.endsWith(".js")) {
            value += ".rename"
        }

        if (!name.isEmpty() && !value.isEmpty()) {
            link.attributes[name] = value
        }
    }
}

fun sniffLinkText(link: Element, image: Element?): String {
    var text: String? = StringUtils.trimToNull(link.text())
    if (text == null)
        text = StringUtils.trimToNull(link.attr("title"))
    if (text == null && image != null)
        text = StringUtils.trimToNull(image.attr("alt"))

    return text ?: ""
}

fun maybeUrl(attrName: String, attrValue: String): Boolean {
    val urlAttrs = Arrays.asList("src", "url", "data-src", "data-url")

    if (urlAttrs.contains(attrName))
        return true
    if (attrValue.contains("http://"))
        return true
    return StringUtils.countMatches(attrValue, "/") > 3
}

/**
 * pseudo links :
 * href="#comment"
 * href="javascript:;"
 * href="void:;"
 */
fun isPseudoLink(href: String): Boolean {
    return href.startsWith("#") && !href.startsWith("java") && !href.startsWith("void")
}
