package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when same-document navigation happens, e.g. due to history API usage or anchor navigation.
 */
@Experimental
public data class NavigatedWithinDocument(
  @JsonProperty("frameId")
  public val frameId: String,
  @JsonProperty("url")
  public val url: String,
)
