package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.page.BackForwardCacheNotUsed
import ai.platon.cdt.kt.protocol.events.page.CompilationCacheProduced
import ai.platon.cdt.kt.protocol.events.page.DocumentOpened
import ai.platon.cdt.kt.protocol.events.page.DomContentEventFired
import ai.platon.cdt.kt.protocol.events.page.DownloadProgress
import ai.platon.cdt.kt.protocol.events.page.DownloadWillBegin
import ai.platon.cdt.kt.protocol.events.page.FileChooserOpened
import ai.platon.cdt.kt.protocol.events.page.FrameAttached
import ai.platon.cdt.kt.protocol.events.page.FrameClearedScheduledNavigation
import ai.platon.cdt.kt.protocol.events.page.FrameDetached
import ai.platon.cdt.kt.protocol.events.page.FrameNavigated
import ai.platon.cdt.kt.protocol.events.page.FrameRequestedNavigation
import ai.platon.cdt.kt.protocol.events.page.FrameResized
import ai.platon.cdt.kt.protocol.events.page.FrameScheduledNavigation
import ai.platon.cdt.kt.protocol.events.page.FrameStartedLoading
import ai.platon.cdt.kt.protocol.events.page.FrameStoppedLoading
import ai.platon.cdt.kt.protocol.events.page.InterstitialHidden
import ai.platon.cdt.kt.protocol.events.page.InterstitialShown
import ai.platon.cdt.kt.protocol.events.page.JavascriptDialogClosed
import ai.platon.cdt.kt.protocol.events.page.JavascriptDialogOpening
import ai.platon.cdt.kt.protocol.events.page.LifecycleEvent
import ai.platon.cdt.kt.protocol.events.page.LoadEventFired
import ai.platon.cdt.kt.protocol.events.page.NavigatedWithinDocument
import ai.platon.cdt.kt.protocol.events.page.ScreencastFrame
import ai.platon.cdt.kt.protocol.events.page.ScreencastVisibilityChanged
import ai.platon.cdt.kt.protocol.events.page.WindowOpen
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.Optional
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.debugger.SearchMatch
import ai.platon.cdt.kt.protocol.types.page.AppManifest
import ai.platon.cdt.kt.protocol.types.page.CaptureScreenshotFormat
import ai.platon.cdt.kt.protocol.types.page.CaptureSnapshotFormat
import ai.platon.cdt.kt.protocol.types.page.CompilationCacheParams
import ai.platon.cdt.kt.protocol.types.page.FontFamilies
import ai.platon.cdt.kt.protocol.types.page.FontSizes
import ai.platon.cdt.kt.protocol.types.page.FrameResourceTree
import ai.platon.cdt.kt.protocol.types.page.FrameTree
import ai.platon.cdt.kt.protocol.types.page.InstallabilityError
import ai.platon.cdt.kt.protocol.types.page.LayoutMetrics
import ai.platon.cdt.kt.protocol.types.page.Navigate
import ai.platon.cdt.kt.protocol.types.page.NavigationHistory
import ai.platon.cdt.kt.protocol.types.page.PermissionsPolicyFeatureState
import ai.platon.cdt.kt.protocol.types.page.PrintToPDF
import ai.platon.cdt.kt.protocol.types.page.PrintToPDFTransferMode
import ai.platon.cdt.kt.protocol.types.page.ReferrerPolicy
import ai.platon.cdt.kt.protocol.types.page.ResourceContent
import ai.platon.cdt.kt.protocol.types.page.SetDownloadBehaviorBehavior
import ai.platon.cdt.kt.protocol.types.page.SetWebLifecycleStateState
import ai.platon.cdt.kt.protocol.types.page.StartScreencastFormat
import ai.platon.cdt.kt.protocol.types.page.TransitionType
import ai.platon.cdt.kt.protocol.types.page.Viewport
import java.lang.Deprecated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

/**
 * Actions and events related to the inspected page belong to the page domain.
 */
public interface Page {
  /**
   * Deprecated, please use addScriptToEvaluateOnNewDocument instead.
   * @param scriptSource
   */
  @Deprecated
  @Experimental
  @Returns("identifier")
  public suspend fun addScriptToEvaluateOnLoad(@ParamName("scriptSource") scriptSource: String):
      String

