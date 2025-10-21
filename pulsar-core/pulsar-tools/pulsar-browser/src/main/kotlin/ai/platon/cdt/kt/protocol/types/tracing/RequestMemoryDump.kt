package ai.platon.cdt.kt.protocol.types.tracing

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

public data class RequestMemoryDump(
  @JsonProperty("dumpGuid")
  public val dumpGuid: String,
  @JsonProperty("success")
  public val success: Boolean,
)
