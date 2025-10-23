@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String

/**
 * Represents function call argument. Either remote object id `objectId`, primitive `value`,
 * unserializable primitive value or neither of (for undefined) them should be specified.
 */
data class CallArgument(
  @param:JsonProperty("value")
  @param:Optional
  val `value`: Any? = null,
  @param:JsonProperty("unserializableValue")
  @param:Optional
  val unserializableValue: String? = null,
  @param:JsonProperty("objectId")
  @param:Optional
  val objectId: String? = null,
)
