package com.github.kklisura.cdt.protocol.events.page;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
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

import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.types.page.DialogType;

/**
 * Fired when a JavaScript initiated dialog (alert, confirm, prompt, or onbeforeunload) is about to
 * open.
 */
public class JavascriptDialogOpening {

  private String url;

  private String message;

  private DialogType type;

  private Boolean hasBrowserHandler;

  @Optional private String defaultPrompt;

  /** Frame url. */
  public String getUrl() {
    return url;
  }

  /** Frame url. */
  public void setUrl(String url) {
    this.url = url;
  }

  /** Message that will be displayed by the dialog. */
  public String getMessage() {
    return message;
  }

  /** Message that will be displayed by the dialog. */
  public void setMessage(String message) {
    this.message = message;
  }

  /** Dialog type. */
  public DialogType getType() {
    return type;
  }

  /** Dialog type. */
  public void setType(DialogType type) {
    this.type = type;
  }

  /**
   * True iff browser is capable showing or acting on the given dialog. When browser has no dialog
   * handler for given target, calling alert while Page domain is engaged will stall the page
   * execution. Execution can be resumed via calling Page.handleJavaScriptDialog.
   */
  public Boolean getHasBrowserHandler() {
    return hasBrowserHandler;
  }

  /**
   * True iff browser is capable showing or acting on the given dialog. When browser has no dialog
   * handler for given target, calling alert while Page domain is engaged will stall the page
   * execution. Execution can be resumed via calling Page.handleJavaScriptDialog.
   */
  public void setHasBrowserHandler(Boolean hasBrowserHandler) {
    this.hasBrowserHandler = hasBrowserHandler;
  }

  /** Default dialog prompt. */
  public String getDefaultPrompt() {
    return defaultPrompt;
  }

  /** Default dialog prompt. */
  public void setDefaultPrompt(String defaultPrompt) {
    this.defaultPrompt = defaultPrompt;
  }
}
