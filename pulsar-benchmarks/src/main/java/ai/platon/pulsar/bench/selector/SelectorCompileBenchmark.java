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
package ai.platon.pulsar.bench.selector;

import ai.platon.pulsar.bench.support.AbstractBenchmarkSupport;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Skeleton benchmark for selector "compilation" / normalization.
 * Purpose: Provide a placeholder to plug the real selector parser later.
 * Metric: operations/second (Throughput).
 * Regression triggers: Explosion in intermediate allocations, quadratic scans.
 * NOTE: Currently uses a naive token split implementation as a stand-in.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 4)
@Fork(1)
@State(Scope.Thread)
public class SelectorCompileBenchmark extends AbstractBenchmarkSupport {

    @Param({"SMALL", "MEDIUM", "LARGE"})
    public String size;

    private List<String> selectorSets;

    @Setup(Level.Trial)
    public void setup() {
        int groups;
        int perGroup;
        switch (size) {
            case "SMALL":
                groups = 32; perGroup = 3; break;
            case "MEDIUM":
                groups = 128; perGroup = 5; break;
            default:
                groups = 512; perGroup = 7; break;
        }
        selectorSets = new ArrayList<>(groups);
        for (int g = 0; g < groups; g++) {
            StringBuilder sb = new StringBuilder(perGroup * 12);
            for (int i = 0; i < perGroup; i++) {
                sb.append(randomAscii(3));
                if (i % 2 == 0) sb.append(':').append("nth-child(").append(i + 1).append(')');
                if (i + 1 < perGroup) sb.append(' ');
            }
            selectorSets.add(sb.toString());
        }
    }

    @Benchmark
    public void naiveTokenSplit(Blackhole bh) {
        for (String s : selectorSets) {
            // Naive placeholder: split on whitespace then colon/punctuation
            // Simulates some tokenization overhead.
            int pseudoHash = 1;
            String[] tokens = s.split("[\\s:()]+", -1);
            for (String t : tokens) {
                if (!t.isEmpty()) {
                    // mimic normalization (lowercase + hash combine)
                    pseudoHash = 31 * pseudoHash + t.toLowerCase().hashCode();
                }
            }
            bh.consume(pseudoHash);
        }
    }
}

