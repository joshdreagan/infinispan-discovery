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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;

@ApplicationScoped
public class InfinispanConfigurationProvider {

  private static final String CLUSTER_NAME = "registry-cluster";
  
  @Produces
  public GlobalConfiguration defaultGlobalConfiguration() {
    return new GlobalConfigurationBuilder()
            .clusteredDefault()
            .transport()
            .defaultTransport()
            .clusterName(CLUSTER_NAME)
            .addProperty("configurationFile", "default-configs/default-jgroups-tcp.xml")
            .globalJmxStatistics().allowDuplicateDomains(true).enable()
            .build();
  }

  @Produces
  public Configuration defaultConfiguration() {
    return new ConfigurationBuilder()
            .jmxStatistics().enable()
            .clustering().cacheMode(CacheMode.REPL_SYNC).sync()
            .eviction()
            .strategy(EvictionStrategy.NONE)
            .build();
  }
}
