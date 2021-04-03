package ai.platon.pulsar.common

data class OpenResourceStatus(
    val code: Int = 0,
    val message: String = "",
    val scope: String = "unspecified"
) {
    val isOK get() = code == 0 || code == 200
    val isNotOK get() = code != 0
}
