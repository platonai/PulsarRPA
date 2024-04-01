package ai.platon.pulsar.persist.experimental

import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.persist.KWebAsset

val KWebAsset.isNil: Boolean get() = this is NilWebAsset
val KWebAsset.isNotNil: Boolean get() = !isNil

/**
 * The reversed url of the web page, it's also the key of the underlying storage of this object
 */
val KWebAsset.reversedUrl: String get() = UrlUtils.reverseUrl(url)
