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

package org.warps.pulsar.protocol.file;

import org.warps.pulsar.common.DateTimeUtil;
import org.warps.pulsar.common.HttpHeaders;
import org.warps.pulsar.common.MimeUtil;
import org.warps.pulsar.common.config.ImmutableConfig;
import org.warps.pulsar.crawl.protocol.Content;
import org.warps.pulsar.persist.WebPage;
import org.warps.pulsar.persist.metadata.MultiMetadata;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

import static org.warps.pulsar.common.config.CapabilityTypes.FETCH_PROTOCOL_SHARED_FILE_TIMEOUT;

/************************************
 * FileResponse.java mimics file replies as http response. It tries its best to
 * follow http's way for headers, response codes as well as exceptions.
 *
 * Comments: (1) java.net.URL and java.net.URLConnection can handle file:
 * scheme. However they are not flexible enough, so not used in this
 * implementation.
 *
 * (2) java.io.File is used for its abstractness across platforms. Warning:
 * java.io.File API (1.4.2) does not elaborate on how special files, such as
 * /dev/* in unix and /proc/* on linux, are treated. Tests show (a)
 * java.io.File.isFile() return false for /dev/* (b) java.io.File.isFile()
 * return true for /proc/* (c) java.io.File.length() return 0 for /proc/* We are
 * probably oaky for now. Could be buggy here. How about special files on
 * windows?
 *
 * (3) java.io.File API (1.4.2) does not seem to know unix hard link files. They
 * are just treated as individual files.
 *
 * (4) No funcy POSIX file attributes yet. May never need?
 *
 * @author John Xing
 ***********************************/
public class FileResponse {
    private static final byte[] EMPTY_CONTENT = new byte[0];
    private final File file;
    private ImmutableConfig conf;
    private String orig;
    private String base;
    private Duration timeout = Duration.ZERO;
    private byte[] content;
    private int code;
    private MultiMetadata headers = new MultiMetadata();
    private MimeUtil MIME;

    public FileResponse(URL uri, WebPage page, File file, ImmutableConfig conf) throws FileException, IOException {
        this.conf = conf;

        this.orig = uri.toString();
        this.base = uri.toString();
        this.file = file;

        MIME = new MimeUtil(conf);

        String protocolName = uri.getProtocol();

        boolean isSharedFileProtocol = "sharedFile".equals(protocolName);
        if (isSharedFileProtocol) {
            timeout = conf.getDuration(FETCH_PROTOCOL_SHARED_FILE_TIMEOUT, Duration.ofSeconds(10));
        }

        if (!isSharedFileProtocol && !"file".equals(protocolName)) {
            throw new FileException("Not a (shared) file uri:" + uri);
        }

        read(uri, page);
    }

    /** Returns the response code. */
    public int getCode() {
        return code;
    }

    /** Returns the value of a named header. */
    public String getHeader(String name) {
        return headers.get(name);
    }

    public byte[] getContent() {
        return content;
    }

    public Content toContent() {
        return new Content(orig, base, (content != null ? content : EMPTY_CONTENT),
                getHeader(HttpHeaders.CONTENT_TYPE), headers, this.conf);
    }

    private void read(URL uri, WebPage page) throws FileException, IOException {
        if (File.LOG.isTraceEnabled()) {
            File.LOG.trace("reading " + uri);
        }

        if (!uri.getPath().equals(uri.getFile())) {
            if (File.LOG.isWarnEnabled()) {
                File.LOG.warn("uri.getPath() != uri.getFile(): " + uri);
            }
        }

        String path = uri.getPath().isEmpty() ? "/" : uri.getPath();

        try {
            // specify the encoding via the config later?
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }

        // uri.toURI() is only in j2se 1.5.0
        // java.io.File f = new java.io.File(uri.toURI());
        java.io.File f = new java.io.File(path);

        boolean exists = f.exists();
        if (!exists) {
            if (timeout.isZero()) {
                this.code = 404; // http Not Found
                return;
            }

            // Shared file protocol, wait for other program to write the content, until timeout
            Instant start = Instant.now();
            Instant deadline = start.plus(timeout);
            Instant time = start;

            while (!exists && time.isBefore(deadline)) {
                try {
                    Thread.sleep(1000);
                    time = Instant.now();
                } catch (InterruptedException ignored) {
                }

                exists = f.exists();
            }

            if (!exists) {
                this.code = 404; // http Not Found
                return;
            }
        }

        if (!f.canRead()) {
            this.code = 401; // http Unauthorized
            return;
        }

        // symbolic link or relative path on unix
        // fix me: what's the consequence on windows platform
        // where case is insensitive
        if (!f.equals(f.getCanonicalFile())) {
            // set headers
            // hdrs.put("Location", f.getCanonicalFile().toURI());
            headers.put(HttpHeaders.LOCATION, f.getCanonicalFile().toURI().toURL().toString());

            this.code = 300; // http redirect
            return;
        }

        if (f.lastModified() <= page.getModifiedTime().toEpochMilli()) {
            this.code = 304;
            this.headers.put("Last-Modified", DateTimeUtil.format(f.lastModified()));
            return;
        }

        if (f.isDirectory()) {
            getDirAsHttpResponse(f);
        } else if (f.isFile()) {
            getFileAsHttpResponse(f);
        } else {
            this.code = 500; // http Internal Server Error
        }
    }

