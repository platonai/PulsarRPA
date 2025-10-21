package ai.platon.cdt.kt.protocol.types.indexeddb

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String
import kotlin.collections.List

/**
 * Key path.
 */
public data class KeyPath(
  @JsonProperty("type")
  public val type: KeyPathType,
  @JsonProperty("string")
  @Optional
  public val string: String? = null,
  @JsonProperty("array")
  @Optional
  public val array: List<String>? = null,
)
