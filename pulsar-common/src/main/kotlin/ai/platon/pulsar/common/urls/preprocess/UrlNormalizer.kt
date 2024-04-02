package ai.platon.pulsar.common.urls.preprocess

/**
 * The interface of url normalizer.
 *
 * A url normalizer is a function that takes a url and returns a normalized url.
 * */
interface UrlNormalizer : (String?) -> String?

/**
 * An abstract url normalizer that does nothing.
 * */
abstract class AbstractUrlNormalizer : UrlNormalizer {
    /**
     * Normalize the url, do nothing by default.
     *
     * @param url The url to be normalized
     * @return The normalized url, which is the same as the input url by default
     * */
    override fun invoke(url: String?): String? {
        return url
    }
}

/**
 * A no-op url normalizer that does nothing.
 * */
class DefaultUrlNormalizer : AbstractUrlNormalizer() {
    /**
     * Normalize the url, do nothing by this normalizer.
     *
     * @param url The url to be normalized
     * @return The normalized url, which is the same as the input url
     * */
    override fun invoke(url: String?): String? {
        return url
    }
}

/**
 * Normalize the url by removing the query part
 * */
class StripQueryUrlNormalizer : AbstractUrlNormalizer() {
    /**
     * Normalize the url by removing the query part
     *
     * @param url The url to be normalized
     * @return The normalized url
     * */
    override fun invoke(url: String?): String? {
        return url?.substringAfter("?")?.trimEnd('?')
    }
}

/**
 * A pipeline of url normalizers
 * */
class UrlNormalizerPipeline(
    /**
     * The list of url normalizers
     * */
    val normalizers: MutableList<UrlNormalizer> = mutableListOf()
) : AbstractUrlNormalizer() {

    /**
     * Add a url normalizer to the first of the pipeline
     *
     * @param normalizer The url normalizer
     * */
    fun addFirst(normalizer: UrlNormalizer) {
        normalizers.add(0, normalizer)
    }

    /**
     * Add a url normalizer to the last of the pipeline
     *
     * @param normalizer The url normalizer
     * */
    fun addLast(normalizer: UrlNormalizer) {
        normalizers.add(normalizer)
    }

    /**
     * Invoke the pipeline to normalize the url.
     *
     * @url The url to be normalized
     * */
    override fun invoke(url: String?): String? {
        var normalizedUrl: String? = url
        normalizers.forEach {
            normalizedUrl = it.invoke(normalizedUrl)
        }
        return normalizedUrl
    }
}
