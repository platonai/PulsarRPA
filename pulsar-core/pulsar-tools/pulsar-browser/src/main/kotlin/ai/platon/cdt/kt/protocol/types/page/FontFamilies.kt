package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Generic font families collection.
 */
@Experimental
public data class FontFamilies(
  @JsonProperty("standard")
  @Optional
  public val standard: String? = null,
  @JsonProperty("fixed")
  @Optional
  public val fixed: String? = null,
  @JsonProperty("serif")
  @Optional
  public val serif: String? = null,
  @JsonProperty("sansSerif")
  @Optional
  public val sansSerif: String? = null,
  @JsonProperty("cursive")
  @Optional
  public val cursive: String? = null,
  @JsonProperty("fantasy")
  @Optional
  public val fantasy: String? = null,
  @JsonProperty("pictograph")
  @Optional
  public val pictograph: String? = null,
)
