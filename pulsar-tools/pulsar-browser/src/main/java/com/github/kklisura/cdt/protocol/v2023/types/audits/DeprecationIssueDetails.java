package com.github.kklisura.cdt.protocol.v2023.types.audits;

/*-
 * #%L
 * cdt-java-client
 * %%
 * Copyright (C) 2018 - 2023 Kenan Klisura
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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

/**
 * This issue tracks information needed to print a deprecation message.
 * https://source.chromium.org/chromium/chromium/src/+/main:third_party/blink/renderer/core/frame/third_party/blink/renderer/core/frame/deprecation/README.md
 */
public class DeprecationIssueDetails {

  @Optional
  private AffectedFrame affectedFrame;

  private SourceCodeLocation sourceCodeLocation;

  private String type;

  public AffectedFrame getAffectedFrame() {
    return affectedFrame;
  }

  public void setAffectedFrame(AffectedFrame affectedFrame) {
    this.affectedFrame = affectedFrame;
  }

  public SourceCodeLocation getSourceCodeLocation() {
    return sourceCodeLocation;
  }

  public void setSourceCodeLocation(SourceCodeLocation sourceCodeLocation) {
    this.sourceCodeLocation = sourceCodeLocation;
  }

  /**
   * One of the deprecation names from
   * third_party/blink/renderer/core/frame/deprecation/deprecation.json5
   */
  public String getType() {
    return type;
  }

  /**
   * One of the deprecation names from
   * third_party/blink/renderer/core/frame/deprecation/deprecation.json5
   */
  public void setType(String type) {
    this.type = type;
  }
}
