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
package fun.platonic.pulsar.protocol.file;

import crawlercommons.robots.BaseRobotRules;
import fun.platonic.pulsar.common.StringUtil;
import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.crawl.protocol.Protocol;
import fun.platonic.pulsar.crawl.protocol.ProtocolOutput;
import fun.platonic.pulsar.crawl.protocol.RobotRulesParser;
import fun.platonic.pulsar.persist.ProtocolStatus;
import fun.platonic.pulsar.persist.WebPage;
import fun.platonic.pulsar.persist.gora.generated.GWebPage;
import fun.platonic.pulsar.persist.metadata.MultiMetadata;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * This class is a protocol plugin used for file: scheme. It creates
 * {@link FileResponse} object and gets the content of the url from it.
 * Configurable parameters are {@code file.content.limit} and
 * {@code file.crawl.parent} in pulsar-default.xml defined under
 * "file properties" section.
 */
public class File implements Protocol {

    public static final Logger LOG = LoggerFactory.getLogger(File.class);

    static final int MAX_REDIRECTS = 5;

    int maxContentLength;

    boolean crawlParents;

    /**
     * if true return a redirect for symbolic links and do not resolve the links
     * internally
     */
    boolean symlinksAsRedirects = true;

    private ImmutableConfig conf;

    // constructor
    public File() {
    }

    /**
     * Get the {@link Configuration} object
     */
    public ImmutableConfig getConf() {
        return this.conf;
    }

    /**
     * Set the {@link Configuration} object
     */
    @Override
    public void setConf(ImmutableConfig conf) {
        this.conf = conf;
        this.maxContentLength = conf.getInt("file.content.limit", 64 * 1024);
        this.crawlParents = conf.getBoolean("file.crawl.parent", true);
        this.symlinksAsRedirects = conf.getBoolean(
                "file.crawl.redirect_noncanonical", true);
    }

    /**
     * Set the point at which content is truncated.
     */
    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    /**
     * Creates a {@link FileResponse} object corresponding to the url and return a
     * {@link ProtocolOutput} object as per the content received
     *
     * @param page The CrawlDatum object corresponding to the url
     * @return {@link ProtocolOutput} object for the content of the file indicated
     * by url
     */
    public ProtocolOutput getProtocolOutput(WebPage page) {
        MultiMetadata headers = new MultiMetadata();

        try {
            URL u = new URL(page.getUrl());

            int redirects = 0;

            while (true) {
                FileResponse response;
                response = new FileResponse(u, page, this, getConf()); // make a request
                int code = response.getCode();
                if (code == 200) { // got a good response
                    return new ProtocolOutput(response.toContent()); // return it
                } else if (code == 304) { // got not modified
                    return new ProtocolOutput(response.toContent(), headers, ProtocolStatus.STATUS_NOTMODIFIED);
                } else if (code == 401) { // access denied / no read permissions
                    return new ProtocolOutput(response.toContent(), headers, ProtocolStatus.STATUS_ACCESS_DENIED);
                } else if (code == 404) { // no such file
                    return new ProtocolOutput(response.toContent(), headers, ProtocolStatus.STATUS_NOTFOUND);
                } else if (code >= 300 && code < 400) { // handle redirect
                    u = new URL(response.getHeader("Location"));
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("redirect to " + u);
                    }
                    if (symlinksAsRedirects) {
                        return new ProtocolOutput(response.toContent(), headers, ProtocolStatus.failed(ProtocolStatus.MOVED, "url", u));
                    } else if (redirects == MAX_REDIRECTS) {
                        LOG.trace("Too many redirects: {}", page.getUrl());
                        return new ProtocolOutput(response.toContent(), headers, ProtocolStatus.failed(ProtocolStatus.REDIR_EXCEEDED, "url", u));
                    }
                    redirects++;
                } else { // convert to exception
                    throw new FileError(code);
                }
            }
        } catch (Exception e) {
            LOG.warn(StringUtil.stringifyException(e));
            return new ProtocolOutput(null, headers, ProtocolStatus.failed(e));
        }
    }

    @Override
    public BaseRobotRules getRobotRules(WebPage page) {
        return null;
    }

    /**
     * No robots parsing is done for file protocol. So this returns a set of empty
     * rules which will allow every url.
     */
    public BaseRobotRules getRobotRules(String url, GWebPage page) {
        return RobotRulesParser.EMPTY_RULES;
    }
}
