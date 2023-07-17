package ai.platon.pulsar.common.urls

class DegeneratePlainUrl(
    url: String,
    val action: () -> Unit
): PlainUrl(url), CallableDegenerateUrl {
    override fun invoke() = action()
}

class DegenerateHyperlink(
    url: String,
    text: String = "",
    val action: () -> Unit
): Hyperlink(url, text), CallableDegenerateUrl {
    override fun invoke() = action()
}
