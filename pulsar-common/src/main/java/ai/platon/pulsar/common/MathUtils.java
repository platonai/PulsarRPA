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

package ai.platon.pulsar.common;

/**
 * A minimal math utility class.
 * @deprecated Use a professional library
 */
public final class MathUtils {

    public static float hashFloat(float x, float in, float in2, float out, float out2) {
        if (x < out) return out;
        if (x > out2) return out2;

        // output = output_start + ((output_end - output_start) / (input_end - input_start)) * (input - input_start)
        return out + (out2 - out) / (in2 - in) * (x - in);
    }

    public static int hashInt(int x, int in, int in2, int out, int out2) {
        if (x < out) return out;
        if (x > out2) return out2;

        float r = out + 1.0f * (out2 - out) / (in2 - in) * (x - in);
        return (int) r;
    }
}
