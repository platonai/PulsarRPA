@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class Navigate(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("loaderId")
  @param:Optional
  val loaderId: String? = null,
  @param:JsonProperty("errorText")
  @param:Optional
  val errorText: String? = null,
)
