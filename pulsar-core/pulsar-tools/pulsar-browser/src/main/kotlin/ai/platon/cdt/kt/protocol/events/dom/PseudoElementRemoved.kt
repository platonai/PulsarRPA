package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Called when a pseudo element is removed from an element.
 */
@Experimental
public data class PseudoElementRemoved(
  @JsonProperty("parentId")
  public val parentId: Int,
  @JsonProperty("pseudoElementId")
  public val pseudoElementId: Int,
)
