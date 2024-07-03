package ai.platon.pulsar.skeleton.crawl.index

import java.lang.Exception

/**
 * Created by vincent on 16-8-1.
 */
class IndexingException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
