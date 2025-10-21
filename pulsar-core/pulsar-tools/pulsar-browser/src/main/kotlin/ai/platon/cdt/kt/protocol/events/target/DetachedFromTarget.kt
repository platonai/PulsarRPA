package ai.platon.cdt.kt.protocol.events.target

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated
import kotlin.String

/**
 * Issued when detached from target for any reason (including `detachFromTarget` command). Can be
 * issued multiple times per target if multiple sessions have been attached to it.
 */
@Experimental
public data class DetachedFromTarget(
  @JsonProperty("sessionId")
  public val sessionId: String,
  @JsonProperty("targetId")
  @Optional
  @Deprecated
  public val targetId: String? = null,
)
