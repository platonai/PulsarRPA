package ai.platon.cdt.kt.protocol.types.fetch

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

public data class ResponseBody(
  @JsonProperty("body")
  public val body: String,
  @JsonProperty("base64Encoded")
  public val base64Encoded: Boolean,
)
