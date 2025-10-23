@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Object property descriptor.
 */
data class PropertyDescriptor(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: RemoteObject? = null,
  @param:JsonProperty("writable")
  @param:Optional
  val writable: Boolean? = null,
  @param:JsonProperty("get")
  @param:Optional
  val `get`: RemoteObject? = null,
  @param:JsonProperty("set")
  @param:Optional
  val `set`: RemoteObject? = null,
  @param:JsonProperty("configurable")
  val configurable: Boolean,
  @param:JsonProperty("enumerable")
  val enumerable: Boolean,
  @param:JsonProperty("wasThrown")
  @param:Optional
  val wasThrown: Boolean? = null,
  @param:JsonProperty("isOwn")
  @param:Optional
  val isOwn: Boolean? = null,
  @param:JsonProperty("symbol")
  @param:Optional
  val symbol: RemoteObject? = null,
)
