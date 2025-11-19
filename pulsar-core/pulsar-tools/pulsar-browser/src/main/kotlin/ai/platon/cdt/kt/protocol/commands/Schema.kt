@file:Suppress("unused")
package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.schema.Domain
import kotlin.Deprecated
import kotlin.collections.List

/**
 * This domain is deprecated.
 */
@Deprecated("Deprecated by protocol")
interface Schema {
  /**
   * Returns supported domains.
   */
  @Returns("domains")
  @ReturnTypeParameter(Domain::class)
  suspend fun getDomains(): List<Domain>
}
