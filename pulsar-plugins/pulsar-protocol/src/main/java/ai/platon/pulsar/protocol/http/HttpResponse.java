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
package ai.platon.pulsar.protocol.http;

import ai.platon.pulsar.common.DateTimeUtil;
import ai.platon.pulsar.common.HttpHeaders;
import ai.platon.pulsar.common.config.ImmutableConfig;
import ai.platon.pulsar.common.proxy.NoProxyException;
import ai.platon.pulsar.common.proxy.ProxyEntry;
import ai.platon.pulsar.crawl.protocol.Protocol;
import ai.platon.pulsar.crawl.protocol.ProtocolException;
import ai.platon.pulsar.crawl.protocol.Response;
import ai.platon.pulsar.crawl.protocol.http.AbstractHttpProtocol;
import ai.platon.pulsar.crawl.protocol.http.HttpException;
import ai.platon.pulsar.persist.ProtocolStatus;
import ai.platon.pulsar.persist.WebPage;
import ai.platon.pulsar.persist.metadata.MultiMetadata;
import ai.platon.pulsar.persist.metadata.SpellCheckedMultiMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HttpResponse implements Response {
    private Logger log = LoggerFactory.getLogger(HttpResponse.class);

    private final AbstractHttpProtocol http;
    private final URL url;
    private final MultiMetadata headers = new SpellCheckedMultiMetadata();
    private ImmutableConfig conf;
    private ProxyEntry proxy = null;
    private byte[] content;
    private int code;

    public HttpResponse(AbstractHttpProtocol http, URL url, WebPage page) throws ProtocolException, IOException, NoProxyException {
        this.http = http;
        this.url = url;

        Scheme scheme;

        if ("http".equals(url.getProtocol())) {
            scheme = Scheme.HTTP;
        } else if ("https".equals(url.getProtocol())) {
            scheme = Scheme.HTTPS;
        } else {
            throw new HttpException("Unknown scheme (not http/https) for url:" + url);
        }

        if (Protocol.log.isTraceEnabled()) {
            Protocol.log.trace("fetching " + url);
        }

        String path = "".equals(url.getFile()) ? "/" : url.getFile();

        // some servers will redirect a request with a host line like
        // "Host: <hostname>:80" to "http://<hpstname>/<orig_path>"- they
        // don't want the :80...

        String host = url.getHost();
        int port;
        String portString;
        if (url.getPort() == -1) {
            if (scheme == Scheme.HTTP) {
                port = 80;
            } else {
                port = 443;
            }
            portString = "";
        } else {
            port = url.getPort();
            portString = ":" + port;
        }
        Socket socket = null;
        boolean fetchSuccess = false;

        try {
            socket = new Socket(); // create the socket
            socket.setSoTimeout((int) http.getTimeout().toMillis());

            // connect
            String sockHost = http.useProxy() ? http.getProxyHost() : host;
            int sockPort = http.useProxy() ? http.getProxyPort() : port;
            if (http.useProxyPool()) {
                proxy = http.proxyPool().poll();
                if (proxy == null) {
                    throw new NoProxyException("proxy pool exhausted");
                }

                sockHost = proxy.getHost();
                sockPort = proxy.getPort();

                if (LOG.isDebugEnabled()) {
                    LOG.debug(proxy.toString());
                }
            }

            InetSocketAddress sockAddr = new InetSocketAddress(sockHost, sockPort);
            // API notes : Connects this socket to the server with a specified timeout
            // value.
            // A timeout of zero is interpreted as an infinite timeout.
            // The connection will then block until established or an error occurs.
            // Throws : SocketTimeoutException - if timeout expires before connecting
            // And others, see the official document for more
            socket.connect(sockAddr, (int) http.getTimeout().toMillis());

            if (scheme == Scheme.HTTPS) {
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslsocket = (SSLSocket) factory.createSocket(socket, sockHost, sockPort, true);
                sslsocket.setUseClientMode(true);

                // Get the protocols and ciphers supported by this JVM
                Set<String> protocols = new HashSet<>(Arrays.asList(sslsocket.getSupportedProtocols()));
                Set<String> ciphers = new HashSet<>(Arrays.asList(sslsocket.getSupportedCipherSuites()));

                // Intersect with preferred protocols and ciphers
                protocols.retainAll(http.getTlsPreferredProtocols());
                ciphers.retainAll(http.getTlsPreferredCipherSuites());

                sslsocket.setEnabledProtocols(protocols.toArray(new String[protocols.size()]));
                sslsocket.setEnabledCipherSuites(ciphers.toArray(new String[ciphers.size()]));

                sslsocket.startHandshake();
                socket = sslsocket;
            }

            conf = http.getConf();
            if (conf.getBoolean("store.ip.address", false)) {
                page.getMetadata().set("_ip_", sockAddr.getAddress().getHostAddress());
            }

            // make request
            OutputStream req = socket.getOutputStream();

            StringBuilder reqStr = new StringBuilder("GET ");
            if (http.useProxy()) {
                reqStr.append(url.getProtocol() + "://" + host + portString + path);
            } else {
                reqStr.append(path);
            }

            reqStr.append(" HTTP/1.0\r\n");

            reqStr.append("Host: ");
            reqStr.append(host);
            reqStr.append(portString);
            reqStr.append("\r\n");

            reqStr.append("Accept-Encoding: x-gzip, gzip\r\n");

            reqStr.append("Accept: ");
            reqStr.append(this.http.getAccept());
            reqStr.append("\r\n");

            String userAgent = http.getUserAgent();
            if (userAgent == null || userAgent.isEmpty()) {
                LOG.warn("User-agent is not set!");
            } else {
                reqStr.append("User-Agent: ").append(userAgent).append("\r\n");
            }

            reqStr.append("If-Modified-Since: ")
                    .append(DateTimeUtil.formatHttpDateTime(page.getModifiedTime()))
                    .append("\r\n").append("\r\n");

            byte[] reqBytes = reqStr.toString().getBytes();

            req.write(reqBytes);
            req.flush();

            // blocking for reading the response

            // IOException - if an I/O error occurs when creating the input stream,
            // the socket is closed,
            // the socket is not connected, or the socket input has been shutdown
            // using shutdownInput()

            PushbackInputStream in =
                    new PushbackInputStream(new BufferedInputStream(socket.getInputStream(), Http.BUFFER_SIZE), Http.BUFFER_SIZE);

            StringBuffer line = new StringBuffer();

            boolean haveSeenNonContinueStatus = false;
            while (!haveSeenNonContinueStatus) {
                // parse status code line
                this.code = parseStatusLine(in, line);
                // parse headers
                parseHeaders(in, line);
                haveSeenNonContinueStatus = code != 100; // 100 is "Continue"
            }

            String transferEncoding = getHeader(HttpHeaders.TRANSFER_ENCODING);
            if (transferEncoding != null && "chunked".equalsIgnoreCase(transferEncoding.trim())) {
                readChunkedContent(in, line);
            } else {
                readPlainContent(in);
            }

            String contentEncoding = getHeader(HttpHeaders.CONTENT_ENCODING);
            if ("gzip".equals(contentEncoding) || "x-gzip".equals(contentEncoding)) {
                content = http.processGzipEncoded(content, url);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("fetched " + content.length + " bytes from " + url);
                }
            }

            page.getHeaders().clear();
            for (String key : headers.names()) {
                page.getHeaders().put(key, headers.get(key));
            }

            fetchSuccess = true;
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (http.useProxyPool() && proxy != null) {
                // put back the proxy resource, this is essential important!
                if (fetchSuccess) {
                    log.debug("put back proxy {}", proxy);
                    http.proxyPool().offer(proxy);
                } else {
                    log.debug("retire proxy {}", proxy);
                    http.proxyPool().retire(proxy);
                }
            }
        }
    }

    public String getUrl() {
        return url.toString();
    }

    @Override
    public ProtocolStatus getStatus() {
        System.out.println("Should check the implementation, it always return STATUS_NOTFETCHED");
        return ProtocolStatus.STATUS_NOTFETCHED;
    }

    @Override
    public int getCode() {
        return code;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public MultiMetadata getHeaders() {
        return headers;
    }

    @Nullable
    public byte[] getContent() {
        return content;
    }

    private void readPlainContent(InputStream in) throws HttpException, IOException {
        int contentLength = Integer.MAX_VALUE; // get content length
        String contentLengthString = headers.get(HttpHeaders.CONTENT_LENGTH);
        if (contentLengthString != null) {
            contentLengthString = contentLengthString.trim();
            try {
                if (!contentLengthString.isEmpty())
                    contentLength = Integer.parseInt(contentLengthString);
            } catch (NumberFormatException e) {
                throw new HttpException("bad content length: " + contentLengthString);
            }
        }
        if (http.getMaxContent() >= 0 && contentLength > http.getMaxContent()) {
            // limit download size
            contentLength = http.getMaxContent();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(Http.BUFFER_SIZE);
        byte[] bytes = new byte[Http.BUFFER_SIZE];
        int length = 0;
        // read content
        int i = in.read(bytes);
        while (i != -1) {
            out.write(bytes, 0, i);
            length += i;
            if (length >= contentLength) {
                break;
            }
            if ((length + Http.BUFFER_SIZE) > contentLength) {
                // reading next chunk may hit contentLength,
                // must limit number of bytes read
                i = in.read(bytes, 0, (contentLength - length));
            } else {
                i = in.read(bytes);
            }
        }
        content = out.toByteArray();
    }

    /**
     * @param in
     * @param line
     * @throws HttpException
     * @throws IOException
     */
    private void readChunkedContent(PushbackInputStream in, StringBuffer line) throws HttpException, IOException {
        boolean doneChunks = false;
        int contentBytesRead = 0;
        byte[] bytes = new byte[Http.BUFFER_SIZE];
        ByteArrayOutputStream out = new ByteArrayOutputStream(Http.BUFFER_SIZE);

        while (!doneChunks) {
            if (log.isTraceEnabled()) {
                log.trace("Http: starting chunk");
            }

            readLine(in, line, false);

            String chunkLenStr;
            // if (log.isTraceEnabled()) { log.trace("chunk-header: '" + line + "'");
            // }

            int pos = line.indexOf(";");
            if (pos < 0) {
                chunkLenStr = line.toString();
            } else {
                chunkLenStr = line.substring(0, pos);
                if (log.isTraceEnabled()) {
                    log.trace("got chunk-ext: " + line.substring(pos + 1));
                }
            }
            chunkLenStr = chunkLenStr.trim();
            int chunkLen;
            try {
                chunkLen = Integer.parseInt(chunkLenStr, 16);
            } catch (NumberFormatException e) {
                throw new HttpException("bad chunk length: " + line.toString());
            }

            if (chunkLen == 0) {
                doneChunks = true;
                break;
            }

            final int maxContent = http.getMaxContent();
            if (maxContent >= 0 && (contentBytesRead + chunkLen) > maxContent) {
                chunkLen = maxContent - contentBytesRead;
            }

            // read one chunk
            int chunkBytesRead = 0;
            while (chunkBytesRead < chunkLen) {
                int toRead = (chunkLen - chunkBytesRead) < Http.BUFFER_SIZE ? (chunkLen - chunkBytesRead) : Http.BUFFER_SIZE;
                int len = in.read(bytes, 0, toRead);

                if (len == -1) {
                    throw new HttpException("chunk eof after " + contentBytesRead
                            + " bytes in successful chunks" + " and " + chunkBytesRead
                            + " in current chunk");
                }

                // DANGER!!! Will printed GZIPed stuff right to your
                // terminal!
                // if (log.isTraceEnabled()) { log.trace("read: " + new String(bytes, 0,
                // len)); }

                out.write(bytes, 0, len);
                chunkBytesRead += len;
            }

            readLine(in, line, false);
        }

        if (!doneChunks) {
            if (contentBytesRead != http.getMaxContent()) {
                throw new HttpException("chunk eof: !doneChunk && didn't max out");
            }
            return;
        }

        content = out.toByteArray();
        parseHeaders(in, line);
    }

    private int parseStatusLine(PushbackInputStream in, StringBuffer line) throws IOException, HttpException {
        readLine(in, line, false);

        int codeStart = line.indexOf(" ");
        int codeEnd = line.indexOf(" ", codeStart + 1);

        // handle lines with no plaintext result code, ie:
        // "HTTP/1.1 200" vs "HTTP/1.1 200 OK"
        if (codeEnd == -1) {
            codeEnd = line.length();
        }

        int code;
        try {
            code = Integer.parseInt(line.substring(codeStart + 1, codeEnd));
        } catch (NumberFormatException e) {
            throw new HttpException("bad status line '" + line + "': " + e.getMessage(), e);
        }

        return code;
    }

    private void processHeaderLine(StringBuffer line) throws IOException, HttpException {
        int colonIndex = line.indexOf(":"); // key is up to colon
        if (colonIndex == -1) {
            int i;
            for (i = 0; i < line.length(); i++)
                if (!Character.isWhitespace(line.charAt(i)))
                    break;

            if (i == line.length())
                return;

            throw new HttpException("No colon in header:" + line);
        }

        String key = line.substring(0, colonIndex);

        int valueStart = colonIndex + 1; // skip whitespace
        while (valueStart < line.length()) {
            int c = line.charAt(valueStart);
            if (c != ' ' && c != '\t')
                break;
            valueStart++;
        }
        String value = line.substring(valueStart);
        headers.put(key, value);
    }

    // Adds headers to our headers Metadata
    private void parseHeaders(PushbackInputStream in, StringBuffer line) throws IOException, HttpException {
        while (readLine(in, line, true) != 0) {

            // handle HTTP responses with missing blank line after headers
            int pos;
            if (((pos = line.indexOf("<!DOCTYPE")) != -1)
                    || ((pos = line.indexOf("<HTML")) != -1)
                    || ((pos = line.indexOf("<html")) != -1)) {

                in.unread(line.substring(pos).getBytes("UTF-8"));
                line.setLength(pos);

                try {
                    // TODO: (CM) We don't know the header names here
                    // since we're just handling them generically. It would
                    // be nice to provide some sort of mapping function here
                    // for the returned header names to the standard metadata
                    // names in the ParseData class
                    processHeaderLine(line);
                } catch (Exception e) {
                    // fixme:
                    log.error("Failed with the following exception: ", e);
                }
                return;
            }

            processHeaderLine(line);
        }
    }

    private static int readLine(PushbackInputStream in, StringBuffer line,
                                boolean allowContinuedLine) throws IOException {
        line.setLength(0);
        for (int c = in.read(); c != -1; c = in.read()) {
            switch (c) {
                case '\r':
                    if (peek(in) == '\n') {
                        in.read();
                    }
                case '\n':
                    if (line.length() > 0) {
                        // at EOL -- check for continued line if the current
                        // (possibly continued) line wasn't blank
                        if (allowContinuedLine)
                            switch (peek(in)) {
                                case ' ':
                                case '\t': // line is continued
                                    in.read();
                                    continue;
                            }
                    }
                    return line.length(); // else halt
                default:
                    line.append((char) c);
            }
        }
        throw new EOFException();
    }

    private static int peek(PushbackInputStream in) throws IOException {
        int value = in.read();
        in.unread(value);
        return value;
    }

    protected enum Scheme {
        HTTP, HTTPS,
    }
}