  /**
   * Evaluates given script in every frame upon creation (before loading frame's scripts).
   * @param source
   * @param worldName If specified, creates an isolated world with the given name and evaluates
   * given script in it.
   * This world name will be used as the ExecutionContextDescription::name when the corresponding
   * event is emitted.
   */
  @Returns("identifier")
  public suspend fun addScriptToEvaluateOnNewDocument(@ParamName("source") source: String,
      @ParamName("worldName") @Optional @Experimental worldName: String?): String

  @Returns("identifier")
  public suspend fun addScriptToEvaluateOnNewDocument(@ParamName("source") source: String): String {
    return addScriptToEvaluateOnNewDocument(source, null)
  }

  /**
   * Brings page to front (activates tab).
   */
  public suspend fun bringToFront()

  /**
   * Capture page screenshot.
   * @param format Image compression format (defaults to png).
   * @param quality Compression quality from range [0..100] (jpeg only).
   * @param clip Capture the screenshot of a given region only.
   * @param fromSurface Capture the screenshot from the surface, rather than the view. Defaults to
   * true.
   * @param captureBeyondViewport Capture the screenshot beyond the viewport. Defaults to false.
   */
  @Returns("data")
  public suspend fun captureScreenshot(
    @ParamName("format") @Optional format: CaptureScreenshotFormat? = null,
    @ParamName("quality") @Optional quality: Int? = null,
    @ParamName("clip") @Optional clip: Viewport? = null,
    @ParamName("fromSurface") @Optional @Experimental fromSurface: Boolean? = null,
    @ParamName("captureBeyondViewport") @Optional @Experimental captureBeyondViewport: Boolean? = null,
  ): String

  /**
   * Returns a snapshot of the page as a string. For MHTML format, the serialization includes
   * iframes, shadow DOM, external resources, and element-inline styles.
   * @param format Format (defaults to mhtml).
   */
  @Experimental
  @Returns("data")
  public suspend fun captureSnapshot(@ParamName("format") @Optional format: CaptureSnapshotFormat?):
      String

  @Experimental
  @Returns("data")
  public suspend fun captureSnapshot(): String {
    return captureSnapshot(null)
  }

  /**
   * Creates an isolated world for the given frame.
   * @param frameId Id of the frame in which the isolated world should be created.
   * @param worldName An optional name which is reported in the Execution Context.
   * @param grantUniveralAccess Whether or not universal access should be granted to the isolated
   * world. This is a powerful
   * option, use with caution.
   */
  @Returns("executionContextId")
  public suspend fun createIsolatedWorld(
    @ParamName("frameId") frameId: String,
    @ParamName("worldName") @Optional worldName: String?,
    @ParamName("grantUniveralAccess") @Optional grantUniveralAccess: Boolean?,
  ): Int

  @Returns("executionContextId")
  public suspend fun createIsolatedWorld(@ParamName("frameId") frameId: String): Int {
    return createIsolatedWorld(frameId, null, null)
  }

  /**
   * Disables page domain notifications.
   */
  public suspend fun disable()

  /**
   * Enables page domain notifications.
   */
  public suspend fun enable()

  public suspend fun getAppManifest(): AppManifest

  @Experimental
  @Returns("installabilityErrors")
  @ReturnTypeParameter(InstallabilityError::class)
  public suspend fun getInstallabilityErrors(): List<InstallabilityError>

  @Experimental
  @Returns("primaryIcon")
  public suspend fun getManifestIcons(): String?

  /**
   * Returns present frame tree structure.
   */
  @Returns("frameTree")
  public suspend fun getFrameTree(): FrameTree

  /**
   * Returns metrics relating to the layouting of the page, such as viewport bounds/scale.
   */
  public suspend fun getLayoutMetrics(): LayoutMetrics

  /**
   * Returns navigation history for the current page.
   */
  public suspend fun getNavigationHistory(): NavigationHistory

  /**
   * Resets navigation history for the current page.
   */
  public suspend fun resetNavigationHistory()

  /**
   * Returns content of the given resource.
   * @param frameId Frame id to get resource for.
   * @param url URL of the resource to get content for.
   */
  @Experimental
  public suspend fun getResourceContent(@ParamName("frameId") frameId: String, @ParamName("url")
      url: String): ResourceContent

  /**
   * Returns present frame / resource tree structure.
   */
  @Experimental
  @Returns("frameTree")
  public suspend fun getResourceTree(): FrameResourceTree

