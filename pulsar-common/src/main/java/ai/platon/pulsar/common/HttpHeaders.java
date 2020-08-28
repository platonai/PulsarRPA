/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.common;

/**
 * A collection of HTTP header names.
 *
 * TODO: use guava HttpHeaders
 *
 * @author vincent
 * @version $Id: $Id
 */
public interface HttpHeaders {
    /** Constant <code>TRANSFER_ENCODING="Transfer-Encoding"</code> */
    String TRANSFER_ENCODING = "Transfer-Encoding";
    /**
     * Content encoding from http header, it's a suggestion
     * but Q_TRUSTED_CONTENT_ENCODING can be trusted
     * */
    String CONTENT_ENCODING = "Content-Encoding";
    /** Constant <code>CONTENT_LANGUAGE="Content-Language"</code> */
    String CONTENT_LANGUAGE = "Content-Language";
    /** Constant <code>CONTENT_LENGTH="Content-Length"</code> */
    String CONTENT_LENGTH = "Content-Length";
    /** Constant <code>CONTENT_LOCATION="Content-Location"</code> */
    String CONTENT_LOCATION = "Content-Location";
    /** Constant <code>CONTENT_DISPOSITION="Content-Disposition"</code> */
    String CONTENT_DISPOSITION = "Content-Disposition";
    /** Constant <code>CONTENT_MD5="Content-MD5"</code> */
    String CONTENT_MD5 = "Content-MD5";
    /** Constant <code>CONTENT_TYPE="Content-Type"</code> */
    String CONTENT_TYPE = "Content-Type";
    /** Constant <code>LAST_MODIFIED="Last-Modified"</code> */
    String LAST_MODIFIED = "Last-Modified";
    /** Constant <code>LOCATION="Location"</code> */
    String LOCATION = "Location";

    // Internal usage
    /** Constant <code>Q_TRUSTED_CONTENT_ENCODING="Q-Trusted-Content-Encoding"</code> */
    String Q_TRUSTED_CONTENT_ENCODING = "Q-Trusted-Content-Encoding";
    /** Constant <code>Q_VERSION="Q-Version"</code> */
    String Q_VERSION = "Q-Version";
    /** Constant <code>Q_USERNAME="Q-Username"</code> */
    String Q_USERNAME = "Q-Username";
    /** Constant <code>Q_PASSWORD="Q-Password"</code> */
    String Q_PASSWORD = "Q-Password";
    /** Constant <code>Q_JOB_ID="Q-Job-Id"</code> */
    String Q_JOB_ID = "Q-Job-Id";
    /** Constant <code>Q_PRIORITY="Q-Priority"</code> */
    String Q_PRIORITY = "Q-Priority";
    /** Constant <code>Q_QUEUE_ID="Q-Queue-Id"</code> */
    String Q_QUEUE_ID = "Q-Queue-Id";
    /** Constant <code>Q_ITEM_ID="Q-Item-Id"</code> */
    String Q_ITEM_ID = "Q-Item-Id";
    /** Constant <code>Q_REQUEST_TIME="Q-Request-Time"</code> */
    String Q_REQUEST_TIME = "Q-Request-Time"; // should be millis from epoch, e.g. System.currentTimeMillis()
    /** Constant <code>Q_RESPONSE_TIME="Q-Response-Time"</code> */
    String Q_RESPONSE_TIME = "Q-Response-Time"; // should be millis from epoch, e.g. System.currentTimeMillis()
    /** Constant <code>Q_STATUS_CODE="Q-Status-Code"</code> */
    String Q_STATUS_CODE = "Q-Status-Code";
    /** Constant <code>Q_CHECKSUM="Q-Checksum"</code> */
    String Q_CHECKSUM = "Q-Checksum";
    /** Constant <code>Q_URL="Q-Url"</code> */
    String Q_URL = "Q-Url";
}
