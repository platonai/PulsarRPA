@file:Suppress("unused")
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
data class DOMNode(
  @param:JsonProperty("nodeType")
  val nodeType: Int,
  @param:JsonProperty("nodeName")
  val nodeName: String,
  @param:JsonProperty("nodeValue")
  val nodeValue: String,
  @param:JsonProperty("textValue")
  @param:Optional
  val textValue: String? = null,
  @param:JsonProperty("inputValue")
  @param:Optional
  val inputValue: String? = null,
  @param:JsonProperty("inputChecked")
  @param:Optional
  val inputChecked: Boolean? = null,
  @param:JsonProperty("optionSelected")
  @param:Optional
  val optionSelected: Boolean? = null,
  @param:JsonProperty("backendNodeId")
  val backendNodeId: Int,
  @param:JsonProperty("childNodeIndexes")
  @param:Optional
  val childNodeIndexes: List<Int>? = null,
  @param:JsonProperty("attributes")
  @param:Optional
  val attributes: List<NameValue>? = null,
  @param:JsonProperty("pseudoElementIndexes")
  @param:Optional
  val pseudoElementIndexes: List<Int>? = null,
  @param:JsonProperty("layoutNodeIndex")
  @param:Optional
  val layoutNodeIndex: Int? = null,
  @param:JsonProperty("documentURL")
  @param:Optional
  val documentURL: String? = null,
  @param:JsonProperty("baseURL")
  @param:Optional
  val baseURL: String? = null,
  @param:JsonProperty("contentLanguage")
  @param:Optional
  val contentLanguage: String? = null,
  @param:JsonProperty("documentEncoding")
  @param:Optional
  val documentEncoding: String? = null,
  @param:JsonProperty("publicId")
  @param:Optional
  val publicId: String? = null,
  @param:JsonProperty("systemId")
  @param:Optional
  val systemId: String? = null,
  @param:JsonProperty("frameId")
  @param:Optional
  val frameId: String? = null,
  @param:JsonProperty("contentDocumentIndex")
  @param:Optional
  val contentDocumentIndex: Int? = null,
  @param:JsonProperty("pseudoType")
  @param:Optional
  val pseudoType: PseudoType? = null,
  @param:JsonProperty("shadowRootType")
  @param:Optional
  val shadowRootType: ShadowRootType? = null,
  @param:JsonProperty("isClickable")
  @param:Optional
  val isClickable: Boolean? = null,
  @param:JsonProperty("eventListeners")
  @param:Optional
  val eventListeners: List<EventListener>? = null,
  @param:JsonProperty("currentSourceURL")
  @param:Optional
  val currentSourceURL: String? = null,
  @param:JsonProperty("originURL")
  @param:Optional
  val originURL: String? = null,
  @param:JsonProperty("scrollOffsetX")
  @param:Optional
  val scrollOffsetX: Double? = null,
  @param:JsonProperty("scrollOffsetY")
  @param:Optional
  val scrollOffsetY: Double? = null,
)
