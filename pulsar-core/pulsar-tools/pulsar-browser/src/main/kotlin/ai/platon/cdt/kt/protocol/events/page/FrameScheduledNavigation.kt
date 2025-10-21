package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.types.page.ClientNavigationReason
import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.Double
import kotlin.String

/**
 * Fired when frame schedules a potential navigation.
 */
@Deprecated
public data class FrameScheduledNavigation(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("delay")
  public val delay: Double,
  @JsonProperty("reason")
  public val reason: ClientNavigationReason,
  @JsonProperty("url")
  public val url: String,
)
