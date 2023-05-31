package ai.platon.pulsar.persist

open class StorageException(
    message: String,
    cause: Throwable? = null
): RuntimeException(message, cause)

open class WebDBException(
    message: String,
    cause: Throwable? = null
): StorageException(message, cause)

