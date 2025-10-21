package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Object private field descriptor.
 */
@Experimental
public data class PrivatePropertyDescriptor(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  @Optional
  public val `value`: RemoteObject? = null,
  @JsonProperty("get")
  @Optional
  public val `get`: RemoteObject? = null,
  @JsonProperty("set")
  @Optional
  public val `set`: RemoteObject? = null,
)
