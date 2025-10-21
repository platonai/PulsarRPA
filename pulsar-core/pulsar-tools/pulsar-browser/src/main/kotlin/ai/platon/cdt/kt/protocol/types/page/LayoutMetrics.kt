package ai.platon.cdt.kt.protocol.types.page

import ai.platon.cdt.kt.protocol.types.dom.Rect
import com.fasterxml.jackson.`annotation`.JsonProperty
import java.lang.Deprecated

public data class LayoutMetrics(
  @JsonProperty("layoutViewport")
  @Deprecated
  public val layoutViewport: LayoutViewport,
  @JsonProperty("visualViewport")
  @Deprecated
  public val visualViewport: VisualViewport,
  @JsonProperty("contentSize")
  @Deprecated
  public val contentSize: Rect,
  @JsonProperty("cssLayoutViewport")
  public val cssLayoutViewport: LayoutViewport,
  @JsonProperty("cssVisualViewport")
  public val cssVisualViewport: VisualViewport,
  @JsonProperty("cssContentSize")
  public val cssContentSize: Rect,
)
