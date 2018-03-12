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

package org.warps.pulsar.crawl.signature;

import org.apache.avro.util.Utf8;
import org.apache.hadoop.io.MD5Hash;
import org.warps.pulsar.persist.WebPage;

import java.nio.ByteBuffer;

/**
 * Default implementation of a page signature. It calculates an MD5 hash of the
 * raw binary content of a page. In case there is no content, it calculates a
 * hash from the page's URL.
 *
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public class MD5Signature extends Signature {

    @Override
    public byte[] calculate(WebPage page) {
        ByteBuffer buf = page.getContent();
        byte[] data;
        int of;
        int cb;
        if (buf == null) {
            Utf8 baseUrl = new Utf8(page.getBaseUrl());
            data = baseUrl.getBytes();
            of = 0;
            cb = baseUrl.length();
        } else {
            data = buf.array();
            of = buf.arrayOffset() + buf.position();
            cb = buf.remaining();
        }

        return MD5Hash.digest(data, of, cb).getDigest();
    }
}
