@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.target

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.String

@Experimental
data class RemoteLocation(
  @param:JsonProperty("host")
  val host: String,
  @param:JsonProperty("port")
  val port: Int,
)
