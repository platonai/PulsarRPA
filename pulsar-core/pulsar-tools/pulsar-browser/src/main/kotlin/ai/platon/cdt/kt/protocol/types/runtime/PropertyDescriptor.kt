package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Object property descriptor.
 */
public data class PropertyDescriptor(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("value")
  @Optional
  public val `value`: RemoteObject? = null,
  @JsonProperty("writable")
  @Optional
  public val writable: Boolean? = null,
  @JsonProperty("get")
  @Optional
  public val `get`: RemoteObject? = null,
  @JsonProperty("set")
  @Optional
  public val `set`: RemoteObject? = null,
  @JsonProperty("configurable")
  public val configurable: Boolean,
  @JsonProperty("enumerable")
  public val enumerable: Boolean,
  @JsonProperty("wasThrown")
  @Optional
  public val wasThrown: Boolean? = null,
  @JsonProperty("isOwn")
  @Optional
  public val isOwn: Boolean? = null,
  @JsonProperty("symbol")
  @Optional
  public val symbol: RemoteObject? = null,
)
