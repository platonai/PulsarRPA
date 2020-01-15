package ai.platon.pulsar.common

enum class FlowState {
    CONTINUE, BREAK;

    val isContinue get() = this == CONTINUE
}
