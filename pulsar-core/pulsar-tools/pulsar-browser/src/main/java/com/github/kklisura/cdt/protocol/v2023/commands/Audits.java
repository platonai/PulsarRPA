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

import com.github.kklisura.cdt.protocol.v2023.events.audits.IssueAdded;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.*;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.v2023.support.types.EventListener;
import com.github.kklisura.cdt.protocol.v2023.types.audits.EncodedResponse;
import com.github.kklisura.cdt.protocol.v2023.types.audits.GenericIssueDetails;
import com.github.kklisura.cdt.protocol.v2023.types.audits.GetEncodedResponseEncoding;

import java.util.List;

/** Audits domain allows investigation of page violations and possible improvements. */
@Experimental
public interface Audits {

  /**
   * Returns the response body and size if it were re-encoded with the specified settings. Only
   * applies to images.
   *
   * @param requestId Identifier of the network request to get content for.
   * @param encoding The encoding to use.
   */
  EncodedResponse getEncodedResponse(
      @ParamName("requestId") String requestId,
      @ParamName("encoding") GetEncodedResponseEncoding encoding);

  /**
   * Returns the response body and size if it were re-encoded with the specified settings. Only
   * applies to images.
   *
   * @param requestId Identifier of the network request to get content for.
   * @param encoding The encoding to use.
   * @param quality The quality of the encoding (0-1). (defaults to 1)
   * @param sizeOnly Whether to only return the size information (defaults to false).
   */
  EncodedResponse getEncodedResponse(
      @ParamName("requestId") String requestId,
      @ParamName("encoding") GetEncodedResponseEncoding encoding,
      @Optional @ParamName("quality") Double quality,
      @Optional @ParamName("sizeOnly") Boolean sizeOnly);

  /** Disables issues domain, prevents further issues from being reported to the client. */
  void disable();

  /**
   * Enables issues domain, sends the issues collected so far to the client by means of the
   * `issueAdded` event.
   */
  void enable();

  /**
   * Runs the contrast check for the target page. Found issues are reported using Audits.issueAdded
   * event.
   */
  void checkContrast();

  /**
   * Runs the contrast check for the target page. Found issues are reported using Audits.issueAdded
   * event.
   *
   * @param reportAAA Whether to report WCAG AAA level issues. Default is false.
   */
  void checkContrast(@Optional @ParamName("reportAAA") Boolean reportAAA);

  /**
   * Runs the form issues check for the target page. Found issues are reported using
   * Audits.issueAdded event.
   */
  @Returns("formIssues")
  @ReturnTypeParameter(GenericIssueDetails.class)
  List<GenericIssueDetails> checkFormsIssues();

  @EventName("issueAdded")
  EventListener onIssueAdded(EventHandler<IssueAdded> eventListener);
}
