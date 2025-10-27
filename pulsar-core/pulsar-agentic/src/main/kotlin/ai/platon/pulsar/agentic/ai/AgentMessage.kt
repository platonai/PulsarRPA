package ai.platon.pulsar.agentic.ai

data class SimpleMessage(
    val role: String,
    val content: String,
    val name: String? = null,
) {
    enum class Role {
        USER, SYSTEM
    }

    override fun toString() = content
}

class AgentMessageList(
    val messages: MutableList<SimpleMessage> = mutableListOf()
) {
    fun addSystem(content: String, key: String? = null) {
        add("system", content, key)
    }

    fun addUser(content: String, key: String? = null) {
        add("user", content, key)
    }

    fun add(role: String, content: String, key: String? = null) {
        val msg = SimpleMessage(role, content, key)
        messages.add(msg)
    }

    fun add(message: SimpleMessage) {
        val msg = SimpleMessage(message.role, message.content, message.name)
        messages.add(msg)
    }

    fun find(key: String): SimpleMessage? {
        return messages.find { it.name == key }
    }

    fun systemMessages() = messages.filter { it.role == "system" }

    fun userMessages() = messages.filter { it.role == "user" }
}
