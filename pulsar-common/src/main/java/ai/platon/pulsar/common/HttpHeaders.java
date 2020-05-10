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
 */
public interface HttpHeaders {
    String TRANSFER_ENCODING = "Transfer-Encoding";
    /**
     * Content encoding from http header, it's a suggestion
     * but Q_TRUSTED_CONTENT_ENCODING can be trusted
     * */
    String CONTENT_ENCODING = "Content-Encoding";
    String CONTENT_LANGUAGE = "Content-Language";
    String CONTENT_LENGTH = "Content-Length";
    String CONTENT_LOCATION = "Content-Location";
    String CONTENT_DISPOSITION = "Content-Disposition";
    String CONTENT_MD5 = "Content-MD5";
    String CONTENT_TYPE = "Content-Type";
    String LAST_MODIFIED = "Last-Modified";
    String LOCATION = "Location";

    // Internal usage
    String Q_TRUSTED_CONTENT_ENCODING = "Q-Trusted-Content-Encoding";
    String Q_VERSION = "Q-Version";
    String Q_USERNAME = "Q-Username";
    String Q_PASSWORD = "Q-Password";
    String Q_JOB_ID = "Q-Job-Id";
    String Q_PRIORITY = "Q-Priority";
    String Q_QUEUE_ID = "Q-Queue-Id";
    String Q_ITEM_ID = "Q-Item-Id";
    String Q_REQUEST_TIME = "Q-Request-Time"; // should be millis from epoch, e.g. System.currentTimeMillis()
    String Q_RESPONSE_TIME = "Q-Response-Time"; // should be millis from epoch, e.g. System.currentTimeMillis()
    String Q_STATUS_CODE = "Q-Status-Code";
    String Q_CHECKSUM = "Q-Checksum";
    String Q_URL = "Q-Url";
}
