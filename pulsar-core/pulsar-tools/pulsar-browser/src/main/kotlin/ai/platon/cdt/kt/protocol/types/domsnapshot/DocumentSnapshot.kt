@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.types.domsnapshot

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

/**
 * Document snapshot.
 */
data class DocumentSnapshot(
  @param:JsonProperty("documentURL")
  val documentURL: Int,
  @param:JsonProperty("title")
  val title: Int,
  @param:JsonProperty("baseURL")
  val baseURL: Int,
  @param:JsonProperty("contentLanguage")
  val contentLanguage: Int,
  @param:JsonProperty("encodingName")
  val encodingName: Int,
  @param:JsonProperty("publicId")
  val publicId: Int,
  @param:JsonProperty("systemId")
  val systemId: Int,
  @param:JsonProperty("frameId")
  val frameId: Int,
  @param:JsonProperty("nodes")
  val nodes: NodeTreeSnapshot,
  @param:JsonProperty("layout")
  val layout: LayoutTreeSnapshot,
  @param:JsonProperty("textBoxes")
  val textBoxes: TextBoxSnapshot,
  @param:JsonProperty("scrollOffsetX")
  @param:Optional
  val scrollOffsetX: Double? = null,
  @param:JsonProperty("scrollOffsetY")
  @param:Optional
  val scrollOffsetY: Double? = null,
  @param:JsonProperty("contentWidth")
  @param:Optional
  val contentWidth: Double? = null,
  @param:JsonProperty("contentHeight")
  @param:Optional
  val contentHeight: Double? = null,
)
