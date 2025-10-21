package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean

@Experimental
public data class PermissionsPolicyFeatureState(
  @JsonProperty("feature")
  public val feature: PermissionsPolicyFeature,
  @JsonProperty("allowed")
  public val allowed: Boolean,
  @JsonProperty("locator")
  @Optional
  public val locator: PermissionsPolicyBlockLocator? = null,
)
