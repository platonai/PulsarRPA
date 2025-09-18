package com.github.kklisura.cdt.protocol.v2023.commands;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.kklisura.cdt.protocol.v2023.events.fedcm.DialogShown;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;

/** This domain allows interacting with the FedCM dialog. */
@Experimental
public interface FedCm {

  void enable();

  /**
   * @param disableRejectionDelay Allows callers to disable the promise rejection delay that would
   *     normally happen, if this is unimportant to what's being tested. (step 4 of
   *     https://fedidcg.github.io/FedCM/#browser-api-rp-sign-in)
   */
  void enable(@Optional @ParamName("disableRejectionDelay") Boolean disableRejectionDelay);

  void disable();

  /**
   * @param dialogId
   * @param accountIndex
   */
  void selectAccount(
      @ParamName("dialogId") String dialogId, @ParamName("accountIndex") Integer accountIndex);

  /** @param dialogId */
  void dismissDialog(@ParamName("dialogId") String dialogId);

  /**
   * @param dialogId
   * @param triggerCooldown
   */
  void dismissDialog(
      @ParamName("dialogId") String dialogId,
      @Optional @ParamName("triggerCooldown") Boolean triggerCooldown);

  /**
   * Resets the cooldown time, if any, to allow the next FedCM call to show a dialog even if one was
   * recently dismissed by the user.
   */
  void resetCooldown();

  @EventName("dialogShown")
  EventListener onDialogShown(EventHandler<DialogShown> eventListener);
}