  /**
   * Accepts or dismisses a JavaScript initiated dialog (alert, confirm, prompt, or onbeforeunload).
   * @param accept Whether to accept or dismiss the dialog.
   * @param promptText The text to enter into the dialog prompt before accepting. Used only if this
   * is a prompt
   * dialog.
   */
  public suspend fun handleJavaScriptDialog(@ParamName("accept") accept: Boolean,
      @ParamName("promptText") @Optional promptText: String?)

  public suspend fun handleJavaScriptDialog(@ParamName("accept") accept: Boolean) {
    return handleJavaScriptDialog(accept, null)
  }

  /**
   * Navigates current page to the given URL.
   * @param url URL to navigate the page to.
   * @param referrer Referrer URL.
   * @param transitionType Intended transition type.
   * @param frameId Frame id to navigate, if not specified navigates the top frame.
   * @param referrerPolicy Referrer-policy used for the navigation.
   */
  public suspend fun navigate(
    @ParamName("url") url: String,
    @ParamName("referrer") @Optional referrer: String?,
    @ParamName("transitionType") @Optional transitionType: TransitionType?,
    @ParamName("frameId") @Optional frameId: String?,
    @ParamName("referrerPolicy") @Optional @Experimental referrerPolicy: ReferrerPolicy?,
  ): Navigate

  public suspend fun navigate(@ParamName("url") url: String): Navigate {
    return navigate(url, null, null, null, null)
  }

  /**
   * Navigates current page to the given history entry.
   * @param entryId Unique id of the entry to navigate to.
   */
  public suspend fun navigateToHistoryEntry(@ParamName("entryId") entryId: Int)

  /**
   * Print page as PDF.
   * @param landscape Paper orientation. Defaults to false.
   * @param displayHeaderFooter Display header and footer. Defaults to false.
   * @param printBackground Print background graphics. Defaults to false.
   * @param scale Scale of the webpage rendering. Defaults to 1.
   * @param paperWidth Paper width in inches. Defaults to 8.5 inches.
   * @param paperHeight Paper height in inches. Defaults to 11 inches.
   * @param marginTop Top margin in inches. Defaults to 1cm (~0.4 inches).
   * @param marginBottom Bottom margin in inches. Defaults to 1cm (~0.4 inches).
   * @param marginLeft Left margin in inches. Defaults to 1cm (~0.4 inches).
   * @param marginRight Right margin in inches. Defaults to 1cm (~0.4 inches).
   * @param pageRanges Paper ranges to print, e.g., '1-5, 8, 11-13'. Defaults to the empty string,
   * which means
   * print all pages.
   * @param ignoreInvalidPageRanges Whether to silently ignore invalid but successfully parsed page
   * ranges, such as '3-2'.
   * Defaults to false.
   * @param headerTemplate HTML template for the print header. Should be valid HTML markup with
   * following
   * classes used to inject printing values into them:
   * - `date`: formatted print date
   * - `title`: document title
   * - `url`: document location
   * - `pageNumber`: current page number
   * - `totalPages`: total pages in the document
   *
   * For example, `<span class=title></span>` would generate span containing the title.
   * @param footerTemplate HTML template for the print footer. Should use the same format as the
   * `headerTemplate`.
   * @param preferCSSPageSize Whether or not to prefer page size as defined by css. Defaults to
   * false,
   * in which case the content will be scaled to fit the paper size.
   * @param transferMode return as stream
   */
  public suspend fun printToPDF(
    @ParamName("landscape") @Optional landscape: Boolean?,
    @ParamName("displayHeaderFooter") @Optional displayHeaderFooter: Boolean?,
    @ParamName("printBackground") @Optional printBackground: Boolean?,
    @ParamName("scale") @Optional scale: Double?,
    @ParamName("paperWidth") @Optional paperWidth: Double?,
    @ParamName("paperHeight") @Optional paperHeight: Double?,
    @ParamName("marginTop") @Optional marginTop: Double?,
    @ParamName("marginBottom") @Optional marginBottom: Double?,
    @ParamName("marginLeft") @Optional marginLeft: Double?,
    @ParamName("marginRight") @Optional marginRight: Double?,
    @ParamName("pageRanges") @Optional pageRanges: String?,
    @ParamName("ignoreInvalidPageRanges") @Optional ignoreInvalidPageRanges: Boolean?,
    @ParamName("headerTemplate") @Optional headerTemplate: String?,
    @ParamName("footerTemplate") @Optional footerTemplate: String?,
    @ParamName("preferCSSPageSize") @Optional preferCSSPageSize: Boolean?,
    @ParamName("transferMode") @Optional @Experimental transferMode: PrintToPDFTransferMode?,
  ): PrintToPDF

