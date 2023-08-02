package com.github.kklisura.cdt.protocol.v2023.types.input;

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

@Experimental
public class DragData {

  private List<DragDataItem> items;

  @Optional
  private List<String> files;

  private Integer dragOperationsMask;

  public List<DragDataItem> getItems() {
    return items;
  }

  public void setItems(List<DragDataItem> items) {
    this.items = items;
  }

  /** List of filenames that should be included when dropping */
  public List<String> getFiles() {
    return files;
  }

  /** List of filenames that should be included when dropping */
  public void setFiles(List<String> files) {
    this.files = files;
  }

  /** Bit field representing allowed drag operations. Copy = 1, Link = 2, Move = 16 */
  public Integer getDragOperationsMask() {
    return dragOperationsMask;
  }

  /** Bit field representing allowed drag operations. Copy = 1, Link = 2, Move = 16 */
  public void setDragOperationsMask(Integer dragOperationsMask) {
    this.dragOperationsMask = dragOperationsMask;
  }
}
