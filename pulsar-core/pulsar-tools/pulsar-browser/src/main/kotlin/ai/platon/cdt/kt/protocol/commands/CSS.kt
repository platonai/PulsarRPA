@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.css.FontsUpdated
import ai.platon.cdt.kt.protocol.events.css.MediaQueryResultChanged
import ai.platon.cdt.kt.protocol.events.css.StyleSheetAdded
import ai.platon.cdt.kt.protocol.events.css.StyleSheetChanged
import ai.platon.cdt.kt.protocol.events.css.StyleSheetRemoved
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.css.BackgroundColors
import ai.platon.cdt.kt.protocol.types.css.CSSComputedStyleProperty
import ai.platon.cdt.kt.protocol.types.css.CSSMedia
import ai.platon.cdt.kt.protocol.types.css.CSSRule
import ai.platon.cdt.kt.protocol.types.css.CSSStyle
import ai.platon.cdt.kt.protocol.types.css.InlineStylesForNode
import ai.platon.cdt.kt.protocol.types.css.MatchedStylesForNode
import ai.platon.cdt.kt.protocol.types.css.PlatformFontUsage
import ai.platon.cdt.kt.protocol.types.css.RuleUsage
import ai.platon.cdt.kt.protocol.types.css.SelectorList
import ai.platon.cdt.kt.protocol.types.css.SourceRange
import ai.platon.cdt.kt.protocol.types.css.StyleDeclarationEdit
import ai.platon.cdt.kt.protocol.types.css.TakeCoverageDelta
import ai.platon.cdt.kt.protocol.types.css.Value
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * This domain exposes CSS read/write operations. All CSS objects (stylesheets, rules, and styles)
 * have an associated `id` used in subsequent operations on the related object. Each object type has
 * a specific `id` structure, and those are not interchangeable between objects of different kinds.
 * CSS objects can be loaded using the `get*ForNode()` calls (which accept a DOM node id). A client
 * can also keep track of stylesheets via the `styleSheetAdded`/`styleSheetRemoved` events and
 * subsequently load the required stylesheet contents using the `getStyleSheet[Text]()` methods.
 */
@Experimental
interface CSS {
  /**
   * Inserts a new rule with the given `ruleText` in a stylesheet with given `styleSheetId`, at the
   * position specified by `location`.
   * @param styleSheetId The css style sheet identifier where a new rule should be inserted.
   * @param ruleText The text of a new rule.
   * @param location Text position of a new rule in the target style sheet.
   */
  @Returns("rule")
  suspend fun addRule(
    @ParamName("styleSheetId") styleSheetId: String,
    @ParamName("ruleText") ruleText: String,
    @ParamName("location") location: SourceRange,
  ): CSSRule

  /**
   * Returns all class names from specified stylesheet.
   * @param styleSheetId
   */
  @Returns("classNames")
  @ReturnTypeParameter(String::class)
  suspend fun collectClassNames(@ParamName("styleSheetId") styleSheetId: String): List<String>

  /**
   * Creates a new special "via-inspector" stylesheet in the frame with given `frameId`.
   * @param frameId Identifier of the frame where "via-inspector" stylesheet should be created.
   */
  @Returns("styleSheetId")
  suspend fun createStyleSheet(@ParamName("frameId") frameId: String): String

  /**
   * Disables the CSS agent for the given page.
   */
  suspend fun disable()

  /**
   * Enables the CSS agent for the given page. Clients should not assume that the CSS agent has been
   * enabled until the result of this command is received.
   */
  suspend fun enable()

  /**
   * Ensures that the given node will have specified pseudo-classes whenever its style is computed by
   * the browser.
   * @param nodeId The element id for which to force the pseudo state.
   * @param forcedPseudoClasses Element pseudo classes to force when computing the element's style.
   */
  suspend fun forcePseudoState(@ParamName("nodeId") nodeId: Int, @ParamName("forcedPseudoClasses") forcedPseudoClasses: List<String>)

  /**
   * @param nodeId Id of the node to get background colors for.
   */
  suspend fun getBackgroundColors(@ParamName("nodeId") nodeId: Int): BackgroundColors

  /**
   * Returns the computed style for a DOM node identified by `nodeId`.
   * @param nodeId
   */
  @Returns("computedStyle")
  @ReturnTypeParameter(CSSComputedStyleProperty::class)
  suspend fun getComputedStyleForNode(@ParamName("nodeId") nodeId: Int): List<CSSComputedStyleProperty>

  /**
   * Returns the styles defined inline (explicitly in the "style" attribute and implicitly, using DOM
   * attributes) for a DOM node identified by `nodeId`.
   * @param nodeId
   */
  suspend fun getInlineStylesForNode(@ParamName("nodeId") nodeId: Int): InlineStylesForNode

  /**
   * Returns requested styles for a DOM node identified by `nodeId`.
   * @param nodeId
   */
  suspend fun getMatchedStylesForNode(@ParamName("nodeId") nodeId: Int): MatchedStylesForNode

  /**
   * Returns all media queries parsed by the rendering engine.
   */
  @Returns("medias")
  @ReturnTypeParameter(CSSMedia::class)
  suspend fun getMediaQueries(): List<CSSMedia>

  /**
   * Requests information about platform fonts which we used to render child TextNodes in the given
   * node.
   * @param nodeId
   */
  @Returns("fonts")
  @ReturnTypeParameter(PlatformFontUsage::class)
  suspend fun getPlatformFontsForNode(@ParamName("nodeId") nodeId: Int): List<PlatformFontUsage>