  public suspend fun printToPDF(): PrintToPDF {
    return printToPDF(null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null)
  }

  /**
   * Reloads given page optionally ignoring the cache.
   * @param ignoreCache If true, browser cache is ignored (as if the user pressed Shift+refresh).
   * @param scriptToEvaluateOnLoad If set, the script will be injected into all frames of the
   * inspected page after reload.
   * Argument will be ignored if reloading dataURL origin.
   */
  public suspend fun reload(@ParamName("ignoreCache") @Optional ignoreCache: Boolean?,
      @ParamName("scriptToEvaluateOnLoad") @Optional scriptToEvaluateOnLoad: String?)

  public suspend fun reload() {
    return reload(null, null)
  }

  /**
   * Deprecated, please use removeScriptToEvaluateOnNewDocument instead.
   * @param identifier
   */
  @Deprecated
  @Experimental
  public suspend fun removeScriptToEvaluateOnLoad(@ParamName("identifier") identifier: String)

  /**
   * Removes given script from the list.
   * @param identifier
   */
  public suspend fun removeScriptToEvaluateOnNewDocument(@ParamName("identifier")
      identifier: String)

  /**
   * Acknowledges that a screencast frame has been received by the frontend.
   * @param sessionId Frame number.
   */
  @Experimental
  public suspend fun screencastFrameAck(@ParamName("sessionId") sessionId: Int)

  /**
   * Searches for given string in resource content.
   * @param frameId Frame id for resource to search in.
   * @param url URL of the resource to search in.
   * @param query String to search for.
   * @param caseSensitive If true, search is case sensitive.
   * @param isRegex If true, treats string parameter as regex.
   */
  @Experimental
  @Returns("result")
  @ReturnTypeParameter(SearchMatch::class)
  public suspend fun searchInResource(
    @ParamName("frameId") frameId: String,
    @ParamName("url") url: String,
    @ParamName("query") query: String,
    @ParamName("caseSensitive") @Optional caseSensitive: Boolean?,
    @ParamName("isRegex") @Optional isRegex: Boolean?,
  ): List<SearchMatch>

  @Experimental
  @Returns("result")
  @ReturnTypeParameter(SearchMatch::class)
  public suspend fun searchInResource(
    @ParamName("frameId") frameId: String,
    @ParamName("url") url: String,
    @ParamName("query") query: String,
  ): List<SearchMatch> {
    return searchInResource(frameId, url, query, null, null)
  }

  /**
   * Enable Chrome's experimental ad filter on all sites.
   * @param enabled Whether to block ads.
   */
  @Experimental
  public suspend fun setAdBlockingEnabled(@ParamName("enabled") enabled: Boolean)

  /**
   * Enable page Content Security Policy by-passing.
   * @param enabled Whether to bypass page CSP.
   */
  @Experimental
  public suspend fun setBypassCSP(@ParamName("enabled") enabled: Boolean)

  /**
   * Get Permissions Policy state on given frame.
   * @param frameId
   */
  @Experimental
  @Returns("states")
  @ReturnTypeParameter(PermissionsPolicyFeatureState::class)
  public suspend fun getPermissionsPolicyState(@ParamName("frameId") frameId: String):
      List<PermissionsPolicyFeatureState>

  /**
   * Set generic font families.
   * @param fontFamilies Specifies font families to set. If a font family is not specified, it won't
   * be changed.
   */
  @Experimental
  public suspend fun setFontFamilies(@ParamName("fontFamilies") fontFamilies: FontFamilies)

  /**
   * Set default font sizes.
   * @param fontSizes Specifies font sizes to set. If a font size is not specified, it won't be
   * changed.
   */
  @Experimental
  public suspend fun setFontSizes(@ParamName("fontSizes") fontSizes: FontSizes)

  /**
   * Sets given markup as the document's HTML.
   * @param frameId Frame id to set HTML for.
   * @param html HTML content to set.
   */
  public suspend fun setDocumentContent(@ParamName("frameId") frameId: String, @ParamName("html")
      html: String)

