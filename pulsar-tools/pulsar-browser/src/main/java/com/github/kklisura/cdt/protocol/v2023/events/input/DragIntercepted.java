package com.github.kklisura.cdt.protocol.v2023.events.input;

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
import com.github.kklisura.cdt.protocol.v2023.types.input.DragData;

/**
 * Emitted only when `Input.setInterceptDrags` is enabled. Use this data with
 * `Input.dispatchDragEvent` to restore normal drag and drop behavior.
 */
@Experimental
public class DragIntercepted {

  private DragData data;

  public DragData getData() {
    return data;
  }

  public void setData(DragData data) {
    this.data = data;
  }
}
