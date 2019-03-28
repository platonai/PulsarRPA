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
package ai.platon.pulsar.protocol.httpclient;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.common.HttpHeaders;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata;
import ai.platon.pulsar.crawl.protocol.Response;
import ai.platon.pulsar.crawl.protocol.http.AbstractHttpProtocol;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * An HTTP response.
 *
 * @author Susam Pal
 */
public class HttpResponse implements Response {

    private URL url;
    private byte[] content;
    private int code;
    private MultiMetadata headers = new SpellCheckedMultiMetadata();

    /**
     * Fetches the given <code>url</code> and prepares HTTP response.
     *
     * @param http            An instance of the implementation class of this plugin
     * @param url             URL to be fetched
     * @param page            WebPage
     * @param followRedirects Whether to follow redirects; follows redirect if and only if this
     *                        is true
     * @return HTTP response
     * @throws IOException When an error occurs
     */
    HttpResponse(Http http, URL url, WebPage page, boolean followRedirects) throws IOException {

        // Prepare GET method for HTTP request
        this.url = url;
        GetMethod get = new GetMethod(url.toString());
        get.setFollowRedirects(followRedirects);
        get.setDoAuthentication(true);
        if (page.getModifiedTime().getEpochSecond() > 0) {
            get.setRequestHeader("If-Modified-Since", DateTimeUtil.formatHttpDateTime(page.getModifiedTime()));
        }

        // Set HTTP parameters
        HttpMethodParams params = get.getParams();
        if (http.getUseHttp11()) {
            params.setVersion(HttpVersion.HTTP_1_1);
        } else {
            params.setVersion(HttpVersion.HTTP_1_0);
        }
        params.makeLenient();
        params.setContentCharset("UTF-8");
        params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        params.setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
        // XXX (ab) not sure about this... the default is to retry 3 times; if
        // XXX the request body was sent the method is not retried, so there is
        // XXX little danger in retrying...
        // params.setParameter(HttpMethodParams.RETRY_HANDLER, null);
        try {
            code = Http.getClient().executeMethod(get);

            Header[] heads = get.getResponseHeaders();

            for (Header head : heads) {
                headers.put(head.getName(), head.getValue());
            }

            // Limit download size
            int contentLength = Integer.MAX_VALUE;
            String contentLengthString = headers.get(HttpHeaders.CONTENT_LENGTH);
            if (contentLengthString != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthString.trim());
                } catch (NumberFormatException ex) {
                    throw new HttpException("bad content length: " + contentLengthString);
                }
            }
            if (http.getMaxContent() >= 0 && contentLength > http.getMaxContent()) {
                contentLength = http.getMaxContent();
            }

            // always read content. Sometimes content is useful to find a cause
            // for error.
            InputStream in = get.getResponseBodyAsStream();
            try {
                byte[] buffer = new byte[AbstractHttpProtocol.BUFFER_SIZE];
                int bufferFilled = 0;
                int totalRead = 0;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((bufferFilled = in.read(buffer, 0, buffer.length)) != -1
                        && totalRead + bufferFilled <= contentLength) {
                    totalRead += bufferFilled;
                    out.write(buffer, 0, bufferFilled);
                }

                content = out.toByteArray();
            } catch (Exception e) {
                if (code == 200)
                    throw new IOException(e.toString());
                // for codes other than 200 OK, we are fine with empty content
            } finally {
                if (in != null) {
                    in.close();
                }
                get.abort();
            }

            StringBuilder fetchTrace = null;
            if (Http.LOG.isTraceEnabled()) {
                // Trace message
                fetchTrace = new StringBuilder("url: " + url + "; status code: " + code
                        + "; bytes received: " + content.length);
                if (getHeader(HttpHeaders.CONTENT_LENGTH) != null)
                    fetchTrace.append("; Content-Length: "
                            + getHeader(HttpHeaders.CONTENT_LENGTH));
                if (getHeader(HttpHeaders.LOCATION) != null)
                    fetchTrace.append("; Location: " + getHeader(HttpHeaders.LOCATION));
            }
            // Extract gzip, x-gzip and deflate content
            if (content != null) {
                // check if we have to uncompress it
                String contentEncoding = headers.get(HttpHeaders.CONTENT_ENCODING);
                if (contentEncoding != null && Http.LOG.isTraceEnabled())
                    fetchTrace.append("; Content-Encoding: " + contentEncoding);
                if ("gzip".equals(contentEncoding) || "x-gzip".equals(contentEncoding)) {
                    content = http.processGzipEncoded(content, url);
                    if (Http.LOG.isTraceEnabled())
                        fetchTrace.append("; extracted to " + content.length + " bytes");
                } else if ("deflate".equals(contentEncoding)) {
                    content = http.processDeflateEncoded(content, url);
                    if (Http.LOG.isTraceEnabled())
                        fetchTrace.append("; extracted to " + content.length + " bytes");
                }
            }

            page.getHeaders().clear();
            for (String key : headers.names()) {
                page.getHeaders().put(key, headers.get(key));
            }

            // Logger trace message
            if (fetchTrace != null && Http.LOG.isTraceEnabled()) {
                Http.LOG.trace(fetchTrace.toString());
            }
        } finally {
            get.releaseConnection();
        }
    }

    public String getUrl() {
        return url.toString();
    }

    public int getCode() {
        return code;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public MultiMetadata getHeaders() {
        return headers;
    }

    public byte[] getContent() {
        return content;
    }
}