  /**
   * Set the behavior when downloading a file.
   * @param behavior Whether to allow all or deny all download requests, or use default Chrome
   * behavior if
   * available (otherwise deny).
   * @param downloadPath The default path to save downloaded files to. This is required if behavior
   * is set to 'allow'
   */
  @Deprecated
  @Experimental
  public suspend fun setDownloadBehavior(@ParamName("behavior")
      behavior: SetDownloadBehaviorBehavior, @ParamName("downloadPath") @Optional
      downloadPath: String?)

  @Deprecated
  @Experimental
  public suspend fun setDownloadBehavior(@ParamName("behavior")
      behavior: SetDownloadBehaviorBehavior) {
    return setDownloadBehavior(behavior, null)
  }

  /**
   * Controls whether page will emit lifecycle events.
   * @param enabled If true, starts emitting lifecycle events.
   */
  @Experimental
  public suspend fun setLifecycleEventsEnabled(@ParamName("enabled") enabled: Boolean)

  /**
   * Starts sending each frame using the `screencastFrame` event.
   * @param format Image compression format.
   * @param quality Compression quality from range [0..100].
   * @param maxWidth Maximum screenshot width.
   * @param maxHeight Maximum screenshot height.
   * @param everyNthFrame Send every n-th frame.
   */
  @Experimental
  public suspend fun startScreencast(
    @ParamName("format") @Optional format: StartScreencastFormat?,
    @ParamName("quality") @Optional quality: Int?,
    @ParamName("maxWidth") @Optional maxWidth: Int?,
    @ParamName("maxHeight") @Optional maxHeight: Int?,
    @ParamName("everyNthFrame") @Optional everyNthFrame: Int?,
  )

  @Experimental
  public suspend fun startScreencast() {
    return startScreencast(null, null, null, null, null)
  }

  /**
   * Force the page stop all navigations and pending resource fetches.
   */
  public suspend fun stopLoading()

  /**
   * Crashes renderer on the IO thread, generates minidumps.
   */
  @Experimental
  public suspend fun crash()

  /**
   * Tries to close page, running its beforeunload hooks, if any.
   */
  @Experimental
  public suspend fun close()

  /**
   * Tries to update the web lifecycle state of the page.
   * It will transition the page to the given state according to:
   * https://github.com/WICG/web-lifecycle/
   * @param state Target lifecycle state
   */
  @Experimental
  public suspend fun setWebLifecycleState(@ParamName("state") state: SetWebLifecycleStateState)

  /**
   * Stops sending each frame in the `screencastFrame`.
   */
  @Experimental
  public suspend fun stopScreencast()

  /**
   * Forces compilation cache to be generated for every subresource script.
   * See also: `Page.produceCompilationCache`.
   * @param enabled
   */
  @Experimental
  public suspend fun setProduceCompilationCache(@ParamName("enabled") enabled: Boolean)

  /**
   * Requests backend to produce compilation cache for the specified scripts.
   * Unlike setProduceCompilationCache, this allows client to only produce cache
   * for specific scripts. `scripts` are appeneded to the list of scripts
   * for which the cache for would produced. Disabling compilation cache with
   * `setProduceCompilationCache` would reset all pending cache requests.
   * The list may also be reset during page navigation.
   * When script with a matching URL is encountered, the cache is optionally
   * produced upon backend discretion, based on internal heuristics.
   * See also: `Page.compilationCacheProduced`.
   * @param scripts
   */
  @Experimental
  public suspend fun produceCompilationCache(@ParamName("scripts")
      scripts: List<CompilationCacheParams>)

  /**
   * Seeds compilation cache for given url. Compilation cache does not survive
   * cross-process navigation.
   * @param url
   * @param data Base64-encoded data (Encoded as a base64 string when passed over JSON)
   */
  @Experimental
  public suspend fun addCompilationCache(@ParamName("url") url: String, @ParamName("data")
      `data`: String)

  /**
   * Clears seeded compilation cache.
   */
  @Experimental
  public suspend fun clearCompilationCache()

  /**
   * Generates a report for testing.
   * @param message Message to be displayed in the report.
   * @param group Specifies the endpoint group to deliver the report to.
   */
  @Experimental
  public suspend fun generateTestReport(@ParamName("message") message: String, @ParamName("group")
      @Optional group: String?)

  @Experimental
  public suspend fun generateTestReport(@ParamName("message") message: String) {
    return generateTestReport(message, null)
  }

  /**
   * Pauses page execution. Can be resumed using generic Runtime.runIfWaitingForDebugger.
   */
  @Experimental
  public suspend fun waitForDebugger()

