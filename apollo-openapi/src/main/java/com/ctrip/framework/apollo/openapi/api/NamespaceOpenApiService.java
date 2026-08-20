/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.api;

import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import java.util.List;

/**
 * @author wxq
 */
public interface NamespaceOpenApiService {

  default OpenNamespaceDTO getNamespace(String appId, String env, String clusterName, String namespaceName) {
    return getNamespace(appId, env, clusterName, namespaceName, true);
  }

  /**
   * Retrieves a single namespace
   * @since 2.4.0
   */
  default OpenNamespaceDTO getNamespace(String appId, String env, String clusterName, String namespaceName, boolean fillItemDetail) {
    return getNamespace(appId, env, clusterName, namespaceName, fillItemDetail, false);
  }

  /**
   * Retrieves a single namespace, optionally including extra info such as {@code parentAppId}
   * of an associated public namespace.
   * @since 2.6.0
   */
  OpenNamespaceDTO getNamespace(String appId, String env, String clusterName, String namespaceName, boolean fillItemDetail, boolean extendInfo);

  default List<OpenNamespaceDTO> getNamespaces(String appId, String env, String clusterName) {
    return getNamespaces(appId, env, clusterName, true);
  }

  /**
   * Retrieves a list namespaces
   * @since 2.4.0
   */
  default List<OpenNamespaceDTO> getNamespaces(String appId, String env, String clusterName, boolean fillItemDetail) {
    return getNamespaces(appId, env, clusterName, fillItemDetail, false);
  }

  /**
   * Retrieves a list namespaces, optionally including extra info such as {@code parentAppId}
   * of an associated public namespace.
   * @since 2.6.0
   */
  List<OpenNamespaceDTO> getNamespaces(String appId, String env, String clusterName, boolean fillItemDetail, boolean extendInfo);

  OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespaceDTO);

  OpenNamespaceLockDTO getNamespaceLock(String appId, String env, String clusterName,
      String namespaceName);
}
