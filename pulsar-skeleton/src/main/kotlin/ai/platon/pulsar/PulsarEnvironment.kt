package ai.platon.pulsar

class PulsarEnvironment {
    fun initialize() {
        System.setProperty("tika.config", "tika-config.xml")
    }
}
