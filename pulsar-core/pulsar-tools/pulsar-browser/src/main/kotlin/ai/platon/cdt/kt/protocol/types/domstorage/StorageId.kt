package ai.platon.cdt.kt.protocol.types.domstorage

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * DOM Storage identifier.
 */
public data class StorageId(
  @JsonProperty("securityOrigin")
  public val securityOrigin: String,
  @JsonProperty("isLocalStorage")
  public val isLocalStorage: Boolean,
)
