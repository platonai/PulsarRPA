package com.github.kklisura.cdt.protocol.commands;

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

import com.github.kklisura.cdt.protocol.events.applicationcache.ApplicationCacheStatusUpdated;
import com.github.kklisura.cdt.protocol.events.applicationcache.NetworkStateUpdated;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.ReturnTypeParameter;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;
import com.github.kklisura.cdt.protocol.types.applicationcache.FrameWithManifest;
import java.util.List;

@Experimental
public interface ApplicationCache {

  /** Enables application cache domain notifications. */
  void enable();

  /**
   * Returns relevant application cache data for the document in given frame.
   *
   * @param frameId Identifier of the frame containing document whose application cache is
   *     retrieved.
   */
  @Returns("applicationCache")
  com.github.kklisura.cdt.protocol.types.applicationcache.ApplicationCache
      getApplicationCacheForFrame(@ParamName("frameId") String frameId);

  /**
   * Returns array of frame identifiers with manifest urls for each frame containing a document
   * associated with some application cache.
   */
  @Returns("frameIds")
  @ReturnTypeParameter(FrameWithManifest.class)
  List<FrameWithManifest> getFramesWithManifests();

  /**
   * Returns manifest URL for document in the given frame.
   *
   * @param frameId Identifier of the frame containing document whose manifest is retrieved.
   */
  @Returns("manifestURL")
  String getManifestForFrame(@ParamName("frameId") String frameId);

  @EventName("applicationCacheStatusUpdated")
  EventListener onApplicationCacheStatusUpdated(
      EventHandler<ApplicationCacheStatusUpdated> eventListener);

  @EventName("networkStateUpdated")
  EventListener onNetworkStateUpdated(EventHandler<NetworkStateUpdated> eventListener);
}
