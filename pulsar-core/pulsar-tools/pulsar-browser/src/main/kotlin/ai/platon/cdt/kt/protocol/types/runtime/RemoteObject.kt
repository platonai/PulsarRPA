@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String

/**
 * Mirror object referencing original JavaScript object.
 */
data class RemoteObject(
  @param:JsonProperty("type")
  val type: RemoteObjectType,
  @param:JsonProperty("subtype")
  @param:Optional
  val subtype: RemoteObjectSubtype? = null,
  @param:JsonProperty("className")
  @param:Optional
  val className: String? = null,
  @param:JsonProperty("value")
  @param:Optional
  val `value`: Any? = null,
  @param:JsonProperty("unserializableValue")
  @param:Optional
  val unserializableValue: String? = null,
  @param:JsonProperty("description")
  @param:Optional
  val description: String? = null,
  @param:JsonProperty("objectId")
  @param:Optional
  val objectId: String? = null,
  @param:JsonProperty("preview")
  @param:Optional
  @param:Experimental
  val preview: ObjectPreview? = null,
  @param:JsonProperty("customPreview")
  @param:Optional
  @param:Experimental
  val customPreview: CustomPreview? = null,
)
