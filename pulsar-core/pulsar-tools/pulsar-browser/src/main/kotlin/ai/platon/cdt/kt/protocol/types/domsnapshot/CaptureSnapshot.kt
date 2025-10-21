package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

public data class CaptureSnapshot(
  @JsonProperty("documents")
  public val documents: List<DocumentSnapshot>,
  @JsonProperty("strings")
  public val strings: List<String>,
)
