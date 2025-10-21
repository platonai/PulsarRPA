package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.webaudio.AudioListenerCreated
import ai.platon.cdt.kt.protocol.events.webaudio.AudioListenerWillBeDestroyed
import ai.platon.cdt.kt.protocol.events.webaudio.AudioNodeCreated
import ai.platon.cdt.kt.protocol.events.webaudio.AudioNodeWillBeDestroyed
import ai.platon.cdt.kt.protocol.events.webaudio.AudioParamCreated
import ai.platon.cdt.kt.protocol.events.webaudio.AudioParamWillBeDestroyed
import ai.platon.cdt.kt.protocol.events.webaudio.ContextChanged
import ai.platon.cdt.kt.protocol.events.webaudio.ContextCreated
import ai.platon.cdt.kt.protocol.events.webaudio.ContextWillBeDestroyed
import ai.platon.cdt.kt.protocol.events.webaudio.NodeParamConnected
import ai.platon.cdt.kt.protocol.events.webaudio.NodeParamDisconnected
import ai.platon.cdt.kt.protocol.events.webaudio.NodesConnected
import ai.platon.cdt.kt.protocol.events.webaudio.NodesDisconnected
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.webaudio.ContextRealtimeData
import kotlin.String
import kotlin.Unit

/**
 * This domain allows inspection of Web Audio API.
 * https://webaudio.github.io/web-audio-api/
 */
@Experimental
public interface WebAudio {
  /**
   * Enables the WebAudio domain and starts sending context lifetime events.
   */
  public suspend fun enable()

  /**
   * Disables the WebAudio domain.
   */
  public suspend fun disable()

  /**
   * Fetch the realtime data from the registered contexts.
   * @param contextId
   */
  @Returns("realtimeData")
  public suspend fun getRealtimeData(@ParamName("contextId") contextId: String): ContextRealtimeData

  @EventName("contextCreated")
  public fun onContextCreated(eventListener: EventHandler<ContextCreated>): EventListener

  @EventName("contextCreated")
  public fun onContextCreated(eventListener: suspend (ContextCreated) -> Unit): EventListener

  @EventName("contextWillBeDestroyed")
  public fun onContextWillBeDestroyed(eventListener: EventHandler<ContextWillBeDestroyed>):
      EventListener

  @EventName("contextWillBeDestroyed")
  public fun onContextWillBeDestroyed(eventListener: suspend (ContextWillBeDestroyed) -> Unit):
      EventListener

  @EventName("contextChanged")
  public fun onContextChanged(eventListener: EventHandler<ContextChanged>): EventListener

  @EventName("contextChanged")
  public fun onContextChanged(eventListener: suspend (ContextChanged) -> Unit): EventListener

  @EventName("audioListenerCreated")
  public fun onAudioListenerCreated(eventListener: EventHandler<AudioListenerCreated>):
      EventListener

  @EventName("audioListenerCreated")
  public fun onAudioListenerCreated(eventListener: suspend (AudioListenerCreated) -> Unit):
      EventListener

  @EventName("audioListenerWillBeDestroyed")
  public
      fun onAudioListenerWillBeDestroyed(eventListener: EventHandler<AudioListenerWillBeDestroyed>):
      EventListener

  @EventName("audioListenerWillBeDestroyed")
  public
      fun onAudioListenerWillBeDestroyed(eventListener: suspend (AudioListenerWillBeDestroyed) -> Unit):
      EventListener

  @EventName("audioNodeCreated")
  public fun onAudioNodeCreated(eventListener: EventHandler<AudioNodeCreated>): EventListener

  @EventName("audioNodeCreated")
  public fun onAudioNodeCreated(eventListener: suspend (AudioNodeCreated) -> Unit): EventListener

  @EventName("audioNodeWillBeDestroyed")
  public fun onAudioNodeWillBeDestroyed(eventListener: EventHandler<AudioNodeWillBeDestroyed>):
      EventListener

  @EventName("audioNodeWillBeDestroyed")
  public fun onAudioNodeWillBeDestroyed(eventListener: suspend (AudioNodeWillBeDestroyed) -> Unit):
      EventListener

  @EventName("audioParamCreated")
  public fun onAudioParamCreated(eventListener: EventHandler<AudioParamCreated>): EventListener

  @EventName("audioParamCreated")
  public fun onAudioParamCreated(eventListener: suspend (AudioParamCreated) -> Unit): EventListener

  @EventName("audioParamWillBeDestroyed")
  public fun onAudioParamWillBeDestroyed(eventListener: EventHandler<AudioParamWillBeDestroyed>):
      EventListener

  @EventName("audioParamWillBeDestroyed")
  public
      fun onAudioParamWillBeDestroyed(eventListener: suspend (AudioParamWillBeDestroyed) -> Unit):
      EventListener

  @EventName("nodesConnected")
  public fun onNodesConnected(eventListener: EventHandler<NodesConnected>): EventListener

  @EventName("nodesConnected")
  public fun onNodesConnected(eventListener: suspend (NodesConnected) -> Unit): EventListener

  @EventName("nodesDisconnected")
  public fun onNodesDisconnected(eventListener: EventHandler<NodesDisconnected>): EventListener

  @EventName("nodesDisconnected")
  public fun onNodesDisconnected(eventListener: suspend (NodesDisconnected) -> Unit): EventListener

  @EventName("nodeParamConnected")
  public fun onNodeParamConnected(eventListener: EventHandler<NodeParamConnected>): EventListener

  @EventName("nodeParamConnected")
  public fun onNodeParamConnected(eventListener: suspend (NodeParamConnected) -> Unit):
      EventListener

  @EventName("nodeParamDisconnected")
  public fun onNodeParamDisconnected(eventListener: EventHandler<NodeParamDisconnected>):
      EventListener

  @EventName("nodeParamDisconnected")
  public fun onNodeParamDisconnected(eventListener: suspend (NodeParamDisconnected) -> Unit):
      EventListener
}
