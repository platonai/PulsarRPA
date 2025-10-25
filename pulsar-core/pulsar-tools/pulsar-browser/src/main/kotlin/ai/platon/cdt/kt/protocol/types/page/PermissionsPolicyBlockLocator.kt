@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

@Experimental
data class PermissionsPolicyBlockLocator(
  @param:JsonProperty("frameId")
  val frameId: String,
  @param:JsonProperty("blockReason")
  val blockReason: PermissionsPolicyBlockReason,
)
