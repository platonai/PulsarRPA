@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

@Experimental
data class PermissionsPolicyFeatureState(
  @param:JsonProperty("feature")
  val feature: PermissionsPolicyFeature,
  @param:JsonProperty("allowed")
  val allowed: Boolean,
  @param:JsonProperty("locator")
  @param:Optional
  val locator: PermissionsPolicyBlockLocator? = null,
)
