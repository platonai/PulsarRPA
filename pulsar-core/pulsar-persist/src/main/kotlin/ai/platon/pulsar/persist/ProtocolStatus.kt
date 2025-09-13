package ai.platon.pulsar.persist

import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.persist.gora.generated.GProtocolStatus
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes.INCOMPATIBLE_CODE_START
import java.util.*

class ProtocolStatus {
    private val protocolStatus: GProtocolStatus

    constructor(majorCode: Short) {
        this.protocolStatus = GProtocolStatus.newBuilder().build()
        this.majorCode = majorCode
        minorCode = -1
    }

    constructor(majorCode: Short, minorCode: Int) {
        this.protocolStatus = GProtocolStatus.newBuilder().build()
        this.majorCode = majorCode
        this.minorCode = minorCode
    }

    private constructor(protocolStatus: GProtocolStatus) {
        Objects.requireNonNull(protocolStatus)
        this.protocolStatus = protocolStatus
    }

    fun unbox(): GProtocolStatus {
        return protocolStatus
    }

    val isNotFetched: Boolean
        get() = majorCode == NOTFETCHED

    val isSuccess: Boolean
        get() = majorCode == SUCCESS

    val isFailed: Boolean
        get() = majorCode == FAILED

    val isCanceled: Boolean
        /**
         * If a fetch task is canceled, the page status will not be changed
         */
        get() = minorCode == ProtocolStatusCodes.CANCELED

    val isNotFound: Boolean
        /**
         * the page displays "404 Not Found" or something similar,
         * the server should issue a 404 error code, but not guaranteed
         */
        get() = minorCode == ProtocolStatusCodes.SC_NOT_FOUND

    val isGone: Boolean
        get() = minorCode == ProtocolStatusCodes.GONE

    val isRetry: Boolean
        get() = minorCode == ProtocolStatusCodes.RETRY

    fun isRetry(scope: RetryScope): Boolean {
        val defaultScope = RetryScope.CRAWL
        return minorCode == ProtocolStatusCodes.RETRY && getArgOrElse(
            ARG_RETRY_SCOPE,
            defaultScope.toString()
        ) == scope.toString()
    }

    fun isRetry(scope: RetryScope, reason: Any): Boolean {
        val reasonString = if (reason is Exception) {
            reason.javaClass.simpleName
        } else if (reason is Class<*>) {
            reason.simpleName
        } else {
            reason.toString()
        }

        if (!isRetry(scope)) {
            return false
        }

        if (getArgOrElse(ARG_REASON, "") == reasonString) {
            return true
        }

        return false
    }

    val isTempMoved: Boolean
        get() = minorCode == ProtocolStatusCodes.MOVED_TEMPORARILY

    val isMoved: Boolean
        get() = minorCode == ProtocolStatusCodes.MOVED_TEMPORARILY || minorCode == ProtocolStatusCodes.MOVED_PERMANENTLY

    val isTimeout: Boolean
        get() = isTimeout(this)

    val majorName: String
        get() = getMajorName(majorCode.toInt())

    var majorCode: Short
        get() = protocolStatus.majorCode.toShort()
        set(majorCode) {
            protocolStatus.majorCode = majorCode.toInt()
        }

    val minorName: String
        get() = getMinorName(minorCode)

    var minorCode: Int
        get() = protocolStatus.minorCode
        set(minorCode) {
            protocolStatus.minorCode = minorCode
        }

    fun setMinorCode(minorCode: Int, message: String) {
        this.minorCode = minorCode
        args[minorName] = message
    }

    fun getArgOrElse(name: String, defaultValue: String): String {
        return args.getOrDefault(name, defaultValue).toString()
    }

    val args: MutableMap<CharSequence, CharSequence>
        get() = protocolStatus.args

    fun setArgs(args: Map<CharSequence?, CharSequence?>?) {
        protocolStatus.args = args
    }

    val name: String
        get() = (majorCodes.getOrDefault(majorCode, "unknown") + "/"
                + minorCodes.getOrDefault(minorCode, "unknown"))

