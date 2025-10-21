package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

/**
 * Fired when the page with currently enabled screencast was shown or hidden `.
 */
@Experimental
public data class ScreencastVisibilityChanged(
  @JsonProperty("visible")
  public val visible: Boolean,
)
