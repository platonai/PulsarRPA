package ai.platon.cdt.kt.protocol.types.domsnapshot

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.types.dom.PseudoType
import ai.platon.cdt.kt.protocol.types.dom.ShadowRootType
import ai.platon.cdt.kt.protocol.types.domdebugger.EventListener
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List

/**
 * A Node in the DOM tree.
 */
public data class DOMNode(
  @JsonProperty("nodeType")
  public val nodeType: Int,
  @JsonProperty("nodeName")
  public val nodeName: String,
  @JsonProperty("nodeValue")
  public val nodeValue: String,
  @JsonProperty("textValue")
  @Optional
  public val textValue: String? = null,
  @JsonProperty("inputValue")
  @Optional
  public val inputValue: String? = null,
  @JsonProperty("inputChecked")
  @Optional
  public val inputChecked: Boolean? = null,
  @JsonProperty("optionSelected")
  @Optional
  public val optionSelected: Boolean? = null,
  @JsonProperty("backendNodeId")
  public val backendNodeId: Int,
  @JsonProperty("childNodeIndexes")
  @Optional
  public val childNodeIndexes: List<Int>? = null,
  @JsonProperty("attributes")
  @Optional
  public val attributes: List<NameValue>? = null,
  @JsonProperty("pseudoElementIndexes")
  @Optional
  public val pseudoElementIndexes: List<Int>? = null,
  @JsonProperty("layoutNodeIndex")
  @Optional
  public val layoutNodeIndex: Int? = null,
  @JsonProperty("documentURL")
  @Optional
  public val documentURL: String? = null,
  @JsonProperty("baseURL")
  @Optional
  public val baseURL: String? = null,
  @JsonProperty("contentLanguage")
  @Optional
  public val contentLanguage: String? = null,
  @JsonProperty("documentEncoding")
  @Optional
  public val documentEncoding: String? = null,
  @JsonProperty("publicId")
  @Optional
  public val publicId: String? = null,
  @JsonProperty("systemId")
  @Optional
  public val systemId: String? = null,
  @JsonProperty("frameId")
  @Optional
  public val frameId: String? = null,
  @JsonProperty("contentDocumentIndex")
  @Optional
  public val contentDocumentIndex: Int? = null,
  @JsonProperty("pseudoType")
  @Optional
  public val pseudoType: PseudoType? = null,
  @JsonProperty("shadowRootType")
  @Optional
  public val shadowRootType: ShadowRootType? = null,
  @JsonProperty("isClickable")
  @Optional
  public val isClickable: Boolean? = null,
  @JsonProperty("eventListeners")
  @Optional
  public val eventListeners: List<EventListener>? = null,
  @JsonProperty("currentSourceURL")
  @Optional
  public val currentSourceURL: String? = null,
  @JsonProperty("originURL")
  @Optional
  public val originURL: String? = null,
  @JsonProperty("scrollOffsetX")
  @Optional
  public val scrollOffsetX: Double? = null,
  @JsonProperty("scrollOffsetY")
  @Optional
  public val scrollOffsetY: Double? = null,
)
