package ai.platon.pulsar.persist

import ai.platon.pulsar.persist.metadata.CrawlStatusCodes

class CrawlStatus(private val status: Byte) : CrawlStatusCodes {
    companion object {
        @JvmField
        val STATUS_UNFETCHED = CrawlStatus(CrawlStatusCodes.UNFETCHED)
        val STATUS_FETCHED = CrawlStatus(CrawlStatusCodes.FETCHED)
        val STATUS_NOTMODIFIED = CrawlStatus(CrawlStatusCodes.NOTMODIFIED)

        val STATUS_GONE = CrawlStatus(CrawlStatusCodes.GONE)
        val STATUS_REDIR_TEMP = CrawlStatus(CrawlStatusCodes.REDIR_TEMP)
        val STATUS_REDIR_PERM = CrawlStatus(CrawlStatusCodes.REDIR_PERM)
        val STATUS_RETRY = CrawlStatus(CrawlStatusCodes.RETRY)

        private val NAMES: MutableMap<Byte, String> = HashMap()

        init {
            NAMES[CrawlStatusCodes.UNFETCHED] = "status_unfetched"
            NAMES[CrawlStatusCodes.FETCHED] = "status_fetched"
            NAMES[CrawlStatusCodes.RETRY] = "status_retry"
            NAMES[CrawlStatusCodes.GONE] = "status_gone"
            NAMES[CrawlStatusCodes.REDIR_TEMP] = "status_redir_temp"
            NAMES[CrawlStatusCodes.REDIR_PERM] = "status_redir_perm"
            NAMES[CrawlStatusCodes.NOTMODIFIED] = "status_notmodified"
        }
    }

    val code get() = status.toInt()
    val name get() = NAMES[status] ?: "status_unknown"
    val isUnFetched get() = status == CrawlStatusCodes.UNFETCHED
    val isFetched get() = status == CrawlStatusCodes.FETCHED
    val isFailed
        get() = status !in arrayOf(
            CrawlStatusCodes.UNFETCHED,
            CrawlStatusCodes.FETCHED,
            CrawlStatusCodes.NOTMODIFIED
        )
    val isRetry get() = status == CrawlStatusCodes.RETRY
    val isGone get() = status == CrawlStatusCodes.GONE

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return other is CrawlStatus && status.toInt() == other.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    override fun toString(): String {
        return name
    }
}
