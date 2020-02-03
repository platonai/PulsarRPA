package com.github.kklisura.cdt.protocol.commands;

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

import com.github.kklisura.cdt.protocol.events.fetch.AuthRequired;
import com.github.kklisura.cdt.protocol.events.fetch.RequestPaused;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;
import com.github.kklisura.cdt.protocol.types.fetch.AuthChallengeResponse;
import com.github.kklisura.cdt.protocol.types.fetch.HeaderEntry;
import com.github.kklisura.cdt.protocol.types.fetch.RequestPattern;
import com.github.kklisura.cdt.protocol.types.fetch.ResponseBody;
import com.github.kklisura.cdt.protocol.types.network.ErrorReason;
import java.util.List;

/** A domain for letting clients substitute browser's network layer with client code. */
@Experimental
public interface Fetch {

  /** Disables the fetch domain. */
  void disable();

  /**
   * Enables issuing of requestPaused events. A request will be paused until client calls one of
   * failRequest, fulfillRequest or continueRequest/continueWithAuth.
   */
  void enable();

  /**
   * Enables issuing of requestPaused events. A request will be paused until client calls one of
   * failRequest, fulfillRequest or continueRequest/continueWithAuth.
   *
   * @param patterns If specified, only requests matching any of these patterns will produce
   *     fetchRequested event and will be paused until clients response. If not set, all requests
   *     will be affected.
   * @param handleAuthRequests If true, authRequired events will be issued and requests will be
   *     paused expecting a call to continueWithAuth.
   */
  void enable(
      @Optional @ParamName("patterns") List<RequestPattern> patterns,
      @Optional @ParamName("handleAuthRequests") Boolean handleAuthRequests);

  /**
   * Causes the request to fail with specified reason.
   *
   * @param requestId An id the client received in requestPaused event.
   * @param errorReason Causes the request to fail with the given reason.
   */
  void failRequest(
      @ParamName("requestId") String requestId, @ParamName("errorReason") ErrorReason errorReason);

  /**
   * Provides response to the request.
   *
   * @param requestId An id the client received in requestPaused event.
   * @param responseCode An HTTP response code.
   * @param responseHeaders Response headers.
   */
  void fulfillRequest(
      @ParamName("requestId") String requestId,
      @ParamName("responseCode") Integer responseCode,
      @ParamName("responseHeaders") List<HeaderEntry> responseHeaders);

  /**
   * Provides response to the request.
   *
   * @param requestId An id the client received in requestPaused event.
   * @param responseCode An HTTP response code.
   * @param responseHeaders Response headers.
   * @param body A response body.
   * @param responsePhrase A textual representation of responseCode. If absent, a standard phrase
   *     mathcing responseCode is used.
   */
  void fulfillRequest(
      @ParamName("requestId") String requestId,
      @ParamName("responseCode") Integer responseCode,
      @ParamName("responseHeaders") List<HeaderEntry> responseHeaders,
      @Optional @ParamName("body") String body,
      @Optional @ParamName("responsePhrase") String responsePhrase);

  /**
   * Continues the request, optionally modifying some of its parameters.
   *
   * @param requestId An id the client received in requestPaused event.
   */
  void continueRequest(@ParamName("requestId") String requestId);

  /**
   * Continues the request, optionally modifying some of its parameters.
   *
   * @param requestId An id the client received in requestPaused event.
   * @param url If set, the request url will be modified in a way that's not observable by page.
   * @param method If set, the request method is overridden.
   * @param postData If set, overrides the post data in the request.
   * @param headers If set, overrides the request headrts.
   */
  void continueRequest(
      @ParamName("requestId") String requestId,
      @Optional @ParamName("url") String url,
      @Optional @ParamName("method") String method,
      @Optional @ParamName("postData") String postData,
      @Optional @ParamName("headers") List<HeaderEntry> headers);

  /**
   * Continues a request supplying authChallengeResponse following authRequired event.
   *
   * @param requestId An id the client received in authRequired event.
   * @param authChallengeResponse Response to with an authChallenge.
   */
  void continueWithAuth(
      @ParamName("requestId") String requestId,
      @ParamName("authChallengeResponse") AuthChallengeResponse authChallengeResponse);

  /**
   * Causes the body of the response to be received from the server and returned as a single string.
   * May only be issued for a request that is paused in the Response stage and is mutually exclusive
   * with takeResponseBodyForInterceptionAsStream. Calling other methods that affect the request or
   * disabling fetch domain before body is received results in an undefined behavior.
   *
   * @param requestId Identifier for the intercepted request to get body for.
   */
  ResponseBody getResponseBody(@ParamName("requestId") String requestId);

  /**
   * Returns a handle to the stream representing the response body. The request must be paused in
   * the HeadersReceived stage. Note that after this command the request can't be continued as is --
   * client either needs to cancel it or to provide the response body. The stream only supports
   * sequential read, IO.read will fail if the position is specified. This method is mutually
   * exclusive with getResponseBody. Calling other methods that affect the request or disabling
   * fetch domain before body is received results in an undefined behavior.
   *
   * @param requestId
   */
  @Returns("stream")
  String takeResponseBodyAsStream(@ParamName("requestId") String requestId);

  /**
   * Issued when the domain is enabled and the request URL matches the specified filter. The request
   * is paused until the client responds with one of continueRequest, failRequest or fulfillRequest.
   * The stage of the request can be determined by presence of responseErrorReason and
   * responseStatusCode -- the request is at the response stage if either of these fields is present
   * and in the request stage otherwise.
   */
  @EventName("requestPaused")
  EventListener onRequestPaused(EventHandler<RequestPaused> eventListener);

  /**
   * Issued when the domain is enabled with handleAuthRequests set to true. The request is paused
   * until client responds with continueWithAuth.
   */
  @EventName("authRequired")
  EventListener onAuthRequired(EventHandler<AuthRequired> eventListener);
}
