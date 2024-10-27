
package ai.platon.pulsar.filter.common

/**
 * A generic regular expression rule.
 *
 * @author Jrme Charron
 */

/**
 * Constructs a new regular expression rule.
 *
 * @param sign
 * specifies if this rule must filter-in or filter-out. A
 * `true` value means that any url matching this rule must
 * be accepted, a `false` value means that any url
 * matching this rule must be rejected.
 * @param regex
 * is the regular expression used for matching (see
 * [.match] method).
 */
abstract class RegexRule(private val sign: Boolean, regex: String) {
    /**
     * Return if this rule is used for filtering-in or out.
     *
     * @return `true` if any url matching this rule must be accepted,
     * otherwise `false`.
     */
    fun accept(): Boolean {
        return sign
    }

    /**
     * Checks if a url matches this rule.
     *
     * @param url
     * is the url to check.
     * @return `true` if the specified url matches this rule, otherwise
     * `false`.
     */
    abstract fun match(url: String): Boolean
}