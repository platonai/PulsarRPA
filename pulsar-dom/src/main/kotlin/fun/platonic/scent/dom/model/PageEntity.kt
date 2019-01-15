package `fun`.platonic.scent.dom.model

import `fun`.platonic.pulsar.common.config.PulsarConstants.NIL_PAGE_URL
import java.util.*

val METADATA = FragmentCategory("Metadata")
val PREDEFINED = FragmentCategory("PreDefined")
val CANDIDATE = FragmentCategory("Candidate")
val REGEX_EXTRACTED = FragmentCategory("RegexExtracted")
val MANUAL_RULE_EXTRACTED = FragmentCategory("ManualRuleExtracted")

const val META_DOMAIN = "Meta-Domain"
const val META_BASE_URI = "Meta-BaseUri"
const val META_LINK = "Meta-Link"
const val META_LOCATION = "Meta-Location"
const val META_TITLE = "Meta-Title"
const val META_KEYWORDS = "Meta-Keywords"
const val META_DESCRIPTION = "Meta-Description"
const val META_MOBILE_AGENT = "Meta-MobileAgent"
const val META_DOWNLOAD_AT = "Meta-DownloadAt"
const val META_EXTRACTED_AT = "Meta-ExtractedAt"
const val META_DIAGNOSER = "Meta-Diagnoser"

data class FragmentCategory(val name: String = "", val category: FragmentCategory? = null) {
    val fullName: String
        get() = "${category?.fullName}/$name"

    override fun toString(): String {
        return fullName
    }
}

data class PageAttribute(
        val name: String,
        var value: String,
        var richText: String = value,
        var extractor: String? = null,
        var valuePath: String? = null,
        var category: FragmentCategory? = null,
        val fullName: String = "${category?.fullName}/$name",
        val labels: HashSet<String> = HashSet()
)

class PageEntity(baseUri_: String = NIL_PAGE_URL, var category: FragmentCategory? = null) {

    val attributes = HashMap<String, PageAttribute>()
    val metadata = HashMap<String, String>()
    val nestedPages = HashSet<PageEntity>()

    val size: Int get() = attributes.size

    var baseUri: String = baseUri_
        set(value) {
            field = value
            put(META_BASE_URI, baseUri, METADATA)
        }

    var title: String = ""
        set(value) {
            field = value
            put(META_TITLE, title, METADATA)
        }

    var location: String = ""
        set(location) {
            field = location
            put(META_LOCATION, location, METADATA)
        }

    fun isEmpty(): Boolean {
        return attributes.isEmpty()
    }

    fun put(name: String, value: String, category: FragmentCategory? = null): PageAttribute {
        return add(PageAttribute(name, value, category = category))
    }

    fun put(name: String, value: String, richText: String, category: FragmentCategory? = null): PageAttribute {
        return add(PageAttribute(name, value, richText, category = category))
    }

    fun add(attribute: PageAttribute): PageAttribute {
        attributes[attribute.fullName] = attribute
        return attribute
    }

    fun addAll(attributes: Iterable<PageAttribute>) {
        for (attribute in attributes) {
            add(attribute)
        }
    }

    fun add(pageEntity: PageEntity): Boolean {
        return nestedPages.add(pageEntity)
    }

    operator fun get(name: String): Set<PageAttribute> {
        return attributes.values.filter { it.name == name }.toSet()
    }

    operator fun get(name: String, value: String): Set<PageAttribute> {
        return attributes.values.filter { it.name == name && it.value == value }.toSet()
    }

    operator fun get(name: String, value: String, category: FragmentCategory): Set<PageAttribute> {
        return attributes.values.filter { it.name == name && it.value == value && it.category == category }.toSet()
    }

    fun filterBy(category: FragmentCategory): List<PageAttribute> {
        return attributes.values.filter { it.fullName.contains(category.name) }
    }

    fun first(name: String): PageAttribute? {
        return get(name).firstOrNull()
    }

    fun firstValue(name: String): String? {
        return first(name)?.value
    }

    fun firstText(name: String): String {
        return firstValue(name) ?: ""
    }

    fun joinToString(name: String, sep: String = ", "): String {
        return get(name).joinToString { sep }
    }

    override fun hashCode(): Int {
        return baseUri.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is PageEntity && baseUri.equals(other.baseUri)
    }
}

fun PageAttribute.hasCategory(category: String): Boolean {
    return this.fullName.contains("/$category/")
}
