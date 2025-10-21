package ai.platon.cdt.kt.protocol.types.io

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

public data class Read(
  @JsonProperty("base64Encoded")
  @Optional
  public val base64Encoded: Boolean? = null,
  @JsonProperty("data")
  public val `data`: String,
  @JsonProperty("eof")
  public val eof: Boolean,
)
