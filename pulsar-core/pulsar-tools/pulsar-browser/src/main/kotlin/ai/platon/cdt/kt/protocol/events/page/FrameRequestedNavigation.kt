package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.ClientNavigationDisposition
import ai.platon.cdt.kt.protocol.types.page.ClientNavigationReason
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when a renderer-initiated navigation is requested.
 * Navigation may still be cancelled after the event is issued.
 */
@Experimental
public data class FrameRequestedNavigation(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("reason")
  public val reason: ClientNavigationReason,
  @JsonProperty("url")
  public val url: String,
  @JsonProperty("disposition")
  public val disposition: ClientNavigationDisposition,
)
