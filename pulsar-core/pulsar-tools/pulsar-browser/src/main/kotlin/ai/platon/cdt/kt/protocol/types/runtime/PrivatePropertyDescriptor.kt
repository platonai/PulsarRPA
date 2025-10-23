@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.String

/**
 * Object private field descriptor.
 */
@Experimental
data class PrivatePropertyDescriptor(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: RemoteObject? = null,
  @param:JsonProperty("get")
  @param:Optional
  val `get`: RemoteObject? = null,
  @param:JsonProperty("set")
  @param:Optional
  val `set`: RemoteObject? = null,
)
