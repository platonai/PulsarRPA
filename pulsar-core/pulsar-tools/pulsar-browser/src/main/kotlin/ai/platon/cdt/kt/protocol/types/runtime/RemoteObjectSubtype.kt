package ai.platon.cdt.kt.protocol.types.runtime

import com.fasterxml.jackson.`annotation`.JsonProperty

/**
 * Object subtype hint. Specified for `object` type values only.
 * NOTE: If you change anything here, make sure to also update
 * `subtype` in `ObjectPreview` and `PropertyPreview` below.
 */
public enum class RemoteObjectSubtype {
  @JsonProperty("array")
  ARRAY,
  @JsonProperty("null")
  NULL,
  @JsonProperty("node")
  NODE,
  @JsonProperty("regexp")
  REGEXP,
  @JsonProperty("date")
  DATE,
  @JsonProperty("map")
  MAP,
  @JsonProperty("set")
  SET,
  @JsonProperty("weakmap")
  WEAKMAP,
  @JsonProperty("weakset")
  WEAKSET,
  @JsonProperty("iterator")
  ITERATOR,
  @JsonProperty("generator")
  GENERATOR,
  @JsonProperty("error")
  ERROR,
  @JsonProperty("proxy")
  PROXY,
  @JsonProperty("promise")
  PROMISE,
  @JsonProperty("typedarray")
  TYPEDARRAY,
  @JsonProperty("arraybuffer")
  ARRAYBUFFER,
  @JsonProperty("dataview")
  DATAVIEW,
  @JsonProperty("webassemblymemory")
  WEBASSEMBLYMEMORY,
  @JsonProperty("wasmvalue")
  WASMVALUE,
}
