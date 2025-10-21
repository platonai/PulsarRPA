package ai.platon.cdt.kt.protocol

import ai.platon.cdt.kt.protocol.commands.Accessibility
import ai.platon.cdt.kt.protocol.commands.Animation
import ai.platon.cdt.kt.protocol.commands.ApplicationCache
import ai.platon.cdt.kt.protocol.commands.Audits
import ai.platon.cdt.kt.protocol.commands.BackgroundService
import ai.platon.cdt.kt.protocol.commands.Browser
import ai.platon.cdt.kt.protocol.commands.CSS
import ai.platon.cdt.kt.protocol.commands.CacheStorage
import ai.platon.cdt.kt.protocol.commands.Cast
import ai.platon.cdt.kt.protocol.commands.Console
import ai.platon.cdt.kt.protocol.commands.DOM
import ai.platon.cdt.kt.protocol.commands.DOMDebugger
import ai.platon.cdt.kt.protocol.commands.DOMSnapshot
import ai.platon.cdt.kt.protocol.commands.DOMStorage
import ai.platon.cdt.kt.protocol.commands.Database
import ai.platon.cdt.kt.protocol.commands.Debugger
import ai.platon.cdt.kt.protocol.commands.DeviceOrientation
import ai.platon.cdt.kt.protocol.commands.Emulation
import ai.platon.cdt.kt.protocol.commands.Fetch
import ai.platon.cdt.kt.protocol.commands.HeadlessExperimental
import ai.platon.cdt.kt.protocol.commands.HeapProfiler
import ai.platon.cdt.kt.protocol.commands.IO
import ai.platon.cdt.kt.protocol.commands.IndexedDB
import ai.platon.cdt.kt.protocol.commands.Input
import ai.platon.cdt.kt.protocol.commands.Inspector
import ai.platon.cdt.kt.protocol.commands.LayerTree
import ai.platon.cdt.kt.protocol.commands.Log
import ai.platon.cdt.kt.protocol.commands.Media
import ai.platon.cdt.kt.protocol.commands.Memory
import ai.platon.cdt.kt.protocol.commands.Network
import ai.platon.cdt.kt.protocol.commands.Overlay
import ai.platon.cdt.kt.protocol.commands.Page
import ai.platon.cdt.kt.protocol.commands.Performance
import ai.platon.cdt.kt.protocol.commands.PerformanceTimeline
import ai.platon.cdt.kt.protocol.commands.Profiler
import ai.platon.cdt.kt.protocol.commands.Runtime
import ai.platon.cdt.kt.protocol.commands.Schema
import ai.platon.cdt.kt.protocol.commands.Security
import ai.platon.cdt.kt.protocol.commands.ServiceWorker
import ai.platon.cdt.kt.protocol.commands.Storage
import ai.platon.cdt.kt.protocol.commands.SystemInfo
import ai.platon.cdt.kt.protocol.commands.Target
import ai.platon.cdt.kt.protocol.commands.Tethering
import ai.platon.cdt.kt.protocol.commands.Tracing
import ai.platon.cdt.kt.protocol.commands.WebAudio
import ai.platon.cdt.kt.protocol.commands.WebAuthn

public interface ChromeDevTools {
  /**
   * This domain is deprecated - use Runtime or Log instead.
   */
  public val console: Console

  /**
   * Debugger domain exposes JavaScript debugging capabilities. It allows setting and removing
   * breakpoints, stepping through execution, exploring stack traces, etc.
   */
  public val debugger: Debugger

  public val heapProfiler: HeapProfiler

  public val profiler: Profiler

  /**
   * Runtime domain exposes JavaScript runtime by means of remote evaluation and mirror objects.
   * Evaluation results are returned as mirror object that expose object type, string representation
   * and unique identifier that can be used for further object reference. Original objects are
   * maintained in memory unless they are either explicitly released or are released along with the
   * other objects in their object group.
   */
  public val runtime: Runtime

  /**
   * This domain is deprecated.
   */
  public val schema: Schema

  public val accessibility: Accessibility

  public val animation: Animation

  public val applicationCache: ApplicationCache

  /**
   * Audits domain allows investigation of page violations and possible improvements.
   */
  public val audits: Audits

  /**
   * Defines events for background web platform features.
   */
  public val backgroundService: BackgroundService

