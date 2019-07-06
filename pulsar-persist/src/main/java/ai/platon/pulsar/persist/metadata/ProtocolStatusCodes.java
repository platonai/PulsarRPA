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

/**
 * See ai.platon.pulsar.crawl.protocol.http.AbstractHttpProtocol#getProtocolOutput for more information
 * @link {https://en.wikipedia.org/wiki/List_of_HTTP_status_codes}
 * @link {http://hc.apache.org/httpcomponents-core-ga/httpcore/apidocs/org/apache/http/HttpStatus.html}
 * */
public interface ProtocolStatusCodes {

    //
    // The following codes are compatible with HTTP status codes, close but may not equals to HTTP status code
    // @see {https://en.wikipedia.org/wiki/List_of_HTTP_status_codes}
    //

    int SUCCESS_OK = 200;
    /**
     * Resource has moved permanently. New url should be found in args.
     */
    int MOVED = 300;
    /**
     * Unchanged since the last fetch.
     */
    int NOTMODIFIED = 304;
    /**
     * Resource has moved temporarily. New url should be found in args.
     */
    int TEMP_MOVED = 307;

    /**
     * Access denied - authorization required, but missing/incorrect.
     */
    int ACCESS_DENIED = 401;
    /**
     * Resource was not found.
     */
    int NOTFOUND = 404;
    /**
     * Find the target host timed out.
     */
    int REQUEST_TIMEOUT = 408;
    /**
     * Resource is gone.
     */
    int GONE = 410;

    //
    // The following codes are NOT compatible with HTTP status codes
    //
    /**
     * Failed to find the target host.
     */
    int UNKNOWN_HOST = 1460;
    /**
     * Access denied by robots.txt rules.
     */
    int ROBOTS_DENIED = 1461;
    /**
     * Unspecified exception occured. Further information may be provided in args.
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
    int WOULDBLOCK = 1465;
    /**
     * Thread was blocked http.max.delays times during fetching.
     */
    int BLOCKED = 1466;
    /**
     * The fetch thread is timeout.
     */
    int THREAD_TIMEOUT = 1467;
    /**
     * Selenium web driver is timeout.
     */
    int WEB_DRIVER_TIMEOUT = 1468;
    /**
     * Selenium web driver is timeout.
     */
    int DOCUMENT_READY_TIMEOUT = 1469;
    /**
     * This protocol was not found. Application may attempt to retry later.
     */
    int PROTO_NOT_FOUND = 1600;
    /**
     * Temporary failure. Application may retry immediately.
     */
    int RETRY = 1601;
    /**
     * The fetch task is canceled by the client
     */
    int CANCELED = 1602;
}
