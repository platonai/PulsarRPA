package ai.platon.cdt.kt.protocol.types.domsnapshot

import ai.platon.cdt.kt.protocol.support.annotations.Optional
import com.fasterxml.jackson.`annotation`.JsonProperty
import kotlin.Double
import kotlin.Int

/**
 * Document snapshot.
 */
public data class DocumentSnapshot(
  @JsonProperty("documentURL")
  public val documentURL: Int,
  @JsonProperty("title")
  public val title: Int,
  @JsonProperty("baseURL")
  public val baseURL: Int,
  @JsonProperty("contentLanguage")
  public val contentLanguage: Int,
  @JsonProperty("encodingName")
  public val encodingName: Int,
  @JsonProperty("publicId")
  public val publicId: Int,
  @JsonProperty("systemId")
  public val systemId: Int,
  @JsonProperty("frameId")
  public val frameId: Int,
  @JsonProperty("nodes")
  public val nodes: NodeTreeSnapshot,
  @JsonProperty("layout")
  public val layout: LayoutTreeSnapshot,
  @JsonProperty("textBoxes")
  public val textBoxes: TextBoxSnapshot,
  @JsonProperty("scrollOffsetX")
  @Optional
  public val scrollOffsetX: Double? = null,
  @JsonProperty("scrollOffsetY")
  @Optional
  public val scrollOffsetY: Double? = null,
  @JsonProperty("contentWidth")
  @Optional
  public val contentWidth: Double? = null,
  @JsonProperty("contentHeight")
  @Optional
  public val contentHeight: Double? = null,
)
