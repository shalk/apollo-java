/*
 * Copyright 2026 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.mockserver;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Routes okhttp's MockWebServer logs (which go through java.util.logging directly) to
 * slf4j/logback for the lifetime of active {@link ApolloTestingServer} instances, restoring the
 * original JUL root handlers once the last one closes. Reference-counted so concurrently running
 * mock servers don't uninstall the bridge out from under each other.
 */
final class JulSlf4jBridge {

  private static final AtomicInteger ACTIVE = new AtomicInteger(0);
  private static Handler[] originalHandlers;

  private JulSlf4jBridge() {}

  static synchronized void install() {
    if (ACTIVE.getAndIncrement() == 0) {
      originalHandlers = LogManager.getLogManager().getLogger("").getHandlers();
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    }
  }

  static synchronized void uninstall() {
    if (ACTIVE.decrementAndGet() == 0) {
      SLF4JBridgeHandler.uninstall();
      for (Handler handler : originalHandlers) {
        LogManager.getLogManager().getLogger("").addHandler(handler);
      }
      originalHandlers = null;
    }
  }
}
