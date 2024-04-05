package ai.platon.pulsar.common.extractor

interface TextBlockFilter {
    fun process(document: TextDocument): Boolean
}
