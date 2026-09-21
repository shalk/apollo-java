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
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Routes okhttp's MockWebServer logs (which go through java.util.logging directly) to
 * slf4j/logback for the lifetime of active {@link ApolloTestingServer} instances.
 *
 * <p>Unlike {@link SLF4JBridgeHandler#install()}, this only touches the JUL logger used by
 * MockWebServer, not the JUL root logger, so unrelated JUL handlers elsewhere in the JVM are
 * never removed. It also skips installing when the SLF4J binding is slf4j-jdk14, since bridging
 * JUL to an SLF4J binding that itself delegates back to JUL creates an infinite loop. Calls are
 * reference-counted so concurrently running mock servers don't uninstall the bridge out from
 * under each other.
 */
final class JulSlf4jBridge {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JulSlf4jBridge.class);
  private static final String MOCK_WEB_SERVER_LOGGER_NAME = "okhttp3.mockwebserver.MockWebServer";
  private static final AtomicInteger ACTIVE = new AtomicInteger(0);

  private static Handler[] originalHandlers;
  private static boolean originalUseParentHandlers;
  private static boolean bridged;
  private static SLF4JBridgeHandler bridgeHandler;

  private JulSlf4jBridge() {}

  static synchronized void install() {
    if (ACTIVE.getAndIncrement() > 0) {
      return;
    }
    bridged = false;
    if (isJdk14Binding(LoggerFactory.getILoggerFactory().getClass().getName())) {
      LOG.warn("Skipping JUL-to-SLF4J bridge for MockWebServer logs because the SLF4J binding "
          + "is slf4j-jdk14; bridging it would create an infinite JUL<->SLF4J logging loop.");
      return;
    }
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    originalHandlers = mockWebServerLogger.getHandlers();
    originalUseParentHandlers = mockWebServerLogger.getUseParentHandlers();
    for (Handler handler : originalHandlers) {
      mockWebServerLogger.removeHandler(handler);
    }
    bridgeHandler = new SLF4JBridgeHandler();
    mockWebServerLogger.addHandler(bridgeHandler);
    mockWebServerLogger.setUseParentHandlers(false);
    bridged = true;
  }

  static synchronized void uninstall() {
    if (ACTIVE.decrementAndGet() > 0 || !bridged) {
      return;
    }
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    mockWebServerLogger.removeHandler(bridgeHandler);
    for (Handler handler : originalHandlers) {
      if (!containsHandler(mockWebServerLogger.getHandlers(), handler)) {
        mockWebServerLogger.addHandler(handler);
      }
    }
    mockWebServerLogger.setUseParentHandlers(originalUseParentHandlers);
    originalHandlers = null;
    bridgeHandler = null;
    bridged = false;
  }

  private static boolean containsHandler(Handler[] handlers, Handler target) {
    for (Handler handler : handlers) {
      if (handler == target) {
        return true;
      }
    }
    return false;
  }

  /** Package-visible for testing without depending on the actual runtime SLF4J binding. */
  static boolean isJdk14Binding(String iLoggerFactoryClassName) {
    return "org.slf4j.impl.JDK14LoggerFactory".equals(iLoggerFactoryClassName)
        || "org.slf4j.jul.JDK14LoggerFactory".equals(iLoggerFactoryClassName);
  }
}