  /**
   * Intercept file chooser requests and transfer control to protocol clients.
   * When file chooser interception is enabled, native file chooser dialog is not shown.
   * Instead, a protocol event `Page.fileChooserOpened` is emitted.
   * @param enabled
   */
  @Experimental
  public suspend fun setInterceptFileChooserDialog(@ParamName("enabled") enabled: Boolean)

  @EventName("domContentEventFired")
  public fun onDomContentEventFired(eventListener: EventHandler<DomContentEventFired>):
      EventListener

  @EventName("domContentEventFired")
  public fun onDomContentEventFired(eventListener: suspend (DomContentEventFired) -> Unit):
      EventListener

  @EventName("fileChooserOpened")
  public fun onFileChooserOpened(eventListener: EventHandler<FileChooserOpened>): EventListener

  @EventName("fileChooserOpened")
  public fun onFileChooserOpened(eventListener: suspend (FileChooserOpened) -> Unit): EventListener

  @EventName("frameAttached")
  public fun onFrameAttached(eventListener: EventHandler<FrameAttached>): EventListener

  @EventName("frameAttached")
  public fun onFrameAttached(eventListener: suspend (FrameAttached) -> Unit): EventListener

  @EventName("frameClearedScheduledNavigation")
  @Deprecated
  public
      fun onFrameClearedScheduledNavigation(eventListener: EventHandler<FrameClearedScheduledNavigation>):
      EventListener

  @EventName("frameClearedScheduledNavigation")
  @Deprecated
  public
      fun onFrameClearedScheduledNavigation(eventListener: suspend (FrameClearedScheduledNavigation) -> Unit):
      EventListener

  @EventName("frameDetached")
  public fun onFrameDetached(eventListener: EventHandler<FrameDetached>): EventListener

  @EventName("frameDetached")
  public fun onFrameDetached(eventListener: suspend (FrameDetached) -> Unit): EventListener

  @EventName("frameNavigated")
  public fun onFrameNavigated(eventListener: EventHandler<FrameNavigated>): EventListener

  @EventName("frameNavigated")
  public fun onFrameNavigated(eventListener: suspend (FrameNavigated) -> Unit): EventListener

  @EventName("documentOpened")
  @Experimental
  public fun onDocumentOpened(eventListener: EventHandler<DocumentOpened>): EventListener

  @EventName("documentOpened")
  @Experimental
  public fun onDocumentOpened(eventListener: suspend (DocumentOpened) -> Unit): EventListener

  @EventName("frameResized")
  @Experimental
  public fun onFrameResized(eventListener: EventHandler<FrameResized>): EventListener

  @EventName("frameResized")
  @Experimental
  public fun onFrameResized(eventListener: suspend (FrameResized) -> Unit): EventListener

  @EventName("frameRequestedNavigation")
  @Experimental
  public fun onFrameRequestedNavigation(eventListener: EventHandler<FrameRequestedNavigation>):
      EventListener

  @EventName("frameRequestedNavigation")
  @Experimental
  public fun onFrameRequestedNavigation(eventListener: suspend (FrameRequestedNavigation) -> Unit):
      EventListener

  @EventName("frameScheduledNavigation")
  @Deprecated
  public fun onFrameScheduledNavigation(eventListener: EventHandler<FrameScheduledNavigation>):
      EventListener

  @EventName("frameScheduledNavigation")
  @Deprecated
  public fun onFrameScheduledNavigation(eventListener: suspend (FrameScheduledNavigation) -> Unit):
      EventListener

  @EventName("frameStartedLoading")
  @Experimental
  public fun onFrameStartedLoading(eventListener: EventHandler<FrameStartedLoading>): EventListener

  @EventName("frameStartedLoading")
  @Experimental
  public fun onFrameStartedLoading(eventListener: suspend (FrameStartedLoading) -> Unit):
      EventListener

  @EventName("frameStoppedLoading")
  @Experimental
  public fun onFrameStoppedLoading(eventListener: EventHandler<FrameStoppedLoading>): EventListener

  @EventName("frameStoppedLoading")
  @Experimental
  public fun onFrameStoppedLoading(eventListener: suspend (FrameStoppedLoading) -> Unit):
      EventListener

  @EventName("downloadWillBegin")
  @Deprecated
  @Experimental
  public fun onDownloadWillBegin(eventListener: EventHandler<DownloadWillBegin>): EventListener

