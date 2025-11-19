package ai.platon.pulsar.common

import org.apache.commons.lang3.StringUtils

object KStrings {

    fun replaceContentInSections(input: String, boundaries: List<Pair<String, String>>, replacement: String): String {
        var compacted = input
        boundaries.map { (a, b) ->
            val substr = StringUtils.substringBetween(compacted, a, b)
            if (substr != null) {
                compacted = compacted.replace(substr, replacement)
            }
        }

        return compacted
    }
}
