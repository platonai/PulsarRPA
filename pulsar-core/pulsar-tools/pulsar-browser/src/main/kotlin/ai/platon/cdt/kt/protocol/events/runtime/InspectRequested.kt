package ai.platon.cdt.kt.protocol.events.runtime

import ai.platon.cdt.kt.protocol.types.runtime.RemoteObject
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Any
import kotlin.String
import kotlin.collections.Map

/**
 * Issued when object should be inspected (for example, as a result of inspect() command line API
 * call).
 */
public data class InspectRequested(
  @JsonProperty("object")
  public val `object`: RemoteObject,
  @JsonProperty("hints")
  public val hints: Map<String, Any?>,
)
