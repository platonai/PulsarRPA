package ai.platon.pulsar.common.urls.preprocess

interface UrlFilter : (String) -> String?

abstract class AbstractUrlFilter : UrlFilter {
    override fun invoke(url: String): String? {
        return url
    }
}

class DefaultUrlFilter : AbstractUrlFilter() {
    override fun invoke(url: String): String {
        return url
    }
}
