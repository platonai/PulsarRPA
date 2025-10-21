package ai.platon.cdt.kt.protocol.types.domsnapshot

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * Table containing nodes.
 */
public data class NodeTreeSnapshot(
  @JsonProperty("parentIndex")
  @Optional
  public val parentIndex: List<Int>? = null,
  @JsonProperty("nodeType")
  @Optional
  public val nodeType: List<Int>? = null,
  @JsonProperty("nodeName")
  @Optional
  public val nodeName: List<Int>? = null,
  @JsonProperty("nodeValue")
  @Optional
  public val nodeValue: List<Int>? = null,
  @JsonProperty("backendNodeId")
  @Optional
  public val backendNodeId: List<Int>? = null,
  @JsonProperty("attributes")
  @Optional
  public val attributes: List<List<Int>>? = null,
  @JsonProperty("textValue")
  @Optional
  public val textValue: RareStringData? = null,
  @JsonProperty("inputValue")
  @Optional
  public val inputValue: RareStringData? = null,
  @JsonProperty("inputChecked")
  @Optional
  public val inputChecked: RareBooleanData? = null,
  @JsonProperty("optionSelected")
  @Optional
  public val optionSelected: RareBooleanData? = null,
  @JsonProperty("contentDocumentIndex")
  @Optional
  public val contentDocumentIndex: RareIntegerData? = null,
  @JsonProperty("pseudoType")
  @Optional
  public val pseudoType: RareStringData? = null,
  @JsonProperty("isClickable")
  @Optional
  public val isClickable: RareBooleanData? = null,
  @JsonProperty("currentSourceURL")
  @Optional
  public val currentSourceURL: RareStringData? = null,
  @JsonProperty("originURL")
  @Optional
  public val originURL: RareStringData? = null,
)
