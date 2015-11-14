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

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.manager.DefaultCacheManager;

@ApplicationScoped
public class InfinispanCacheManagerProvider {

  @Inject
  private GlobalConfiguration globalConfiguration;

  @Inject
  private Configuration configuration;

  private DefaultCacheManager manager;

  @Produces
  public DefaultCacheManager getCacheManager() {
    if (manager == null) {
      manager = new DefaultCacheManager(globalConfiguration, configuration, true);
    }
    return manager;
  }

  @PreDestroy
  public void cleanUp() {
    manager.stop();
    manager = null;
  }
}