    val retryScope: Any?
        get() = args[ARG_RETRY_SCOPE]

    val reason: Any?
        get() = args[ARG_REASON]

    fun upgradeRetry(scope: RetryScope) {
        args[ARG_RETRY_SCOPE] = scope.toString()
    }

    override fun toString(): String {
        val minorName = minorCodes.getOrDefault(minorCode, "Unknown")
        var str = "$minorName($minorCode)"
        if (args.isNotEmpty()) {
            val keys = listOf(ARG_RETRY_SCOPE, ARG_REASON, ARG_HTTP_CODE)
            val args = args.entries
                .filter { keys.contains(it.key.toString()) }
                .joinToString(", ") { it.key.toString() + ": " + it.value }
            str += " $args"
        }
        return str
    }

    companion object {
        const val ARG_HTTP_CODE: String = "httpCode"
        const val ARG_REDIRECT_TO_URL: String = "redirectTo"
        const val ARG_URL: String = "url"
        const val ARG_RETRY_SCOPE: String = "rsp"
        const val ARG_REASON: String = "rs"

        /**
         * Content was not retrieved yet.
         */
        private const val NOTFETCHED: Short = 0

        /**
         * Content was retrieved without errors.
         */
        private const val SUCCESS: Short = 1

        /**
         * Content was not retrieved. Any further errors may be indicated in args.
         */
        private const val FAILED: Short = 2

        val STATUS_SUCCESS: ProtocolStatus = ProtocolStatus(SUCCESS, ProtocolStatusCodes.SC_OK)
        val STATUS_NOTMODIFIED: ProtocolStatus = ProtocolStatus(SUCCESS, ProtocolStatusCodes.NOT_MODIFIED)
        val STATUS_NOTFETCHED: ProtocolStatus = ProtocolStatus(NOTFETCHED)

        val STATUS_PROTO_NOT_FOUND: ProtocolStatus = failed(ProtocolStatusCodes.PROTO_NOT_FOUND)
        val STATUS_ACCESS_DENIED: ProtocolStatus = failed(ProtocolStatusCodes.UNAUTHORIZED)
        val STATUS_NOTFOUND: ProtocolStatus = failed(ProtocolStatusCodes.SC_NOT_FOUND)

        // NOTE:
        // What are the differences between a canceled page and a retry page?
        // If a task is canceled, nothing will be saved, while if a task is retry, all the metadata should be saved.
        val STATUS_CANCELED: ProtocolStatus = failed(ProtocolStatusCodes.CANCELED)
        val STATUS_EXCEPTION: ProtocolStatus = failed(ProtocolStatusCodes.EXCEPTION)

        private val majorCodes = HashMap<Short, String>()
        private val minorCodes = HashMap<Int, String>()

        init {
            majorCodes[NOTFETCHED] = "NotFetched"
            majorCodes[SUCCESS] = "Success"
            majorCodes[FAILED] = "Failed"

            minorCodes[ProtocolStatusCodes.SC_OK] = "OK"
            minorCodes[ProtocolStatusCodes.CREATED] = "Created"
            minorCodes[ProtocolStatusCodes.MOVED_PERMANENTLY] = "Moved"
            minorCodes[ProtocolStatusCodes.MOVED_TEMPORARILY] = "TempMoved"
            minorCodes[ProtocolStatusCodes.NOT_MODIFIED] = "NotModified"

            minorCodes[ProtocolStatusCodes.PROTO_NOT_FOUND] = "ProtoNotFound"
            minorCodes[ProtocolStatusCodes.UNAUTHORIZED] = "AccessDenied"
            minorCodes[ProtocolStatusCodes.SC_NOT_FOUND] = "NotFound"
            minorCodes[ProtocolStatusCodes.PRECONDITION_FAILED] = "PreconditionFailed"
            minorCodes[ProtocolStatusCodes.REQUEST_TIMEOUT] = "RequestTimeout"
            minorCodes[ProtocolStatusCodes.GONE] = "Gone"

            minorCodes[ProtocolStatusCodes.UNKNOWN_HOST] = "UnknownHost"
            minorCodes[ProtocolStatusCodes.ROBOTS_DENIED] = "RobotsDenied"
            minorCodes[ProtocolStatusCodes.EXCEPTION] = "Exception"
            minorCodes[ProtocolStatusCodes.REDIR_EXCEEDED] = "RedirExceeded"
            minorCodes[ProtocolStatusCodes.WOULD_BLOCK] = "WouldBlock"
            minorCodes[ProtocolStatusCodes.BLOCKED] = "Blocked"

            minorCodes[ProtocolStatusCodes.RETRY] = "Retry"
            minorCodes[ProtocolStatusCodes.CANCELED] = "Canceled"
            minorCodes[ProtocolStatusCodes.THREAD_TIMEOUT] = "ThreadTimeout"
            minorCodes[ProtocolStatusCodes.WEB_DRIVER_TIMEOUT] = "WebDriverTimeout"
            minorCodes[ProtocolStatusCodes.SCRIPT_TIMEOUT] = "ScriptTimeout"
        }

        fun box(protocolStatus: GProtocolStatus): ProtocolStatus {
            return ProtocolStatus(protocolStatus)
        }

        fun getMajorName(code: Int): String {
            return majorCodes.getOrDefault(code.toShort(), "unknown")
        }

        @Deprecated("Use getStatusText instead for consistency with ResourceStatus", ReplaceWith(expression = "getStatusText"))
        fun getMinorName(code: Int): String {
            return minorCodes.getOrDefault(code, "unknown")
        }

        /**
         * Keep consistency with [ai.platon.pulsar.common.ResourceStatus]
         * */
        fun getStatusText(code: Int): String {
            return when {
                code < INCOMPATIBLE_CODE_START -> ResourceStatus.getStatusText(code)
                else -> minorCodes.getOrDefault(code, "unknown")
            }
        }

        fun retry(scope: RetryScope?, reason: Any): ProtocolStatus {
            val reasonString = if (reason is Exception) {
                reason.javaClass.simpleName
            } else {
                reason.toString()
            }

            return failed(
                ProtocolStatusCodes.RETRY,
                ARG_RETRY_SCOPE, scope,
                ARG_REASON, reasonString
            )
        }

        
        fun cancel(reason: Any?): ProtocolStatus {
            return failed(
                ProtocolStatusCodes.CANCELED,
                ARG_REASON, reason
            )
        }

        
        fun failed(minorCode: Int): ProtocolStatus {
            return ProtocolStatus(FAILED, minorCode)
        }

        
        fun failed(minorCode: Int, vararg args: Any?): ProtocolStatus {
            val protocolStatus = ProtocolStatus(FAILED, minorCode)

            if (args.size % 2 == 0) {
                val protocolStatusArgs = protocolStatus.args
                var i = 0
                while (i < args.size - 1) {
                    if (args[i] != null && args[i + 1] != null) {
                        protocolStatusArgs[args[i].toString()] = args[i + 1].toString()
                    }
                    i += 2
                }
            }

            return protocolStatus
        }

        fun failed(e: Throwable?): ProtocolStatus {
            return failed(ProtocolStatusCodes.EXCEPTION, "error", e?.message)
        }

        fun fromMinor(minorCode: Int): ProtocolStatus {
            return if (minorCode == ProtocolStatusCodes.SC_OK || minorCode == ProtocolStatusCodes.NOT_MODIFIED) {
                STATUS_SUCCESS
            } else {
                failed(minorCode)
            }
        }

        fun isTimeout(protocolStatus: ProtocolStatus): Boolean {
            val code = protocolStatus.minorCode
            return isTimeout(code)
        }

        fun isTimeout(code: Int): Boolean {
            return code == ProtocolStatusCodes.REQUEST_TIMEOUT || code == ProtocolStatusCodes.THREAD_TIMEOUT || code == ProtocolStatusCodes.WEB_DRIVER_TIMEOUT || code == ProtocolStatusCodes.SCRIPT_TIMEOUT
        }
    }
}
