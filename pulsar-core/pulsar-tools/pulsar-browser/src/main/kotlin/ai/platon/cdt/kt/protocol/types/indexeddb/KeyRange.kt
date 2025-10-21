package ai.platon.cdt.kt.protocol.types.indexeddb

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

/**
 * Key range.
 */
public data class KeyRange(
  @JsonProperty("lower")
  @Optional
  public val lower: Key? = null,
  @JsonProperty("upper")
  @Optional
  public val upper: Key? = null,
  @JsonProperty("lowerOpen")
  public val lowerOpen: Boolean,
  @JsonProperty("upperOpen")
  public val upperOpen: Boolean,
)
