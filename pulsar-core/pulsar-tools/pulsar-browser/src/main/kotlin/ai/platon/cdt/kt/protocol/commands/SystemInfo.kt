@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.Experimental
import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.systeminfo.Info
import ai.platon.cdt.kt.protocol.types.systeminfo.ProcessInfo
import kotlin.collections.List

/**
 * The SystemInfo domain defines methods and events for querying low-level system information.
 */
@Experimental
interface SystemInfo {
  /**
   * Returns information about the system.
   */
  suspend fun getInfo(): Info

  /**
   * Returns information about all running processes.
   */
  @Returns("processInfo")
  @ReturnTypeParameter(ProcessInfo::class)
  suspend fun getProcessInfo(): List<ProcessInfo>
}
