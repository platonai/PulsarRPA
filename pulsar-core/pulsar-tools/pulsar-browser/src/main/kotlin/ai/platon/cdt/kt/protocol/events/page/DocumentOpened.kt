package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.page.Frame
import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Fired when opening document to write to.
 */
@Experimental
public data class DocumentOpened(
  @JsonProperty("frame")
  public val frame: Frame,
)
