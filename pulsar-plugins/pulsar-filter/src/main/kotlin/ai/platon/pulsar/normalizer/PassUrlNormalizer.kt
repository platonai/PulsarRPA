
package ai.platon.pulsar.normalizer

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.filter.AbstractScopedUrlNormalizer
import ai.platon.pulsar.skeleton.crawl.filter.UrlNormalizer

/**
 * This UrlNormalizer doesn't change urls. It is sometimes useful if for a given
 * scope at least one normalizer must be defined but no transformations are
 * required.
 *
 * @author Andrzej Bialecki
 */
class PassUrlNormalizer(conf: ImmutableConfig?) : AbstractScopedUrlNormalizer() {
    
    override fun normalize(url: String, scope: String): String {
        return url
    }

    override fun valid(urlString: String, scope: String): Boolean {
        return true
    }
}
