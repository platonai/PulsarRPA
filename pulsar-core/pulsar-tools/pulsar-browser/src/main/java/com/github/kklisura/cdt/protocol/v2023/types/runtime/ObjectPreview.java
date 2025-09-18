package com.github.kklisura.cdt.protocol.v2023.types.runtime;

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

import com.github.kklisura.cdt.protocol.v2023.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.v2023.support.annotations.Optional;

import java.util.List;

/** Object containing abbreviated remote object value. */
@Experimental
public class ObjectPreview {

  private ObjectPreviewType type;

  @Optional
  private ObjectPreviewSubtype subtype;

  @Optional private String description;

  private Boolean overflow;

  private List<PropertyPreview> properties;

  @Optional private List<EntryPreview> entries;

  /** Object type. */
  public ObjectPreviewType getType() {
    return type;
  }

  /** Object type. */
  public void setType(ObjectPreviewType type) {
    this.type = type;
  }

  /** Object subtype hint. Specified for `object` type values only. */
  public ObjectPreviewSubtype getSubtype() {
    return subtype;
  }

  /** Object subtype hint. Specified for `object` type values only. */
  public void setSubtype(ObjectPreviewSubtype subtype) {
    this.subtype = subtype;
  }

  /** String representation of the object. */
  public String getDescription() {
    return description;
  }

  /** String representation of the object. */
  public void setDescription(String description) {
    this.description = description;
  }

  /** True iff some of the properties or entries of the original object did not fit. */
  public Boolean getOverflow() {
    return overflow;
  }

  /** True iff some of the properties or entries of the original object did not fit. */
  public void setOverflow(Boolean overflow) {
    this.overflow = overflow;
  }

  /** List of the properties. */
  public List<PropertyPreview> getProperties() {
    return properties;
  }

  /** List of the properties. */
  public void setProperties(List<PropertyPreview> properties) {
    this.properties = properties;
  }

  /** List of the entries. Specified for `map` and `set` subtype values only. */
  public List<EntryPreview> getEntries() {
    return entries;
  }

  /** List of the entries. Specified for `map` and `set` subtype values only. */
  public void setEntries(List<EntryPreview> entries) {
    this.entries = entries;
  }
}