  @EventName("downloadWillBegin")
  @Deprecated
  @Experimental
  public fun onDownloadWillBegin(eventListener: suspend (DownloadWillBegin) -> Unit): EventListener

  @EventName("downloadProgress")
  @Deprecated
  @Experimental
  public fun onDownloadProgress(eventListener: EventHandler<DownloadProgress>): EventListener

  @EventName("downloadProgress")
  @Deprecated
  @Experimental
  public fun onDownloadProgress(eventListener: suspend (DownloadProgress) -> Unit): EventListener

  @EventName("interstitialHidden")
  public fun onInterstitialHidden(eventListener: EventHandler<InterstitialHidden>): EventListener

  @EventName("interstitialHidden")
  public fun onInterstitialHidden(eventListener: suspend (InterstitialHidden) -> Unit):
      EventListener

  @EventName("interstitialShown")
  public fun onInterstitialShown(eventListener: EventHandler<InterstitialShown>): EventListener

  @EventName("interstitialShown")
  public fun onInterstitialShown(eventListener: suspend (InterstitialShown) -> Unit): EventListener

  @EventName("javascriptDialogClosed")
  public fun onJavascriptDialogClosed(eventListener: EventHandler<JavascriptDialogClosed>):
      EventListener

  @EventName("javascriptDialogClosed")
  public fun onJavascriptDialogClosed(eventListener: suspend (JavascriptDialogClosed) -> Unit):
      EventListener

  @EventName("javascriptDialogOpening")
  public fun onJavascriptDialogOpening(eventListener: EventHandler<JavascriptDialogOpening>):
      EventListener

  @EventName("javascriptDialogOpening")
  public fun onJavascriptDialogOpening(eventListener: suspend (JavascriptDialogOpening) -> Unit):
      EventListener

  @EventName("lifecycleEvent")
  public fun onLifecycleEvent(eventListener: EventHandler<LifecycleEvent>): EventListener

  @EventName("lifecycleEvent")
  public fun onLifecycleEvent(eventListener: suspend (LifecycleEvent) -> Unit): EventListener

  @EventName("backForwardCacheNotUsed")
  @Experimental
  public fun onBackForwardCacheNotUsed(eventListener: EventHandler<BackForwardCacheNotUsed>):
      EventListener

  @EventName("backForwardCacheNotUsed")
  @Experimental
  public fun onBackForwardCacheNotUsed(eventListener: suspend (BackForwardCacheNotUsed) -> Unit):
      EventListener

  @EventName("loadEventFired")
  public fun onLoadEventFired(eventListener: EventHandler<LoadEventFired>): EventListener

  @EventName("loadEventFired")
  public fun onLoadEventFired(eventListener: suspend (LoadEventFired) -> Unit): EventListener

  @EventName("navigatedWithinDocument")
  @Experimental
  public fun onNavigatedWithinDocument(eventListener: EventHandler<NavigatedWithinDocument>):
      EventListener

  @EventName("navigatedWithinDocument")
  @Experimental
  public fun onNavigatedWithinDocument(eventListener: suspend (NavigatedWithinDocument) -> Unit):
      EventListener

  @EventName("screencastFrame")
  @Experimental
  public fun onScreencastFrame(eventListener: EventHandler<ScreencastFrame>): EventListener

  @EventName("screencastFrame")
  @Experimental
  public fun onScreencastFrame(eventListener: suspend (ScreencastFrame) -> Unit): EventListener

  @EventName("screencastVisibilityChanged")
  @Experimental
  public
      fun onScreencastVisibilityChanged(eventListener: EventHandler<ScreencastVisibilityChanged>):
      EventListener

  @EventName("screencastVisibilityChanged")
  @Experimental
  public
      fun onScreencastVisibilityChanged(eventListener: suspend (ScreencastVisibilityChanged) -> Unit):
      EventListener

  @EventName("windowOpen")
  public fun onWindowOpen(eventListener: EventHandler<WindowOpen>): EventListener

  @EventName("windowOpen")
  public fun onWindowOpen(eventListener: suspend (WindowOpen) -> Unit): EventListener

  @EventName("compilationCacheProduced")
  @Experimental
  public fun onCompilationCacheProduced(eventListener: EventHandler<CompilationCacheProduced>):
      EventListener

  @EventName("compilationCacheProduced")
  @Experimental
  public fun onCompilationCacheProduced(eventListener: suspend (CompilationCacheProduced) -> Unit):
      EventListener
}
