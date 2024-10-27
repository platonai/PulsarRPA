package ai.platon.pulsar.test

import ai.platon.pulsar.ql.context.SQLContext
import ai.platon.pulsar.ql.context.SQLContexts

class VerboseSQLRunner(
    val cx: SQLContext = SQLContexts.create(),
): VerboseSQLExecutor() {
    val loadArgs = "-i 1d -ignF -nJitRetry 3"
}
