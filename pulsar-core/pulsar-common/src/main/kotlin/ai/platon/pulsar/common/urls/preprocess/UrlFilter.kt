package ai.platon.pulsar.common.urls.preprocess

/**
 * The interface of url filter.
 *
 * A url filter is a function that takes a url and returns a filtered url.
 * */
interface UrlFilter : (String) -> String?

/**
 * An abstract url filter that does nothing.
 * */
abstract class AbstractUrlFilter : UrlFilter {
    /**
     * Filter the url, do nothing by default.
     *
     * @param url The url to be filtered
     * @return The filtered url, which is the same as the input url by default
     * */
    override fun invoke(url: String): String? {
        return url
    }
}

/**
 * A no-op url filter that does nothing.
 * */
class DefaultUrlFilter : AbstractUrlFilter() {
    /**
     * Filter the url, do nothing by this filter.
     *
     * @param url The url to be filtered
     * @return The filtered url, which is the same as the input url
     * */
    override fun invoke(url: String): String {
        return url
    }
}
