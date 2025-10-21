package ai.platon.cdt.kt.protocol.types.profiler

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * Source offset and types for a parameter or return value.
 */
@Experimental
public data class TypeProfileEntry(
  @JsonProperty("offset")
  public val offset: Int,
  @JsonProperty("types")
  public val types: List<TypeObject>,
)
