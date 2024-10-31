package ai.platon.pulsar.skeleton.crawl.filter

typealias NaiveUrlNormalizer = ai.platon.pulsar.common.urls.preprocess.UrlNormalizer

/**
 * Default scope. If no scope properties are defined then the configuration
 * for this scope will be used.
 */
const val SCOPE_DEFAULT: String = "default"
const val SCOPE_GENERATE = "generate"
const val SCOPE_INJECT = "inject"
const val SCOPE_FETCH = "fetch"

/**
 * Interface used to convert URLs to normal form and optionally perform
 * substitutions
 */
@Deprecated("Inappropriate name", ReplaceWith("ScopedUrlNormalizer"))
interface UrlNormalizer : NaiveUrlNormalizer {

    fun isRelevant(url: String, scope: String = SCOPE_DEFAULT): Boolean

    fun normalize(url: String, scope: String = SCOPE_DEFAULT): String?

    fun valid(urlString: String, scope: String): Boolean {
        return normalize(urlString, scope) != null
    }
}

interface ScopedUrlNormalizer : UrlNormalizer

abstract class AbstractScopedUrlNormalizer : ScopedUrlNormalizer {
    override fun isRelevant(url: String, scope: String): Boolean = false

    override fun invoke(url: String?) = url?.let { normalize(it) }

    abstract override fun normalize(url: String, scope: String): String?
}

class RegexUrlNormalizer(
    private val pattern: String,
    private val replacement: String
) : AbstractScopedUrlNormalizer() {
    private val regex = Regex(pattern)
    override fun normalize(url: String, scope: String): String? {
        // find each group in url that matches pattern, and replace the placeholder in replacement with it
        // for example, "^(http://www.baidu.com).*" will replace "$1" with "http://www.baidu.com"
        regex.matchEntire(url)?.groups?.let { groups ->
            return if (groups.size > 1) {
                replacement.replace(Regex("\\$\\d+"), groups[1]?.value ?: "")
            } else {
                null
            }
        }
        
        return null
    }
}
