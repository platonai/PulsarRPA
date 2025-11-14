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
package ai.platon.pulsar.bench.html;

import ai.platon.pulsar.bench.support.AbstractBenchmarkSupport;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Skeleton HTML parse benchmark.
 * Purpose: Provide a placeholder for real HTML tokenizer / DOM builder integration.
 * Current implementation simulates parsing by scanning characters and counting tags & paragraphs.
 * Metric: Throughput (documents/second).
 * Regression triggers: Higher algorithmic complexity in tag scanning, excessive allocations.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 4)
@Fork(1)
@State(Scope.Thread)
public class HtmlParseBenchmark extends AbstractBenchmarkSupport {

    @Param({"SMALL","MEDIUM","LARGE"})
    public String size;

    private String htmlDoc;

    @Setup(Level.Trial)
    public void setup() {
        int paragraphs;
        int words;
        switch (size) {
            case "SMALL": paragraphs = 8; words = 12; break;
            case "MEDIUM": paragraphs = 40; words = 18; break;
            default: paragraphs = 120; words = 24; break;
        }
        htmlDoc = generateHtml(paragraphs, words);
    }

    @Benchmark
    public void simulateParse(Blackhole bh) {
        // Naive tag & paragraph counter to mimic some CPU work without external libs.
        int tags = 0;
        int ps = 0;
        char[] chars = htmlDoc.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '<') {
                tags++;
                // crude check for <p>
                if (i + 2 < chars.length && (chars[i + 1] == 'p' || chars[i + 1] == 'P') && chars[i + 2] == '>') {
                    ps++;
                }
            }
        }
        // combine into pseudo hash to avoid DCE
        int pseudo = 31 * tags + ps;
        bh.consume(pseudo);
    }
}

