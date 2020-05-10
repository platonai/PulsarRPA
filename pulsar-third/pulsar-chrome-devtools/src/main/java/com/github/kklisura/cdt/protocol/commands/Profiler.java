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

import com.github.kklisura.cdt.protocol.events.profiler.ConsoleProfileFinished;
import com.github.kklisura.cdt.protocol.events.profiler.ConsoleProfileStarted;
import com.github.kklisura.cdt.protocol.support.annotations.EventName;
import com.github.kklisura.cdt.protocol.support.annotations.Experimental;
import com.github.kklisura.cdt.protocol.support.annotations.Optional;
import com.github.kklisura.cdt.protocol.support.annotations.ParamName;
import com.github.kklisura.cdt.protocol.support.annotations.ReturnTypeParameter;
import com.github.kklisura.cdt.protocol.support.annotations.Returns;
import com.github.kklisura.cdt.protocol.support.types.EventHandler;
import com.github.kklisura.cdt.protocol.support.types.EventListener;
import com.github.kklisura.cdt.protocol.types.profiler.Profile;
import com.github.kklisura.cdt.protocol.types.profiler.ScriptCoverage;
import com.github.kklisura.cdt.protocol.types.profiler.ScriptTypeProfile;
import java.util.List;

public interface Profiler {

  void disable();

  void enable();

  /**
   * Collect coverage data for the current isolate. The coverage data may be incomplete due to
   * garbage collection.
   */
  @Returns("result")
  @ReturnTypeParameter(ScriptCoverage.class)
  List<ScriptCoverage> getBestEffortCoverage();

  /**
   * Changes CPU profiler sampling interval. Must be called before CPU profiles recording started.
   *
   * @param interval New sampling interval in microseconds.
   */
  void setSamplingInterval(@ParamName("interval") Integer interval);

  void start();

  /**
   * Enable precise code coverage. Coverage data for JavaScript executed before enabling precise
   * code coverage may be incomplete. Enabling prevents running optimized code and resets execution
   * counters.
   */
  void startPreciseCoverage();

  /**
   * Enable precise code coverage. Coverage data for JavaScript executed before enabling precise
   * code coverage may be incomplete. Enabling prevents running optimized code and resets execution
   * counters.
   *
   * @param callCount Collect accurate call counts beyond simple 'covered' or 'not covered'.
   * @param detailed Collect block-based coverage.
   */
  void startPreciseCoverage(
      @Optional @ParamName("callCount") Boolean callCount,
      @Optional @ParamName("detailed") Boolean detailed);

  /** Enable type profile. */
  @Experimental
  void startTypeProfile();

  @Returns("profile")
  Profile stop();

  /**
   * Disable precise code coverage. Disabling releases unnecessary execution count records and
   * allows executing optimized code.
   */
  void stopPreciseCoverage();

  /** Disable type profile. Disabling releases type profile data collected so far. */
  @Experimental
  void stopTypeProfile();

  /**
   * Collect coverage data for the current isolate, and resets execution counters. Precise code
   * coverage needs to have started.
   */
  @Returns("result")
  @ReturnTypeParameter(ScriptCoverage.class)
  List<ScriptCoverage> takePreciseCoverage();

  /** Collect type profile. */
  @Experimental
  @Returns("result")
  @ReturnTypeParameter(ScriptTypeProfile.class)
  List<ScriptTypeProfile> takeTypeProfile();

  @EventName("consoleProfileFinished")
  EventListener onConsoleProfileFinished(EventHandler<ConsoleProfileFinished> eventListener);

  /** Sent when new profile recording is started using console.profile() call. */
  @EventName("consoleProfileStarted")
  EventListener onConsoleProfileStarted(EventHandler<ConsoleProfileStarted> eventListener);
}
