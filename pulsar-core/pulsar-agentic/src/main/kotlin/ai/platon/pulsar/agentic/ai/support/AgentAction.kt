package ai.platon.pulsar.agentic.ai.support

interface AgentAction {
    val declaration : String

    val domain: String
    val method: String
    val arguments: List<Pair<String, String>>

    fun execute(): Any?

    fun onWillExecute()
    fun onDidExecute()


    fun requireSelector(): Boolean

    fun validate(): Boolean
}
