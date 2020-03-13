package ai.platon.pulsar.crawl.filter

import ai.platon.pulsar.common.Strings
import com.google.gson.annotations.Expose
import org.apache.commons.lang3.StringUtils

class TextFilter {
    @Expose
    var contains: String? = null
    @Expose
    var containsAny: String? = null
    @Expose
    var notContains: String? = null
    @Expose
    var containsNone: String? = null
    private var splitted = false
    private var _contains: Array<String> = arrayOf()
    private var _containsAny: Array<String> = arrayOf()
    private var _notContains: Array<String> = arrayOf()
    private var _containsNone: Array<String> = arrayOf()

    fun test(text: String?): Boolean {
        buildCache()
        if (_contains.isNotEmpty() && !Strings.contains(text, *_contains)) {
            return false
        }
        if (_containsAny.isNotEmpty() && Strings.containsNone(text, *_containsAny)) {
            return false
        }
        if (_notContains.isNotEmpty() && Strings.contains(text, *_notContains)) {
            return false
        }
        return !(_containsNone.isNotEmpty() && Strings.containsAny(text, *_containsNone))
    }

    private fun buildCache() {
        if (!splitted) {
            if (contains != null) _contains = StringUtils.split(contains, SEPERATORS)
            if (containsAny != null) _containsAny = StringUtils.split(containsAny, SEPERATORS)
            if (notContains != null) _notContains = StringUtils.split(notContains, SEPERATORS)
            if (containsNone != null) _containsNone = StringUtils.split(containsNone, SEPERATORS)
            splitted = true
        }
    }

    override fun toString(): String {
        return ("\n\tcontains : " + contains + "\n\tcontainsAny : "
                + containsAny + "\n\tnotContains : " + notContains + "\n\tcontainsNone : " + containsNone)
    }

    companion object {
        const val SEPERATORS = ",， "
    }
}
