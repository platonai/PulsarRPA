package com.github.kklisura.cdt.protocol.events.webaudio;

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

import com.github.kklisura.cdt.protocol.types.webaudio.AudioNode;

/** Notifies that a new AudioNode has been created. */
public class AudioNodeCreated {

  private AudioNode node;

  public AudioNode getNode() {
    return node;
  }

  public void setNode(AudioNode node) {
    this.node = node;
  }
}
