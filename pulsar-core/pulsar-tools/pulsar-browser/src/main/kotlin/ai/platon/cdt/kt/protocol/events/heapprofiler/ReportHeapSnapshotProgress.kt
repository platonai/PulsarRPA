@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.heapprofiler

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Int

data class ReportHeapSnapshotProgress(
  @param:JsonProperty("done")
  val done: Int,
  @param:JsonProperty("total")
  val total: Int,
  @param:JsonProperty("finished")
  @param:Optional
  val finished: Boolean? = null,
)
