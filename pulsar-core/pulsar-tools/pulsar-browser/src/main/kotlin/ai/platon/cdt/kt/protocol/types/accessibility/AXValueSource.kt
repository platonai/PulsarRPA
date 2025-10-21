package ai.platon.cdt.kt.protocol.types.accessibility

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * A single source for a computed AX property.
 */
public data class AXValueSource(
  @JsonProperty("type")
  public val type: AXValueSourceType,
  @JsonProperty("value")
  @Optional
  public val `value`: AXValue? = null,
  @JsonProperty("attribute")
  @Optional
  public val attribute: String? = null,
  @JsonProperty("attributeValue")
  @Optional
  public val attributeValue: AXValue? = null,
  @JsonProperty("superseded")
  @Optional
  public val superseded: Boolean? = null,
  @JsonProperty("nativeSource")
  @Optional
  public val nativeSource: AXValueNativeSourceType? = null,
  @JsonProperty("nativeSourceValue")
  @Optional
  public val nativeSourceValue: AXValue? = null,
  @JsonProperty("invalid")
  @Optional
  public val invalid: Boolean? = null,
  @JsonProperty("invalidReason")
  @Optional
  public val invalidReason: String? = null,
)
