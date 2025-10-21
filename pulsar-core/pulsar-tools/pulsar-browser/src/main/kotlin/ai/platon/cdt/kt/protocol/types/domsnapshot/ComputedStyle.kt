package ai.platon.cdt.kt.protocol.types.domsnapshot

import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.collections.List

/**
 * A subset of the full ComputedStyle as defined by the request whitelist.
 */
public data class ComputedStyle(
  @JsonProperty("properties")
  public val properties: List<NameValue>,
)
