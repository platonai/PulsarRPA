package ai.platon.pulsar.common.options

import ai.platon.pulsar.common.url.UrlAware

object LoadOptionsNormalizer {

    fun normalize(options: LoadOptions, url: UrlAware): LoadOptions {
        val args = url.args
        val actualOptions = options.clone()
        if (args != null) {
            actualOptions.mergeModified(args)
        }

        if (url.label.isNotBlank()) {
            actualOptions.label = url.label
        }

        return actualOptions
    }
}
