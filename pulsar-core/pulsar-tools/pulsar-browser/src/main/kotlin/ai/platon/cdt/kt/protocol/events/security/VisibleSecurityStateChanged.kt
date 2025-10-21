package ai.platon.cdt.kt.protocol.events.security

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.security.VisibleSecurityState
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * The security state of the page changed.
 */
@Experimental
public data class VisibleSecurityStateChanged(
  @JsonProperty("visibleSecurityState")
  public val visibleSecurityState: VisibleSecurityState,
)
