package ai.platon.pulsar.persist

import java.time.Instant

data class FetchTimeInfo(
    var prevFetchTime: Instant,
    var prevModifiedTime: Instant,
    var fetchTime: Instant,
    var modifiedTime: Instant,
    var state: Int,
)
