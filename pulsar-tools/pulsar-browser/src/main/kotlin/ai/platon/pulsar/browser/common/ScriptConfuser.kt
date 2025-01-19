package ai.platon.pulsar.browser.common

interface ScriptConfuser {
    fun confuse(script: String): String
    fun reset()
}
