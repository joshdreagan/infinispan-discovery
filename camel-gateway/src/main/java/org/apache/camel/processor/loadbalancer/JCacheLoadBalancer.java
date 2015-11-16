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
package org.apache.camel.processor.loadbalancer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.Service;
import org.apache.camel.support.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class JCacheLoadBalancer extends ServiceSupport implements LoadBalancer, CamelContextAware, InitializingBean {

  private final Logger log = LoggerFactory.getLogger(JCacheLoadBalancer.class);

  private static final LoadBalancer DEFAULT_DELEGATE = new RoundRobinLoadBalancer();

  private Cache<String, Set<String>> registry;
  private String groupId;
  private LoadBalancer delegate;
  private UriPreProcessor uriPreProcessor;
  private Boolean throwExceptionIfEmpty;

  private Map<String, Processor> processorMap;
  private CacheEntryListenerConfiguration<String, Set<String>> registryListenerConfiguration;

  private CamelContext camelContext;

  public void setRegistry(Cache<String, Set<String>> registry) {
    this.registry = registry;
  }

  public Cache<String, Set<String>> getRegistry() {
    return registry;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setDelegate(LoadBalancer delegate) {
    this.delegate = delegate;
  }

  public LoadBalancer getDelegate() {
    return delegate;
  }

  public UriPreProcessor getUriPreProcessor() {
    return uriPreProcessor;
  }

  public void setUriPreProcessor(UriPreProcessor uriPreProcessor) {
    this.uriPreProcessor = uriPreProcessor;
  }

  public Boolean isThrowExceptionIfEmpty() {
    return throwExceptionIfEmpty;
  }

  public void setThrowExceptionIfEmpty(Boolean throwExceptionIfEmpty) {
    this.throwExceptionIfEmpty = throwExceptionIfEmpty;
  }

  @Override
  public void addProcessor(Processor processor) {
    delegate.addProcessor(processor);
  }

  @Override
  public void removeProcessor(Processor processor) {
    delegate.removeProcessor(processor);
  }

  @Override
  public List<Processor> getProcessors() {
    return delegate.getProcessors();
  }

  @Override
  public boolean process(Exchange exchange, AsyncCallback callback) {
    if ((getProcessors() == null || getProcessors().isEmpty()) && throwExceptionIfEmpty) {
      if (throwExceptionIfEmpty) {
        exchange.setException(new LoadBalancerUnavailableException(String.format("No URIs found for service '%s'.", groupId)));
      }
      callback.done(true);
      return true;
    } else {
      return delegate.process(exchange, callback);
    }
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    if ((getProcessors() == null || getProcessors().isEmpty()) && throwExceptionIfEmpty) {
      throw new LoadBalancerUnavailableException(String.format("No URIs found for service '%s'.", groupId));
    }
    delegate.process(exchange);
  }

  @Override
  protected void doStart() throws Exception {
    if (delegate == DEFAULT_DELEGATE) {
      ((Service) delegate).start();
    }
    registry.registerCacheEntryListener(registryListenerConfiguration);
    processUris(registry.get(groupId));
  }

  @Override
  protected void doStop() throws Exception {
    registry.deregisterCacheEntryListener(registryListenerConfiguration);
    if (delegate == DEFAULT_DELEGATE) {
      ((Service) delegate).stop();
    }
  }

  @Override
  public void setCamelContext(CamelContext camelContext) {
    this.camelContext = camelContext;
  }

  @Override
  public CamelContext getCamelContext() {
    return camelContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Objects.requireNonNull(registry, "The registry property must not be null.");
    Objects.requireNonNull(groupId, "The groupId property must not be null.");
    Objects.requireNonNull(camelContext, "The camelContext property must not be null.");

    if (delegate == null) {
      delegate = DEFAULT_DELEGATE;
    }

    if (throwExceptionIfEmpty == null) {
      throwExceptionIfEmpty = true;
    }

    processorMap = new HashMap<>();
    registryListenerConfiguration = new MutableCacheEntryListenerConfiguration<>(new LookupCacheListenerFactory(), null, false, false);
  }

  private void processUris(Set<String> uris) throws Exception {
    if (uris == null) {
      uris = new HashSet<>();
    }
    for (String uri : processorMap.keySet()) {
      if (!uris.contains(uri)) {
        log.info(String.format("Removing uri: %s", uri));
        Processor processor = processorMap.remove(uri);
        removeProcessor(processor);
        if (processor instanceof Producer) {
          camelContext.removeEndpoint(((Producer) processor).getEndpoint());
        }
      }
    }
    for (String uri : uris) {
      if (!processorMap.containsKey(uri)) {
        log.info(String.format("Adding uri: %s", uri));
        String processedUri = uri;
        if (uriPreProcessor != null) {
          processedUri = uriPreProcessor.process(uri);
        }
        Processor processor = camelContext.getEndpoint(processedUri).createProducer();
        processorMap.put(uri, processor);
        addProcessor(processor);
      }
    }
  }

  private class LookupCacheListener implements CacheEntryCreatedListener<String, Set<String>>,
          CacheEntryUpdatedListener<String, Set<String>>,
          CacheEntryRemovedListener<String, Set<String>>,
          CacheEntryExpiredListener<String, Set<String>>,
          Serializable {

    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends String, ? extends Set<String>>> events) throws CacheEntryListenerException {
      onEvent(events);
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends Set<String>>> events) throws CacheEntryListenerException {
      onEvent(events);
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends String, ? extends Set<String>>> events) throws CacheEntryListenerException {
      onEvent(events);
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends String, ? extends Set<String>>> events) throws CacheEntryListenerException {
      onEvent(events);
    }

    public void onEvent(Iterable<CacheEntryEvent<? extends String, ? extends Set<String>>> events) throws CacheEntryListenerException {
      for (CacheEntryEvent<? extends String, ? extends Set<String>> event : events) {
        log.debug(String.format("Got a cache event: %s", event));
        try {
          processUris(event.getValue());
        } catch (Exception e) {
          throw new CacheEntryListenerException(e);
        }
      }
    }
  }

  private class LookupCacheListenerFactory implements Factory<LookupCacheListener> {

    @Override
    public LookupCacheListener create() {
      return new LookupCacheListener();
    }
  }
}
