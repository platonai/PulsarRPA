package com.github.kklisura.cdt.protocol.types.runtime;

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

import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;

@Experimental
public class EntryPreview {

  @Optional private ObjectPreview key;

  private ObjectPreview value;

  /** Preview of the key. Specified for map-like collection entries. */
  public ObjectPreview getKey() {
    return key;
  }

  /** Preview of the key. Specified for map-like collection entries. */
  public void setKey(ObjectPreview key) {
    this.key = key;
  }

  /** Preview of the value. */
  public ObjectPreview getValue() {
    return value;
  }

  /** Preview of the value. */
  public void setValue(ObjectPreview value) {
    this.value = value;
  }
}
