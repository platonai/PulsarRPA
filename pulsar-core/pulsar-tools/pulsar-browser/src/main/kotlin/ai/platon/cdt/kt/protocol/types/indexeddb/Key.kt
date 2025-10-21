package ai.platon.cdt.kt.protocol.types.indexeddb

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.String
import kotlin.collections.List

/**
 * Key.
 */
public data class Key(
  @JsonProperty("type")
  public val type: KeyType,
  @JsonProperty("number")
  @Optional
  public val number: Double? = null,
  @JsonProperty("string")
  @Optional
  public val string: String? = null,
  @JsonProperty("date")
  @Optional
  public val date: Double? = null,
  @JsonProperty("array")
  @Optional
  public val array: List<Key>? = null,
)
