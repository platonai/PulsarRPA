package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Post data entry for HTTP request
 */
public data class PostDataEntry(
  @JsonProperty("bytes")
  @Optional
  public val bytes: String? = null,
)
