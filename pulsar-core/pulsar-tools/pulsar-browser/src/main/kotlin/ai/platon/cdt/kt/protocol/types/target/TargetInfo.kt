@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.target

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

data class TargetInfo(
  @param:JsonProperty("targetId")
  val targetId: String,
  @param:JsonProperty("type")
  val type: String,
  @param:JsonProperty("title")
  val title: String,
  @param:JsonProperty("url")
  val url: String,
  @param:JsonProperty("attached")
  val attached: Boolean,
  @param:JsonProperty("openerId")
  @param:Optional
  val openerId: String? = null,
  @param:JsonProperty("canAccessOpener")
  @param:Experimental
  val canAccessOpener: Boolean,
  @param:JsonProperty("openerFrameId")
  @param:Optional
  @param:Experimental
  val openerFrameId: String? = null,
  @param:JsonProperty("browserContextId")
  @param:Optional
  @param:Experimental
  val browserContextId: String? = null,
)
