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
    val instruction get() = find("instruction")

    fun addSystem(content: String, name: String? = null) {
        addLast("system", content, name)
    }

    fun addUser(content: String, name: String? = null) {
        addLast("user", content, name)
    }

    fun addFirst(role: String, content: String, name: String? = null) {
        val msg = SimpleMessage(role, content, name)
        messages.add(0, msg)
    }

    fun addFirst(message: SimpleMessage) {
        val msg = SimpleMessage(message.role, message.content, message.name)
        messages.add(0, msg)
    }

    fun addLast(role: String, content: String, name: String? = null) {
        val msg = SimpleMessage(role, content, name)
        messages.add(msg)
    }

    fun addLast(message: SimpleMessage) {
        val msg = SimpleMessage(message.role, message.content, message.name)
        messages.add(msg)
    }

    fun find(key: String): SimpleMessage? {
        return messages.find { it.name == key }
    }

    fun exists(key: String): Boolean {
        return find(key) != null
    }

    fun systemMessages() = messages.filter { it.role == "system" }

    fun userMessages() = messages.filter { it.role == "user" }

    override fun toString(): String {
        return messages.joinToString("\n")
    }
}
