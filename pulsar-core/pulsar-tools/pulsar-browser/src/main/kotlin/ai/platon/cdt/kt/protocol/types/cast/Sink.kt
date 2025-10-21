package ai.platon.cdt.kt.protocol.types.cast

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

public data class Sink(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("id")
  public val id: String,
  @JsonProperty("session")
  @Optional
  public val session: String? = null,
)
