/*
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
 */
package org.jboss.poc.greeter.impl;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener()
public class GreeterServiceRegistrar implements ServletContextListener {

  private static final String SERVICE_NAMESPACE = "http://poc.jboss.org/greeter";
  private static final String SERVICE_NAME = "GreeterService";

  private static final String REGISTRY_CACHE_NAME = "registry-cache";

  private final Logger log = LoggerFactory.getLogger(GreeterServiceRegistrar.class);

  @Inject
  private DefaultCacheManager cacheManager;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    final String key = getServiceKey();
    final String uri = getServiceURI(sce);

    if (cacheManager != null) {
      log.info(String.format("Registering service: {'%s': '%s'}", key, uri));

      Cache<String, Set<String>> cache = cacheManager.getCache(REGISTRY_CACHE_NAME);
      Set<String> uris = cache.getOrDefault(key, new HashSet<String>());
      uris.add(uri);
      cache.put(key, uris);
    } else {
      log.warn(String.format("Unable to register service: {'%s': '%s'}. CacheManager is null.", key, uri));
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    final String key = getServiceKey();
    final String uri = getServiceURI(sce);

    if (cacheManager != null) {
      log.info(String.format("Unregistering service: {'%s': '%s'}", key, uri));
      Cache<String, Set<String>> cache = cacheManager.getCache(REGISTRY_CACHE_NAME);
      Set<String> uris = cache.getOrDefault(key, new HashSet<String>());
      uris.remove(uri);
      cache.put(key, uris);
    } else {
      log.warn(String.format("Unable to unregister service: {'%s': '%s'}. CacheManager is null.", key, uri));
    }
  }

  private String getServiceKey() {
    return String.format("/services/soap-http/{%s}%s", SERVICE_NAMESPACE, SERVICE_NAME);
  }

  private String getServiceURI(ServletContextEvent sce) {
    String prefix = "http";
    String host;
    try {
      host = (String) ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("jboss.as:interface=public"), "inet-address");
    } catch (MalformedObjectNameException | MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException e) {
      log.warn("Unable to find service host. Attempting to resolve local address.", e);
      try {
        host = InetAddress.getLocalHost().getHostAddress();
      } catch (UnknownHostException e1) {
        log.warn("Unable to find service host. Defaulting to 'localhost'.", e1);
        host = "localhost";
      }
    }
    Integer port;
    try {
      port = (Integer) ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("jboss.as:socket-binding-group=standard-sockets,socket-binding=http"), "port");
    } catch (MalformedObjectNameException | MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException e) {
      log.warn("Unable to find service port. Defaulting to '8080'.", e);
      port = 8080;
    }
    String contextPath = sce.getServletContext().getContextPath();
    return String.format("%s://%s:%s%s/%s", prefix, host, port, contextPath, SERVICE_NAME);
  }
}
