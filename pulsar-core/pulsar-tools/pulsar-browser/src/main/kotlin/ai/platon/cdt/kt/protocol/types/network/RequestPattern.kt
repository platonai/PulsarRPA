@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.network

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Request pattern for interception.
 */
@Experimental
data class RequestPattern(
  @param:JsonProperty("urlPattern")
  @param:Optional
  val urlPattern: String? = null,
  @param:JsonProperty("resourceType")
  @param:Optional
  val resourceType: ResourceType? = null,
  @param:JsonProperty("interceptionStage")
  @param:Optional
  val interceptionStage: InterceptionStage? = null,
)
