package ai.platon.cdt.kt.protocol.types.audits

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

public data class EncodedResponse(
  @JsonProperty("body")
  @Optional
  public val body: String? = null,
  @JsonProperty("originalSize")
  public val originalSize: Int,
  @JsonProperty("encodedSize")
  public val encodedSize: Int,
)
