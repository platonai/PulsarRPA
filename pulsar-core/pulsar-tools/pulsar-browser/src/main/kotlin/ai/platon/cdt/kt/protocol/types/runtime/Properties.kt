@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

data class Properties(
  @param:JsonProperty("result")
  val result: List<PropertyDescriptor>,
  @param:JsonProperty("internalProperties")
  @param:Optional
  val internalProperties: List<InternalPropertyDescriptor>? = null,
  @param:JsonProperty("privateProperties")
  @param:Optional
  @param:Experimental
  val privateProperties: List<PrivatePropertyDescriptor>? = null,
  @param:JsonProperty("exceptionDetails")
  @param:Optional
  val exceptionDetails: ExceptionDetails? = null,
)
