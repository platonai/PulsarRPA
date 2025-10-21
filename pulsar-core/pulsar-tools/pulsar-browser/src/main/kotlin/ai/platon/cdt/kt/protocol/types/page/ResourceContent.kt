package ai.platon.cdt.kt.protocol.types.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

public data class ResourceContent(
  @JsonProperty("content")
  public val content: String,
  @JsonProperty("base64Encoded")
  public val base64Encoded: Boolean,
)
