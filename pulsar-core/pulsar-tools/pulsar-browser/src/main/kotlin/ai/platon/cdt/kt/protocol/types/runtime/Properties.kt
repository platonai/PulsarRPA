package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

public data class Properties(
  @JsonProperty("result")
  public val result: List<PropertyDescriptor>,
  @JsonProperty("internalProperties")
  @Optional
  public val internalProperties: List<InternalPropertyDescriptor>? = null,
  @JsonProperty("privateProperties")
  @Optional
  @Experimental
  public val privateProperties: List<PrivatePropertyDescriptor>? = null,
  @JsonProperty("exceptionDetails")
  @Optional
  public val exceptionDetails: ExceptionDetails? = null,
)
