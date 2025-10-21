package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.events.database.AddDatabase
import ai.platon.cdt.kt.protocol.support.annotations.EventName
import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ParamName
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.support.types.EventHandler
import ai.platon.cdt.kt.protocol.support.types.EventListener
import ai.platon.cdt.kt.protocol.types.database.ExecuteSQL
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

@Experimental
public interface Database {
  /**
   * Disables database tracking, prevents database events from being sent to the client.
   */
  public suspend fun disable()

  /**
   * Enables database tracking, database events will now be delivered to the client.
   */
  public suspend fun enable()

  /**
   * @param databaseId
   * @param query
   */
  public suspend fun executeSQL(@ParamName("databaseId") databaseId: String, @ParamName("query")
      query: String): ExecuteSQL

  /**
   * @param databaseId
   */
  @Returns("tableNames")
  @ReturnTypeParameter(String::class)
  public suspend fun getDatabaseTableNames(@ParamName("databaseId") databaseId: String):
      List<String>

  @EventName("addDatabase")
  public fun onAddDatabase(eventListener: EventHandler<AddDatabase>): EventListener

  @EventName("addDatabase")
  public fun onAddDatabase(eventListener: suspend (AddDatabase) -> Unit): EventListener
}
