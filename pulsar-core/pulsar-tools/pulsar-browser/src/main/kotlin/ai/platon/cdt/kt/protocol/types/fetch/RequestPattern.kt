@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.fetch

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.network.ResourceType
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

data class RequestPattern(
  @param:JsonProperty("urlPattern")
  @param:Optional
  val urlPattern: String? = null,
  @param:JsonProperty("resourceType")
  @param:Optional
  val resourceType: ResourceType? = null,
  @param:JsonProperty("requestStage")
  @param:Optional
  val requestStage: RequestStage? = null,
)
