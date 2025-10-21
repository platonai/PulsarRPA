package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.types.domsnapshot.CaptureSnapshot
import ai.platon.cdt.kt.protocol.types.domsnapshot.Snapshot
import java.lang.Deprecated
import kotlin.Boolean
import kotlin.String
import kotlin.collections.List

/**
 * This domain facilitates obtaining document snapshots with DOM, layout, and style information.
 */
@Experimental
public interface DOMSnapshot {
  /**
   * Disables DOM snapshot agent for the given page.
   */
  public suspend fun disable()

  /**
   * Enables DOM snapshot agent for the given page.
   */
  public suspend fun enable()

  /**
   * Returns a document snapshot, including the full DOM tree of the root node (including iframes,
   * template contents, and imported documents) in a flattened array, as well as layout and
   * white-listed computed style information for the nodes. Shadow DOM in the returned DOM tree is
   * flattened.
   * @param computedStyleWhitelist Whitelist of computed styles to return.
   * @param includeEventListeners Whether or not to retrieve details of DOM listeners (default
   * false).
   * @param includePaintOrder Whether to determine and include the paint order index of
   * LayoutTreeNodes (default false).
   * @param includeUserAgentShadowTree Whether to include UA shadow tree in the snapshot (default
   * false).
   */
  @Deprecated
  public suspend fun getSnapshot(
    @ParamName("computedStyleWhitelist") computedStyleWhitelist: List<String>,
    @ParamName("includeEventListeners") @Optional includeEventListeners: Boolean?,
    @ParamName("includePaintOrder") @Optional includePaintOrder: Boolean?,
    @ParamName("includeUserAgentShadowTree") @Optional includeUserAgentShadowTree: Boolean?,
  ): Snapshot

  @Deprecated
  public suspend fun getSnapshot(@ParamName("computedStyleWhitelist")
      computedStyleWhitelist: List<String>): Snapshot {
    return getSnapshot(computedStyleWhitelist, null, null, null)
  }

  /**
   * Returns a document snapshot, including the full DOM tree of the root node (including iframes,
   * template contents, and imported documents) in a flattened array, as well as layout and
   * white-listed computed style information for the nodes. Shadow DOM in the returned DOM tree is
   * flattened.
   * @param computedStyles Whitelist of computed styles to return.
   * @param includePaintOrder Whether to include layout object paint orders into the snapshot.
   * @param includeDOMRects Whether to include DOM rectangles (offsetRects, clientRects,
   * scrollRects) into the snapshot
   * @param includeBlendedBackgroundColors Whether to include blended background colors in the
   * snapshot (default: false).
   * Blended background color is achieved by blending background colors of all elements
   * that overlap with the current element.
   * @param includeTextColorOpacities Whether to include text color opacity in the snapshot
   * (default: false).
   * An element might have the opacity property set that affects the text color of the element.
   * The final text color opacity is computed based on the opacity of all overlapping elements.
   */
  public suspend fun captureSnapshot(
    @ParamName("computedStyles") computedStyles: List<String>,
    @ParamName("includePaintOrder") @Optional includePaintOrder: Boolean?,
    @ParamName("includeDOMRects") @Optional includeDOMRects: Boolean?,
    @ParamName("includeBlendedBackgroundColors") @Optional @Experimental
        includeBlendedBackgroundColors: Boolean?,
    @ParamName("includeTextColorOpacities") @Optional @Experimental
        includeTextColorOpacities: Boolean?,
  ): CaptureSnapshot

  public suspend fun captureSnapshot(@ParamName("computedStyles") computedStyles: List<String>):
      CaptureSnapshot {
    return captureSnapshot(computedStyles, null, null, null, null)
  }
}
