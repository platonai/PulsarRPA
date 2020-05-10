package com.github.kklisura.cdt.protocol.types.profiler;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2019 Kenan Klisura
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/** Coverage data for a source range. */
public class CoverageRange {

  private Integer startOffset;

  private Integer endOffset;

  private Integer count;

  /** JavaScript script source offset for the range start. */
  public Integer getStartOffset() {
    return startOffset;
  }

  /** JavaScript script source offset for the range start. */
  public void setStartOffset(Integer startOffset) {
    this.startOffset = startOffset;
  }

  /** JavaScript script source offset for the range end. */
  public Integer getEndOffset() {
    return endOffset;
  }

  /** JavaScript script source offset for the range end. */
  public void setEndOffset(Integer endOffset) {
    this.endOffset = endOffset;
  }

  /** Collected execution count of the source range. */
  public Integer getCount() {
    return count;
  }

  /** Collected execution count of the source range. */
  public void setCount(Integer count) {
    this.count = count;
  }
}
