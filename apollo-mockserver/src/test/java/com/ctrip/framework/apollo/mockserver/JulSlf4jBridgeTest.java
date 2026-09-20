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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Handler;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

class JulSlf4jBridgeTest {

  private static final String MOCK_WEB_SERVER_LOGGER_NAME = "okhttp3.mockwebserver.MockWebServer";

  @Test
  void isJdk14BindingDetectsKnownFactoryClassNames() {
    assertTrue(JulSlf4jBridge.isJdk14Binding("org.slf4j.impl.JDK14LoggerFactory"));
    assertTrue(JulSlf4jBridge.isJdk14Binding("org.slf4j.jul.JDK14LoggerFactory"));
    assertFalse(JulSlf4jBridge.isJdk14Binding("ch.qos.logback.classic.LoggerContext"));
  }

  @Test
  void installOnlyTouchesMockWebServerLoggerNotRoot() {
    Logger root = Logger.getLogger("");
    Handler[] rootHandlersBefore = root.getHandlers();

    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    Handler[] mwsHandlersBefore = mockWebServerLogger.getHandlers();
    boolean useParentBefore = mockWebServerLogger.getUseParentHandlers();

    JulSlf4jBridge.install();
    try {
      assertArrayEquals(rootHandlersBefore, root.getHandlers(),
          "installing the bridge must not touch the JUL root logger's handlers");
      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "MockWebServer's own logger should get the bridge handler");
      assertFalse(mockWebServerLogger.getUseParentHandlers());
    } finally {
      JulSlf4jBridge.uninstall();
    }

    assertArrayEquals(rootHandlersBefore, root.getHandlers());
    assertArrayEquals(mwsHandlersBefore, mockWebServerLogger.getHandlers());
    assertEquals(useParentBefore, mockWebServerLogger.getUseParentHandlers());
  }

  @Test
  void closingSameServerTwiceDoesNotDisruptAnotherActiveServer() throws Exception {
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    ApolloTestingServer serverA = new ApolloTestingServer();
    ApolloTestingServer serverB = new ApolloTestingServer();
    try {
      serverA.start();
      serverB.start();
      assertTrue(hasBridgeHandler(mockWebServerLogger));

      serverA.close();
      serverA.close();

      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "bridge must remain installed while server B is still running");
    } finally {
      serverA.close();
      serverB.close();
    }

    assertFalse(hasBridgeHandler(mockWebServerLogger));
  }

  @Test
  void closingNeverStartedServerDoesNotDisruptActiveServer() throws Exception {
    Logger mockWebServerLogger = Logger.getLogger(MOCK_WEB_SERVER_LOGGER_NAME);
    ApolloTestingServer active = new ApolloTestingServer();
    ApolloTestingServer neverStarted = new ApolloTestingServer();
    try {
      active.start();
      assertTrue(hasBridgeHandler(mockWebServerLogger));

      neverStarted.close();

      assertTrue(hasBridgeHandler(mockWebServerLogger),
          "closing a server that was never started must not release a bridge it never acquired");
    } finally {
      active.close();
      neverStarted.close();
    }

    assertFalse(hasBridgeHandler(mockWebServerLogger));
  }

  private static boolean hasBridgeHandler(Logger logger) {
    for (Handler handler : logger.getHandlers()) {
      if (handler instanceof SLF4JBridgeHandler) {
        return true;
      }
    }
    return false;
  }
}
