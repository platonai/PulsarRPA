
package ai.platon.pulsar.normalizer

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.KConfigurable
import ai.platon.pulsar.common.urls.UrlUtils.getURLOrNull
import ai.platon.pulsar.skeleton.crawl.filter.AbstractScopedUrlNormalizer
import ai.platon.pulsar.skeleton.crawl.filter.UrlNormalizer
import org.apache.oro.text.regex.*
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL

/**
 * Converts URLs to a normal form:
 *
 *  * remove dot segments in path: `/./` or `/../`
 *  * remove default ports, e.g. 80 for protocol `http://`
 *
 */
class BasicUrlNormalizer(override var conf: ImmutableConfig) : KConfigurable, AbstractScopedUrlNormalizer() {
    val LOG = LoggerFactory.getLogger(BasicUrlNormalizer::class.java)

    private val relativePathRule: Rule = Rule()
    private val leadingRelativePathRule: Rule = Rule()
    private val currentPathRule: Rule = Rule()
    private val adjacentSlashRule: Rule = Rule()
    private val compiler = Perl5Compiler()
    private val matchers: ThreadLocal<Perl5Matcher> = object : ThreadLocal<Perl5Matcher>() {
        override fun initialValue(): Perl5Matcher {
            return Perl5Matcher()
        }
    }

    init {
        try {
            // this pattern tries to find spots like "/xx/../" in the url, which
            // could be replaced by "/" xx consists of chars, different then "/"
            // (slash) and needs to have at least one char different from "."
            relativePathRule.pattern = compiler.compile(
                    "(/[^/]*[^/.]{1}[^/]*/\\.\\./)", Perl5Compiler.READ_ONLY_MASK) as Perl5Pattern
            relativePathRule.substitution = Perl5Substitution("/")
            // this pattern tries to find spots like leading "/../" in the url,
            // which could be replaced by "/"
            leadingRelativePathRule.pattern = compiler.compile(
                    "^(/\\.\\./)+", Perl5Compiler.READ_ONLY_MASK) as Perl5Pattern
            leadingRelativePathRule.substitution = Perl5Substitution("/")
            // this pattern tries to find spots like "/./" in the url,
            // which could be replaced by "/"
            currentPathRule.pattern = compiler.compile("(/\\./)", Perl5Compiler.READ_ONLY_MASK) as Perl5Pattern
            currentPathRule.substitution = Perl5Substitution("/")
            // this pattern tries to find spots like "xx//yy" in the url,
            // which could be replaced by a "/"
            adjacentSlashRule.pattern = compiler.compile("/{2,}", Perl5Compiler.READ_ONLY_MASK) as Perl5Pattern
            adjacentSlashRule.substitution = Perl5Substitution("/")
        } catch (e: MalformedPatternException) {
            throw RuntimeException(e)
        }
    }
    
    override fun normalize(url: String, scope: String): String? {
        var urlString: String = url

        val u = getURLOrNull(urlString) ?: return null
        val protocol = u.protocol
        var host = u.host
        var port = u.port
        var file = u.file
        var changed = false
        if (!urlString.startsWith(protocol)) // protocol was lowercase
            changed = true
        if ("http" == protocol || "https" == protocol || "ftp" == protocol) {
            if (host != null) {
                val newHost = host.toLowerCase() // lowercase host
                if (host != newHost) {
                    host = newHost
                    changed = true
                }
            }
            if (port == u.defaultPort) { // uses default port
                port = -1 // so don't specify it
                changed = true
            }
            if (file == null || "" == file) { // add a slash
                file = "/"
                changed = true
            }
            if (u.ref != null) { // remove the ref
                changed = true
            }
            // check for unnecessary use of "/../"
            val file2 = substituteUnnecessaryRelativePaths(file)
            if (file != file2) {
                changed = true
                file = file2
            }
        }
        if (changed) {
            try {
                urlString = URL(protocol, host, port, file).toString()
            } catch (e: MalformedURLException) {
                LOG.warn(e.message)
                return null
            }
        }

        return urlString
    }

    private fun substituteUnnecessaryRelativePaths(file: String): String {
        var fileWorkCopy = file
        var oldLen = file.length
        var newLen = oldLen - 1
        // All substitutions will be done step by step, to ensure that certain
        // constellations will be normalized, too
        //
        // For example: "/aa/bb/../../cc/../foo.html will be normalized in the
        // following manner:
        // "/aa/bb/../../cc/../foo.html"
        // "/aa/../cc/../foo.html"
        // "/cc/../foo.html"
        // "/foo.html"
        //
        // The normalization also takes care of leading "/../", which will be
        // replaced by "/", because this is a rather a sign of bad webserver
        // configuration than of a wanted link. For example, urls like
        // "http://www.foo.com/../" should return a http 404 error instead of
        // redirecting to "http://www.foo.com".
        //
        val matcher = matchers.get()
        while (oldLen != newLen) {
            // substitue first occurence of "/xx/../" by "/"
            oldLen = fileWorkCopy.length
            fileWorkCopy = Util.substitute(matcher, relativePathRule.pattern, relativePathRule.substitution, fileWorkCopy, 1)
            // remove leading "/../"
            fileWorkCopy = Util.substitute(matcher, leadingRelativePathRule.pattern, leadingRelativePathRule.substitution, fileWorkCopy, 1)
            // remove unnecessary "/./"
            fileWorkCopy = Util.substitute(matcher, currentPathRule.pattern, currentPathRule.substitution, fileWorkCopy, 1)
            // collapse adjacent slashes with "/"
            fileWorkCopy = Util.substitute(matcher, adjacentSlashRule.pattern, adjacentSlashRule.substitution, fileWorkCopy, 1)
            newLen = fileWorkCopy.length
        }
        return fileWorkCopy
    }

    /**
     * Class which holds a compiled pattern and its corresponding substition
     * string.
     */
    private class Rule {
        var pattern: Perl5Pattern? = null
        var substitution: Perl5Substitution? = null
    }
}