  /**
   * Returns the current textual content for a stylesheet.
   * @param styleSheetId
   */
  @Returns("text")
  suspend fun getStyleSheetText(@ParamName("styleSheetId") styleSheetId: String): String

  /**
   * Starts tracking the given computed styles for updates. The specified array of properties
   * replaces the one previously specified. Pass empty array to disable tracking.
   * Use takeComputedStyleUpdates to retrieve the list of nodes that had properties modified.
   * The changes to computed style properties are only tracked for nodes pushed to the front-end
   * by the DOM agent. If no changes to the tracked properties occur after the node has been pushed
   * to the front-end, no updates will be issued for the node.
   * @param propertiesToTrack
   */
  @Experimental
  suspend fun trackComputedStyleUpdates(@ParamName("propertiesToTrack") propertiesToTrack: List<CSSComputedStyleProperty>)

  /**
   * Polls the next batch of computed style updates.
   */
  @Experimental
  @Returns("nodeIds")
  @ReturnTypeParameter(Int::class)
  suspend fun takeComputedStyleUpdates(): List<Int>

  /**
   * Find a rule with the given active property for the given node and set the new value for this
   * property
   * @param nodeId The element id for which to set property.
   * @param propertyName
   * @param value
   */
  suspend fun setEffectivePropertyValueForNode(
    @ParamName("nodeId") nodeId: Int,
    @ParamName("propertyName") propertyName: String,
    @ParamName("value") `value`: String,
  )

  /**
   * Modifies the keyframe rule key text.
   * @param styleSheetId
   * @param range
   * @param keyText
   */
  @Returns("keyText")
  suspend fun setKeyframeKey(
    @ParamName("styleSheetId") styleSheetId: String,
    @ParamName("range") range: SourceRange,
    @ParamName("keyText") keyText: String,
  ): Value

  /**
   * Modifies the rule selector.
   * @param styleSheetId
   * @param range
   * @param text
   */
  @Returns("media")
  suspend fun setMediaText(
    @ParamName("styleSheetId") styleSheetId: String,
    @ParamName("range") range: SourceRange,
    @ParamName("text") text: String,
  ): CSSMedia

  /**
   * Modifies the rule selector.
   * @param styleSheetId
   * @param range
   * @param selector
   */
  @Returns("selectorList")
  suspend fun setRuleSelector(
    @ParamName("styleSheetId") styleSheetId: String,
    @ParamName("range") range: SourceRange,
    @ParamName("selector") selector: String,
  ): SelectorList

  /**
   * Sets the new stylesheet text.
   * @param styleSheetId
   * @param text
   */
  @Returns("sourceMapURL")
  suspend fun setStyleSheetText(@ParamName("styleSheetId") styleSheetId: String, @ParamName("text") text: String): String?

  /**
   * Applies specified style edits one after another in the given order.
   * @param edits
   */
  @Returns("styles")
  @ReturnTypeParameter(CSSStyle::class)
  suspend fun setStyleTexts(@ParamName("edits") edits: List<StyleDeclarationEdit>): List<CSSStyle>

  /**
   * Enables the selector recording.
   */
  suspend fun startRuleUsageTracking()

  /**
   * Stop tracking rule usage and return the list of rules that were used since last call to
   * `takeCoverageDelta` (or since start of coverage instrumentation)
   */
  @Returns("ruleUsage")
  @ReturnTypeParameter(RuleUsage::class)
  suspend fun stopRuleUsageTracking(): List<RuleUsage>

  /**
   * Obtain list of rules that became used since last call to this method (or since start of coverage
   * instrumentation)
   */
  suspend fun takeCoverageDelta(): TakeCoverageDelta

  /**
   * Enables/disables rendering of local CSS fonts (enabled by default).
   * @param enabled Whether rendering of local fonts is enabled.
   */
  @Experimental
  suspend fun setLocalFontsEnabled(@ParamName("enabled") enabled: Boolean)

  @EventName("fontsUpdated")
  fun onFontsUpdated(eventListener: EventHandler<FontsUpdated>): EventListener

  @EventName("fontsUpdated")
  fun onFontsUpdated(eventListener: suspend (FontsUpdated) -> Unit): EventListener

  @EventName("mediaQueryResultChanged")
  fun onMediaQueryResultChanged(eventListener: EventHandler<MediaQueryResultChanged>): EventListener

  @EventName("mediaQueryResultChanged")
  fun onMediaQueryResultChanged(eventListener: suspend (MediaQueryResultChanged) -> Unit): EventListener

  @EventName("styleSheetAdded")
  fun onStyleSheetAdded(eventListener: EventHandler<StyleSheetAdded>): EventListener

  @EventName("styleSheetAdded")
  fun onStyleSheetAdded(eventListener: suspend (StyleSheetAdded) -> Unit): EventListener

  @EventName("styleSheetChanged")
  fun onStyleSheetChanged(eventListener: EventHandler<StyleSheetChanged>): EventListener

  @EventName("styleSheetChanged")
  fun onStyleSheetChanged(eventListener: suspend (StyleSheetChanged) -> Unit): EventListener

  @EventName("styleSheetRemoved")
  fun onStyleSheetRemoved(eventListener: EventHandler<StyleSheetRemoved>): EventListener

  @EventName("styleSheetRemoved")
  fun onStyleSheetRemoved(eventListener: suspend (StyleSheetRemoved) -> Unit): EventListener
}
