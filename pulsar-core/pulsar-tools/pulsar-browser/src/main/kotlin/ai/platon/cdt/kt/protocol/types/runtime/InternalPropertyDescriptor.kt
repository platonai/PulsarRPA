package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Object internal property descriptor. This property isn't normally visible in JavaScript code.
 */
public data class InternalPropertyDescriptor(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  @Optional
  public val `value`: RemoteObject? = null,
)
