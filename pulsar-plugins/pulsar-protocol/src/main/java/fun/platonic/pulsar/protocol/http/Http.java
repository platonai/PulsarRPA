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
package fun.platonic.pulsar.protocol.http;

import fun.platonic.pulsar.common.config.ImmutableConfig;
import fun.platonic.pulsar.common.proxy.NoProxyException;
import fun.platonic.pulsar.crawl.protocol.Protocol;
import fun.platonic.pulsar.crawl.protocol.ProtocolException;
import fun.platonic.pulsar.crawl.protocol.Response;
import fun.platonic.pulsar.crawl.protocol.http.AbstractHttpProtocol;
import fun.platonic.pulsar.persist.WebPage;

import java.io.IOException;
import java.net.URL;

public class Http extends AbstractHttpProtocol {

    public Http() {
    }

    public Http(ImmutableConfig conf) {
        setConf(conf);
    }

    @Override
    protected Response getResponse(String url, WebPage page, boolean redirect)
            throws ProtocolException, IOException, NoProxyException {
        Response r = null;
        try {
            r = new HttpResponse(this, new URL(url), page);
        } catch (InterruptedException e) {
            Protocol.LOG.error(e.toString());
        }
        return r;
    }
}
