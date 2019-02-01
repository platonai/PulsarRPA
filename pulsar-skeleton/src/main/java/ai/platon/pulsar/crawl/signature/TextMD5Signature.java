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

package ai.platon.pulsar.crawl.signature;

import ai.platon.pulsar.persist.WebPage;
import org.apache.hadoop.io.MD5Hash;

/**
 * Default implementation of a page signature. It calculates an MD5 hash of the
 * textual content of a page. In case there is no signature, it calculates a hash
 * from the page's fetched content.
 */
public class TextMD5Signature extends Signature {

    public static int GOOD_CONTENT_TEXT_LENGTH = 2000;

    private Signature fallback = new MD5Signature();

    /**
     * We need calculate signature using a more clean signature content, eg, extracted by signature-persist
     * */
    @Override
    public byte[] calculate(WebPage page) {
        String text = page.getContentText();
        if (text.isEmpty() || text.length() < GOOD_CONTENT_TEXT_LENGTH) {
            text = page.getPageText();
        }

        if (text.isEmpty()) {
            return fallback.calculate(page);
        }

        return MD5Hash.digest(text).getDigest();
    }
}
