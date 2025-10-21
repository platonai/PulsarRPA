package ai.platon.cdt.kt.protocol.types.layertree

import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.String
import kotlin.collections.List

public data class CompositingReasons(
  @JsonProperty("compositingReasons")
  @Deprecated
  public val compositingReasons: List<String>,
  @JsonProperty("compositingReasonIds")
  public val compositingReasonIds: List<String>,
)
