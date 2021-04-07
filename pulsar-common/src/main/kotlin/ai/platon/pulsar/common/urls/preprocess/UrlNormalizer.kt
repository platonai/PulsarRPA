package ai.platon.pulsar.common.urls.preprocess

interface UrlNormalizer : (String?) -> String?

abstract class AbstractUrlNormalizer : UrlNormalizer {
    override fun invoke(url: String?): String? {
        return url
    }
}

class DefaultUrlNormalizer : AbstractUrlNormalizer() {
    override fun invoke(url: String?): String? {
        return url
    }
}

class StripQueryUrlNormalizer : AbstractUrlNormalizer() {
    override fun invoke(url: String?): String? {
        return url?.substringAfter("?")?.trimEnd('?')
    }
}

class UrlNormalizerPipeline(val normalizers: MutableList<UrlNormalizer>) : AbstractUrlNormalizer() {
    override fun invoke(url: String?): String? {
        var normalizedUrl: String? = url
        normalizers.forEach {
            normalizedUrl = it.invoke(normalizedUrl)
        }
        return normalizedUrl
    }
}
