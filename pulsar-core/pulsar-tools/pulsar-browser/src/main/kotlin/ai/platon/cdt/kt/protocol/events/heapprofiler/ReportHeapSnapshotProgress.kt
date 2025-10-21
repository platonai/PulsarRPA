package ai.platon.cdt.kt.protocol.events.heapprofiler

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int

public data class ReportHeapSnapshotProgress(
  @JsonProperty("done")
  public val done: Int,
  @JsonProperty("total")
  public val total: Int,
  @JsonProperty("finished")
  @Optional
  public val finished: Boolean? = null,
)
