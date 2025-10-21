package ai.platon.cdt.kt.protocol.support.types

public fun interface EventHandler<T> {
  public suspend fun onEvent(event: T)
}

public interface EventListener {
  /**
   * Alias to unsubscribe.  
   */
  public fun off()

  /**
   * Unsubscribe this event listener.  
   */
  public fun unsubscribe()
}
