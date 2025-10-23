@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Fired when same-document navigation happens, e.g. due to history API usage or anchor navigation.
 */
@Experimental
data class NavigatedWithinDocument(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("url")
  val url: String,
)
