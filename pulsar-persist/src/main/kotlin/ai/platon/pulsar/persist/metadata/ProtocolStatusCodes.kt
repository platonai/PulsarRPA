/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.persist.metadata

import ai.platon.pulsar.common.ResourceStatus

/**
 * ProtocolStatusCodes describe the fetch phase status, inherited from the standard HTTP error code.
 *
 * @link [List_of_HTTP_status_codes](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes)
 * @link [Status](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status)
 * @link [HttpStatus](http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpStatus.html)
 * @author vincent
 */
object ProtocolStatusCodes {
    //
    // The following codes are compatible with HTTP status codes, close but may not equals to HTTP status code
    // @see {https://en.wikipedia.org/wiki/List_of_HTTP_status_codes}
    //
    /**
     * Resource is OK
     */
    const val SUCCESS_OK: Int = ResourceStatus.SC_OK

    /**
     * Resource is created
     */
    const val CREATED: Int = ResourceStatus.SC_CREATED

    /**
     * Resource has moved permanently. New url should be found in args.
     */
    const val MOVED_PERMANENTLY: Int = ResourceStatus.SC_MOVED_PERMANENTLY

    /**
     * Resource has moved temporarily. New url should be found in args.
     */
    const val MOVED_TEMPORARILY: Int = ResourceStatus.SC_MOVED_TEMPORARILY

    /**
     * Unchanged since the last fetch.
     */
    const val NOT_MODIFIED: Int = ResourceStatus.SC_NOT_MODIFIED

    /**
     * Access denied - authorization required, but missing/incorrect.
     */
    const val UNAUTHORIZED: Int = ResourceStatus.SC_UNAUTHORIZED

    /**
     * 404 Not Found
     * The server can not find the requested resource. In the browser, this means the URL is not recognized.
     * In an API, this can also mean that the endpoint is valid but the resource itself does not exist.
     * Servers may also send this response instead of 403 to hide the existence of a resource from an unauthorized client.
     * This response code is probably the most famous one due to its frequent occurrence on the web.
     */
    const val NOT_FOUND: Int = ResourceStatus.SC_NOT_FOUND

    /**
     * The client has indicated preconditions in its headers which the server does not meet.
     */
    const val PRECONDITION_FAILED: Int = ResourceStatus.SC_PRECONDITION_FAILED

    /**
     * Find the target host timed out.
     */
    const val REQUEST_TIMEOUT: Int = ResourceStatus.SC_REQUEST_TIMEOUT

    /**
     * 410 Gone:
     * This response is sent when the requested content has been permanently deleted from server,
     * with no forwarding address. Clients are expected to remove their caches and links to the resource.
     * The HTTP specification intends this status code to be used for "limited-time, promotional services".
     * APIs should not feel compelled to indicate resources that have been deleted with this status code.
     */
    const val GONE: Int = ResourceStatus.SC_GONE

    //
    // The following codes are NOT compatible with HTTP status codes
    //
    /**
     * Code >= `INCOMPATIBLE_CODE_START` are NOT compatible with HTTP status codes
     */
    const val INCOMPATIBLE_CODE_START: Int = 1000

    /**
     * Failed to find the target host.
     */
    const val UNKNOWN_HOST: Int = 1460

    /**
     * Access denied by robots.txt rules. Or display a robot check page.
     */
    const val ROBOTS_DENIED: Int = 1461

    /**
     * Unspecified exception occurs. Further information may be provided in args.
     */
    const val EXCEPTION: Int = 1462

    /**
     * Too many redirects.
     */
    const val REDIR_EXCEEDED: Int = 1463

    /**
     * Request was refused by protocol plugins, because it would block. The
     * expected number of milliseconds to wait before retry may be provided in
     * args.
     */
    const val WOULD_BLOCK: Int = 1465

    /**
     * Thread was blocked http.max.delays times during fetching.
     */
    const val BLOCKED: Int = 1466
    // 147x: Timeout
    /**
     * The fetch thread is timeout.
     */
    const val THREAD_TIMEOUT: Int = 1470

    /**
     * Web driver timeout.
     */
    const val WEB_DRIVER_TIMEOUT: Int = 1471

    /**
     * Javascript execution timeout.
     */
    const val SCRIPT_TIMEOUT: Int = 1472

    /**
     * Web driver timeout.
     */
    const val PROXY_ERROR: Int = 1481

    /**
     * Web driver timeout.
     */
    const val WEB_DRIVER_GONE: Int = 1482

    /**
     * The browser reports an error, ERR_CONNECTION_TIMED_OUT, for example.
     */
    const val BROWSER_ERROR: Int = 1483

    /**
     * This protocol was not found. Application may attempt to retry later.
     */
    const val PROTO_NOT_FOUND: Int = 1600

    /**
     * Temporary failure. Application may retry immediately.
     */
    const val RETRY: Int = 1601

    /**
     * If the fetch task is canceled, the result will be discarded despite success or failed.
     */
    const val CANCELED: Int = 1602
}
