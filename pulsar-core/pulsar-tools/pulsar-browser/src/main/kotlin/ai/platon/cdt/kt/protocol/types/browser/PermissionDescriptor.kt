@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.browser

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.String

/**
 * Definition of PermissionDescriptor defined in the Permissions API:
 * https://w3c.github.io/permissions/#dictdef-permissiondescriptor.
 */
@Experimental
data class PermissionDescriptor(
  @param:JsonProperty("name")
  val name: String,
  @param:JsonProperty("sysex")
  @param:Optional
  val sysex: Boolean? = null,
  @param:JsonProperty("userVisibleOnly")
  @param:Optional
  val userVisibleOnly: Boolean? = null,
  @param:JsonProperty("allowWithoutSanitization")
  @param:Optional
  val allowWithoutSanitization: Boolean? = null,
  @param:JsonProperty("panTiltZoom")
  @param:Optional
  val panTiltZoom: Boolean? = null,
)