    // get file as http response
    private void getFileAsHttpResponse(java.io.File f) throws FileException, IOException {
        // ignore file of size larger than
        // Integer.MAX_VALUE = 2^31-1 = 2147483647
        long size = f.length();
        if (size > Integer.MAX_VALUE) {
            throw new FileException("file is too large, size: " + size);
            // or we can do this?
            // this.code = 400; // http Bad request
            // return;
        }

        // capture content
        int len = (int) size;

        if (this.file.maxContentLength >= 0 && len > this.file.maxContentLength)
            len = this.file.maxContentLength;

        this.content = new byte[len];

        java.io.InputStream is = new java.io.FileInputStream(f);
        int offset = 0;
        int n = 0;
        while (offset < len
                && (n = is.read(this.content, offset, len - offset)) >= 0) {
            offset += n;
        }
        if (offset < len) { // keep whatever already have, but issue a warning
            if (File.LOG.isWarnEnabled()) {
                File.LOG.warn("not enough bytes read from file: " + f.getPath());
            }
        }
        is.close();

        // set headers
        headers.put(HttpHeaders.CONTENT_LENGTH, Long.toString(size));
        this.headers.put("Last-Modified", DateTimeUtil.isoInstantFormat(f.lastModified()));
        String mimeType = MIME.getMimeType(f);
        String mimeTypeString = mimeType != null ? mimeType : "";
        headers.put(HttpHeaders.CONTENT_TYPE, mimeTypeString);

        // response code
        this.code = 200; // http OK
    }

    // get dir list as http response
    private void getDirAsHttpResponse(java.io.File f) throws IOException {

        String path = f.toString();
        if (this.file.crawlParents)
            this.content = list2html(f.listFiles(), path, !"/".equals(path));
        else
            this.content = list2html(f.listFiles(), path, false);

        // set headers
        headers.put(HttpHeaders.CONTENT_LENGTH, Integer.toString(this.content.length));
        headers.put(HttpHeaders.CONTENT_TYPE, "text/html");
        headers.put("Last-Modified", DateTimeUtil.isoInstantFormat(f.lastModified()));

        // response code
        this.code = 200; // http OK
    }

    // generate html page from dir list
    private byte[] list2html(java.io.File[] list, String path,
                             boolean includeDotDot) {

        StringBuffer x = new StringBuffer("<html><head>");
        x.append("<title>Index of " + path + "</title></head>\n");
        x.append("<body><h1>Index of " + path + "</h1><pre>\n");

        if (includeDotDot) {
            x.append("<a href='../'>../</a>\t-\t-\t-\n");
        }

        // fix me: we might want to sort list here! but not now.

        java.io.File f;
        for (int i = 0; i < list.length; i++) {
            f = list[i];
            String name = f.getName();
            String time = DateTimeUtil.isoInstantFormat(f.lastModified());
            if (f.isDirectory()) {
                // java 1.4.2 service says dir itself and parent dir are not listed
                // so the following is not needed.
                // if (name.equals(".") || name.equals(".."))
                // continue;
                x.append("<a href='" + name + "/" + "'>" + name + "/</a>\t");
                x.append(time + "\t-\n");
            } else if (f.isFile()) {
                x.append("<a href='" + name + "'>" + name + "</a>\t");
                x.append(time + "\t" + f.length() + "\n");
            } else {
                // ignore any other
            }
        }

        x.append("</pre></body></html>\n");

        return x.toString().getBytes();
    }
}
