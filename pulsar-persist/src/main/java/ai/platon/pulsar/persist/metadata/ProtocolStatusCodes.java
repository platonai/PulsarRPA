/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package ai.platon.pulsar.persist.metadata;

import ai.platon.pulsar.common.ResourceStatus;

/**
 *
 * @link {https://en.wikipedia.org/wiki/List_of_HTTP_status_codes}
 * @link {https://developer.mozilla.org/en-US/docs/Web/HTTP/Status}
 * @link {http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpStatus.html}
 * @author vincent
 * @version $Id: $Id
 */
public interface ProtocolStatusCodes {

    //
    // The following codes are compatible with HTTP status codes, close but may not equals to HTTP status code
    // @see {https://en.wikipedia.org/wiki/List_of_HTTP_status_codes}
    //

    /**
     * Resource is OK
     * */
    int SUCCESS_OK = ResourceStatus.SC_OK;
    /**
     * Resource is created
     * */
    int CREATED = ResourceStatus.SC_CREATED;
    /**
     * Resource has moved permanently. New url should be found in args.
     */
    int MOVED_PERMANENTLY = ResourceStatus.SC_MOVED_PERMANENTLY;
    /**
     * Resource has moved temporarily. New url should be found in args.
     */
    int MOVED_TEMPORARILY = ResourceStatus.SC_MOVED_TEMPORARILY;
    /**
     * Unchanged since the last fetch.
     */
    int NOT_MODIFIED = ResourceStatus.SC_NOT_MODIFIED;

    /**
     * Access denied - authorization required, but missing/incorrect.
     */
    int UNAUTHORIZED = ResourceStatus.SC_UNAUTHORIZED;
    /**
     * 404 Not Found
     * The server can not find the requested resource. In the browser, this means the URL is not recognized.
     * In an API, this can also mean that the endpoint is valid but the resource itself does not exist.
     * Servers may also send this response instead of 403 to hide the existence of a resource from an unauthorized client.
     * This response code is probably the most famous one due to its frequent occurrence on the web.
     */
    int NOT_FOUND = ResourceStatus.SC_NOT_FOUND;
    /**
     * The client has indicated preconditions in its headers which the server does not meet.
     * */
    int PRECONDITION_FAILED = ResourceStatus.SC_PRECONDITION_FAILED;
    /**
     * Find the target host timed out.
     */
    int REQUEST_TIMEOUT = ResourceStatus.SC_REQUEST_TIMEOUT;
    /**
     * 410 Gone:
     * This response is sent when the requested content has been permanently deleted from server,
     * with no forwarding address. Clients are expected to remove their caches and links to the resource.
     * The HTTP specification intends this status code to be used for "limited-time, promotional services".
     * APIs should not feel compelled to indicate resources that have been deleted with this status code.
     */
    int GONE = ResourceStatus.SC_GONE;

    //
    // The following codes are NOT compatible with HTTP status codes
    //

    /**
     * Code >= {@code INCOMPATIBLE_CODE_START} are NOT compatible with HTTP status codes
     */
    int INCOMPATIBLE_CODE_START = 1000;

    /**
     * Failed to find the target host.
     */
    int UNKNOWN_HOST = 1460;
    /**
     * Access denied by robots.txt rules. Or display a robot check page.
     */
    int ROBOTS_DENIED = 1461;
    /**
     * Unspecified exception occurs. Further information may be provided in args.
     */
    int EXCEPTION = 1462;
    /**
     * Too many redirects.
     */
    int REDIR_EXCEEDED = 1463;
    /**
     * Request was refused by protocol plugins, because it would block. The
     * expected number of milliseconds to wait before retry may be provided in
     * args.
     */
    int WOULD_BLOCK = 1465;
    /**
     * Thread was blocked http.max.delays times during fetching.
     */
    int BLOCKED = 1466;
    // 147x: Timeout
    /**
     * The fetch thread is timeout.
     */
    int THREAD_TIMEOUT = 1470;
    /**
     * Web driver timeout.
     */
    int WEB_DRIVER_TIMEOUT = 1471;
    /**
     * Javascript execution timeout.
     */
    int SCRIPT_TIMEOUT = 1472;
    /**
     * Document incomplete
     * @deprecated
     */
    int DOCUMENT_INCOMPLETE = 1473;
    /**
     * Web driver timeout.
     * @deprecated
     */
    int BROWSER_ERR_CONNECTION_TIMED_OUT = 1480;
    /**
     * Web driver timeout.
     */
    int PROXY_ERROR = 1481;
    /**
     * Web driver timeout.
     */
    int WEB_DRIVER_GONE = 1482;
    /**
     * The browser reports an error, ERR_CONNECTION_TIMED_OUT, for example.
     */
    int BROWSER_ERROR = 1483;
    /**
     * This protocol was not found. Application may attempt to retry later.
     */
    int PROTO_NOT_FOUND = 1600;
    /**
     * Temporary failure. Application may retry immediately.
     */
    int RETRY = 1601;
    /**
     * The fetch task is canceled, the result is discarded despite success or error
     */
    int CANCELED = 1602;
}
