package ai.platon.cdt.kt.protocol.events.css

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.css.FontFace
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Fires whenever a web font is updated.  A non-empty font parameter indicates a successfully loaded
 * web font
 */
public data class FontsUpdated(
  @JsonProperty("font")
  @Optional
  public val font: FontFace? = null,
)
