package ai.platon.pulsar.rest.api.service

class ScrapeSubmissionException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
