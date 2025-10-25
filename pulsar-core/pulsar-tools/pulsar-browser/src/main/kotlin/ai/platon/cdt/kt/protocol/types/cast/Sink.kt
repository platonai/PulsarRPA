@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.cast

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class Sink(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("id")
  val id: String,
  @param:JsonProperty("session")
  @param:Optional
  val session: String? = null,
)
