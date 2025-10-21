package ai.platon.cdt.kt.protocol.events.target

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.target.TargetInfo
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Issued when attached to target because of auto-attach or `attachToTarget` command.
 */
@Experimental
public data class AttachedToTarget(
  @JsonProperty("sessionId")
  public val sessionId: String,
  @JsonProperty("targetInfo")
  public val targetInfo: TargetInfo,
  @JsonProperty("waitingForDebugger")
  public val waitingForDebugger: Boolean,
)
