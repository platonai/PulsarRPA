@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.tracing

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Data format of a trace. Can be either the legacy JSON format or the
 * protocol buffer format. Note that the JSON format will be deprecated soon.
 */
public enum class StreamFormat {
  @JsonProperty("json")
  JSON,
  @JsonProperty("proto")
  PROTO,
}
