@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Notification is issued every time when binding is called.
 */
@Experimental
data class BindingCalled(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("payload")
  val payload: String,
  @param:JsonProperty("executionContextId")
  val executionContextId: Int,
)
