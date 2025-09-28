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
package ai.platon.pulsar.bench;

import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Simple micro benchmark comparing different string concatenation approaches.
 * Purpose: Provide a template for adding new JMH benchmarks and a sanity performance baseline.
 * Metric: ops/s (higher is better).
 * Potential regression triggers: Large intermediate buffers, unnecessary encoding, GC pressure.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(value = 1)
@State(Scope.Thread)
public class StringOpsBenchmark {

    @Param({"16", "128", "1024"})
    public int size;

    private String[] tokens;

    private static final Random R = new Random(1234);

    @Setup(Level.Trial)
    public void setUp() {
        tokens = new String[size];
        for (int i = 0; i < size; i++) {
            tokens[i] = randomToken();
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[8];
        R.nextBytes(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    @Benchmark
    public String plusConcat() {
        String s = "";
        for (String t : tokens) {
            s = s + t; // intentionally naive
        }
        return s;
    }

    @Benchmark
    public String builderConcat() {
        StringBuilder sb = new StringBuilder(size * 8 + 8);
        for (String t : tokens) {
            sb.append(t);
        }
        return sb.toString();
    }

    @Benchmark
    public int hashCombine() {
        // Example of a lightweight non-string baseline to compare allocation cost
        int h = 1;
        for (String t : tokens) {
            h = 31 * h + t.hashCode();
        }
        return h;
    }
}

