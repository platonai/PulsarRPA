/*
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
 */
package ai.platon.pulsar.bench.support;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Minimal shared helpers for benchmarks.
 * Keep intentionally small to avoid influencing benchmark cost structure.
 */
public abstract class AbstractBenchmarkSupport {
    protected final Random rnd;

    protected AbstractBenchmarkSupport() {
        // Deterministic seed for reproducibility; can be overridden in subclasses if needed.
        this.rnd = new Random(123456789L);
    }

    protected List<String> randomTokens(int n, int len) {
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(randomAscii(len));
        }
        return out;
    }

    protected String randomAscii(int len) {
        char[] c = new char[len];
        for (int i = 0; i < len; i++) {
            int x = 32 + rnd.nextInt(95); // printable
            c[i] = (char) x;
        }
        return new String(c);
    }

    /** Simple HTML snippet generator for synthetic docs. */
    protected String generateHtml(int paragraphs, int wordsPerParagraph) {
        StringBuilder sb = new StringBuilder(paragraphs * wordsPerParagraph * 8);
        sb.append("<html><head><title>T</title></head><body>\n");
        for (int p = 0; p < paragraphs; p++) {
            sb.append("<p>");
            for (int w = 0; w < wordsPerParagraph; w++) {
                sb.append(randomAscii(5));
                sb.append(' ');
            }
            sb.append("</p>\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}

