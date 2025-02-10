package ai.platon.pulsar.skeleton.crawl.fetch.driver

class CommandDispatcher {
    /**
     * Use kotlin parser https://github.com/Kotlin/grammar-tools
     * */
    fun dispatch(command: String): Any? {
        return null
    }
}

fun main() {
    val commandDispatcher = CommandDispatcher()
    commandDispatcher.dispatch("1+1")
}