  /**
   * The Browser domain defines methods and events for browser managing.
   */
  public val browser: Browser

  /**
   * This domain exposes CSS read/write operations. All CSS objects (stylesheets, rules, and styles)
   * have an associated `id` used in subsequent operations on the related object. Each object type
   * has
   * a specific `id` structure, and those are not interchangeable between objects of different
   * kinds.
   * CSS objects can be loaded using the `get*ForNode()` calls (which accept a DOM node id). A
   * client
   * can also keep track of stylesheets via the `styleSheetAdded`/`styleSheetRemoved` events and
   * subsequently load the required stylesheet contents using the `getStyleSheet[Text]()` methods.
   */
  public val cSS: CSS

    public val css: CSS

    public val cacheStorage: CacheStorage

  /**
   * A domain for interacting with Cast, Presentation API, and Remote Playback API
   * functionalities.
   */
  public val cast: Cast

  /**
   * This domain exposes DOM read/write operations. Each DOM Node is represented with its mirror
   * object
   * that has an `id`. This `id` can be used to get additional information on the Node, resolve it
   * into
   * the JavaScript object wrapper, etc. It is important that client receives DOM events only for
   * the
   * nodes that are known to the client. Backend keeps track of the nodes that were sent to the
   * client
   * and never sends the same node twice. It is client's responsibility to collect information about
   * the nodes that were sent to the client.<p>Note that `iframe` owner elements will return
   * corresponding document elements as their child nodes.</p>
   */
  public val dOM: DOM

    public val dom: DOM

    /**
   * DOM debugging allows setting breakpoints on particular DOM operations and events. JavaScript
   * execution will stop on these operations as if there was a regular breakpoint set.
   */
  public val dOMDebugger: DOMDebugger

  /**
   * This domain facilitates obtaining document snapshots with DOM, layout, and style information.
   */
  public val dOMSnapshot: DOMSnapshot

    public val domSnapshot: DOMSnapshot

    /**
   * Query and modify DOM storage.
   */
  public val dOMStorage: DOMStorage

  public val database: Database

  public val deviceOrientation: DeviceOrientation

  /**
   * This domain emulates different environments for the page.
   */
  public val emulation: Emulation

  /**
   * This domain provides experimental commands only supported in headless mode.
   */
  public val headlessExperimental: HeadlessExperimental

  /**
   * Input/Output operations for streams produced by DevTools.
   */
  public val iO: IO

  public val indexedDB: IndexedDB

  public val input: Input

  public val inspector: Inspector

  public val layerTree: LayerTree

  /**
   * Provides access to log entries.
   */
  public val log: Log

  public val memory: Memory

  /**
   * Network domain allows tracking network activities of the page. It exposes information about
   * http,
   * file, data and other requests and responses, their headers, bodies, timing, etc.
   */
  public val network: Network

  /**
   * This domain provides various functionality related to drawing atop the inspected page.
   */
  public val overlay: Overlay

  /**
   * Actions and events related to the inspected page belong to the page domain.
   */
  public val page: Page

  public val performance: Performance

  /**
   * Reporting of performance timeline events, as specified in
   * https://w3c.github.io/performance-timeline/#dom-performanceobserver.
   */
  public val performanceTimeline: PerformanceTimeline

  /**
   * Security
   */
  public val security: Security

  public val serviceWorker: ServiceWorker

  public val storage: Storage

  /**
   * The SystemInfo domain defines methods and events for querying low-level system information.
   */
  public val systemInfo: SystemInfo

  /**
   * Supports additional targets discovery and allows to attach to them.
   */
  public val target: Target

  /**
   * The Tethering domain defines methods and events for browser port binding.
   */
  public val tethering: Tethering

  public val tracing: Tracing

  /**
   * A domain for letting clients substitute browser's network layer with client code.
   */
  public val fetch: Fetch

  /**
   * This domain allows inspection of Web Audio API.
   * https://webaudio.github.io/web-audio-api/
   */
  public val webAudio: WebAudio

  /**
   * This domain allows configuring virtual authenticators to test the WebAuthn
   * API.
   */
  public val webAuthn: WebAuthn

  /**
   * This domain allows detailed inspection of media elements
   */
  public val media: Media
}
