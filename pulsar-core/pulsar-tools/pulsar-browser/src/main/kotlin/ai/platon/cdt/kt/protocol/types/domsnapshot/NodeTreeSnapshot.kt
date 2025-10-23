@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domsnapshot

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Int
import kotlin.collections.List

/**
 * Table containing nodes.
 */
data class NodeTreeSnapshot(
  @param:JsonProperty("parentIndex")
  @param:Optional
  val parentIndex: List<Int>? = null,
  @param:JsonProperty("nodeType")
  @param:Optional
  val nodeType: List<Int>? = null,
  @param:JsonProperty("nodeName")
  @param:Optional
  val nodeName: List<Int>? = null,
  @param:JsonProperty("nodeValue")
  @param:Optional
  val nodeValue: List<Int>? = null,
  @param:JsonProperty("backendNodeId")
  @param:Optional
  val backendNodeId: List<Int>? = null,
  @param:JsonProperty("attributes")
  @param:Optional
  val attributes: List<List<Int>>? = null,
  @param:JsonProperty("textValue")
  @param:Optional
  val textValue: RareStringData? = null,
  @param:JsonProperty("inputValue")
  @param:Optional
  val inputValue: RareStringData? = null,
  @param:JsonProperty("inputChecked")
  @param:Optional
  val inputChecked: RareBooleanData? = null,
  @param:JsonProperty("optionSelected")
  @param:Optional
  val optionSelected: RareBooleanData? = null,
  @param:JsonProperty("contentDocumentIndex")
  @param:Optional
  val contentDocumentIndex: RareIntegerData? = null,
  @param:JsonProperty("pseudoType")
  @param:Optional
  val pseudoType: RareStringData? = null,
  @param:JsonProperty("isClickable")
  @param:Optional
  val isClickable: RareBooleanData? = null,
  @param:JsonProperty("currentSourceURL")
  @param:Optional
  val currentSourceURL: RareStringData? = null,
  @param:JsonProperty("originURL")
  @param:Optional
  val originURL: RareStringData? = null,
)
