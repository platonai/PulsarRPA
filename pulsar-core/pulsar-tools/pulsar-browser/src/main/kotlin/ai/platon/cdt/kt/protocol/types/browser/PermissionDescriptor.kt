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
public data class PermissionDescriptor(
  @JsonProperty("name")
  public val name: String,
  @JsonProperty("sysex")
  @Optional
  public val sysex: Boolean? = null,
  @JsonProperty("userVisibleOnly")
  @Optional
  public val userVisibleOnly: Boolean? = null,
  @JsonProperty("allowWithoutSanitization")
  @Optional
  public val allowWithoutSanitization: Boolean? = null,
  @JsonProperty("panTiltZoom")
  @Optional
  public val panTiltZoom: Boolean? = null,
)
