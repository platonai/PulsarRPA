package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Default font sizes.
 */
@Experimental
public data class FontSizes(
  @JsonProperty("standard")
  @Optional
  public val standard: Int? = null,
  @JsonProperty("fixed")
  @Optional
  public val fixed: Int? = null,
)
