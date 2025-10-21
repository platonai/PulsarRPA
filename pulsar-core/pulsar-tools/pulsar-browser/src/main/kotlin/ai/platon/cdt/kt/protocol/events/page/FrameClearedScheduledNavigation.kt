package ai.platon.cdt.kt.protocol.events.page

import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.String

/**
 * Fired when frame no longer has a scheduled navigation.
 */
@Deprecated
public data class FrameClearedScheduledNavigation(
  @JsonProperty("frameId")
  public val frameId: String,
)
