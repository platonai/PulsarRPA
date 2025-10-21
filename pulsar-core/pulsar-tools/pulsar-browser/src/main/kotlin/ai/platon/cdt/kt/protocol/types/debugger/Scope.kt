package ai.platon.cdt.kt.protocol.types.debugger

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Scope description.
 */
public data class Scope(
  @JsonProperty("type")
  public val type: ScopeType,
  @JsonProperty("object")
  public val `object`: RemoteObject,
  @JsonProperty("name")
  @Optional
  public val name: String? = null,
  @JsonProperty("startLocation")
  @Optional
  public val startLocation: Location? = null,
  @JsonProperty("endLocation")
  @Optional
  public val endLocation: Location? = null,
)
