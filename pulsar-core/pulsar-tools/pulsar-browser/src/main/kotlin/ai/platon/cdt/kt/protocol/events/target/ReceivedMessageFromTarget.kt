package ai.platon.cdt.kt.protocol.events.target

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.String

/**
 * Notifies about a new protocol message received from the session (as reported in
 * `attachedToTarget` event).
 */
public data class ReceivedMessageFromTarget(
  @JsonProperty("sessionId")
  public val sessionId: String,
  @JsonProperty("message")
  public val message: String,
  @JsonProperty("targetId")
  @Optional
  @Deprecated
  public val targetId: String? = null,
)
