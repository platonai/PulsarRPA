package ai.platon.pulsar.persist.data

data class DomStatistics(
    var img: Int = 0,
    var mediumImg: Int = 0,
    var anchor: Int = 0,
    var imgAnchor: Int = 0,
    var anchorImg: Int = 0,
    var numLike: Int = 0
)

data class LabeledHyperLink(
        var label: String,
        var depth: Int,
        var order: Int,
        var anchor: String,
        var url: String
)
