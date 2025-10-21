package ai.platon.cdt.kt.protocol.events.dom

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.types.dom.Node
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int

/**
 * Called when shadow root is pushed into the element.
 */
@Experimental
public data class ShadowRootPushed(
  @JsonProperty("hostId")
  public val hostId: Int,
  @JsonProperty("root")
  public val root: Node,
)
