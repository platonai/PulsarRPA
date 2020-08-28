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
 * <p>ParseStatusCodes interface.</p>
 *
 * @author vincent
 * @version $Id: $Id
 */
public interface ParseStatusCodes {

    // Primary status codes:

    /**
     * Parsing was not performed.
     */
    short NOTPARSED = 0;
    /**
     * Parsing succeeded.
     */
    short SUCCESS = 1;
    /**
     * General failure. There may be a more specific error message in arguments.
     */
    short FAILED = 2;

    // Use different type for primary codes and secondary codes to make less errors
    // Secondary success codes:
    /** Constant <code>SUCCESS_OK=0</code> */
    int SUCCESS_OK = 0;

    /** Constant <code>SUCCESS_IGNORE=1</code> */
    int SUCCESS_IGNORE = 1;

    /** Constant <code>SUCCESS_EXT=2</code> */
    int SUCCESS_EXT = 2;

    /**
     * Parsed content contains a directive to redirect to another URL. The target
     * URL can be retrieved from the arguments.
     */
    int SUCCESS_REDIRECT = 100;
    /**
     * Parsing success. The page is not allowed to index
     */
    int SUCCESS_NO_INDEX = 101;

    // Secondary failure codes go here:

    /**
     * Parsing failed. An Exception occured (which may be retrieved from the
     * arguments).
     */
    int FAILED_EXCEPTION = 200;
    /**
     * Parsing failed. The reason is unknown or not specified.
     */
    int FAILED_NOT_SPECIFIED = 201;
    /**
     * Parsing failed. Content was truncated, but the parser cannot handle
     * incomplete content.
     */
    int FAILED_TRUNCATED = 202;
    /**
     * Parsing failed. Invalid format - the content may be corrupted or of wrong
     * type.
     */
    int FAILED_INVALID_FORMAT = 203;
    /**
     * Parsing failed. Other related parts of the content are needed to halt
     * parsing. The list of URLs to missing parts may be provided in arguments.
     * The SimpleFetcher may decide to fetch these parts at once, then put them into
     * Content.metadata, and supply them for re-parsing.
     */
    int FAILED_MISSING_PARTS = 204;
    /**
     * Parsing failed. There was no content to be parsed - probably caused by
     * errors at protocol stage.
     */
    int FAILED_MISSING_CONTENT = 205;
    /**
     * Parsing failed. No responsible parser found
     */
    int FAILED_NO_PARSER = 206;
    /**
     * Parsing failed. No responsible parser found
     */
    int FAILED_MALFORMED_URL = 207;
    /**
     * Parsing failed. No responsible parser found
     */
    int FAILED_UNKNOWN_ENCODING = 208;
}
