package ai.platon.cdt.kt.protocol.commands

import ai.platon.cdt.kt.protocol.support.annotations.ReturnTypeParameter
import ai.platon.cdt.kt.protocol.support.annotations.Returns
import ai.platon.cdt.kt.protocol.types.schema.Domain
import java.lang.Deprecated
import kotlin.collections.List

/**
 * This domain is deprecated.
 */
@Deprecated
public interface Schema {
  /**
   * Returns supported domains.
   */
  @Returns("domains")
  @ReturnTypeParameter(Domain::class)
  public suspend fun getDomains(): List<Domain>
}
