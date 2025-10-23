@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.events.tethering

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

/**
 * Informs that port was successfully bound and got a specified connection id.
 */
data class Accepted(
  @param:JsonProperty("port")
  val port: Int,
  @param:JsonProperty("connectionId")
  val connectionId: String,
)
