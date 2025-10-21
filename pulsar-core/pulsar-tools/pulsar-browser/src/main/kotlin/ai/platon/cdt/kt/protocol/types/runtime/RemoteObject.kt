package ai.platon.cdt.kt.protocol.types.runtime

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String

/**
 * Mirror object referencing original JavaScript object.
 */
public data class RemoteObject(
  @JsonProperty("type")
  public val type: RemoteObjectType,
  @JsonProperty("subtype")
  @Optional
  public val subtype: RemoteObjectSubtype? = null,
  @JsonProperty("className")
  @Optional
  public val className: String? = null,
  @JsonProperty("value")
  @Optional
  public val `value`: Any? = null,
  @JsonProperty("unserializableValue")
  @Optional
  public val unserializableValue: String? = null,
  @JsonProperty("description")
  @Optional
  public val description: String? = null,
  @JsonProperty("objectId")
  @Optional
  public val objectId: String? = null,
  @JsonProperty("preview")
  @Optional
  @Experimental
  public val preview: ObjectPreview? = null,
  @JsonProperty("customPreview")
  @Optional
  @Experimental
  public val customPreview: CustomPreview? = null,
)
