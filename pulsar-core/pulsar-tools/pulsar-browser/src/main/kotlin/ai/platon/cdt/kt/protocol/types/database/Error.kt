package ai.platon.cdt.kt.protocol.types.database

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Database error.
 */
public data class Error(
  @JsonProperty("message")
  public val message: String,
  @JsonProperty("code")
  public val code: Int,
)
