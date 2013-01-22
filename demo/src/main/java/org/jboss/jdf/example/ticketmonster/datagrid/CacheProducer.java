/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.jdf.example.ticketmonster.datagrid;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.jdf.example.ticketmonster.model.Cart;
import org.jboss.jdf.example.ticketmonster.model.SectionAllocation;


/**
 *
 * 
 * @author Martin Gencur
 * 
 */
@ApplicationScoped
public class CacheProducer {
    private Logger log = Logger.getLogger(this.getClass().getName());

    private EmbeddedCacheManager manager;

    @Produces @ApplicationScoped
    public EmbeddedCacheManager getCacheContainer() {
        if (manager == null) {
            GlobalConfiguration glob = new GlobalConfigurationBuilder()
                    .nonClusteredDefault() //Helper method that gets you a default constructed GlobalConfiguration, preconfigured for use in LOCAL mode
                    .globalJmxStatistics().enable() //This method allows enables the jmx statistics of the global configuration.
                    .transport().defaultTransport()
                    .build(); //Builds  the GlobalConfiguration object
            Configuration loc = new ConfigurationBuilder()
                    .jmxStatistics().enable() //Enable JMX statistics
                    .clustering().cacheMode(CacheMode.LOCAL) //Set Cache mode to LOCAL - Data is not replicated.
                    .transaction().transactionMode(TransactionMode.TRANSACTIONAL).transactionManagerLookup(new GenericTransactionManagerLookup())
                    .lockingMode(LockingMode.PESSIMISTIC)
                    .locking().isolationLevel(IsolationLevel.REPEATABLE_READ) //Sets the isolation level of locking
                    .eviction().maxEntries(4).strategy(EvictionStrategy.LIRS) //Sets  4 as maximum number of entries in a cache instance and uses the LIRS strategy - an efficient low inter-reference recency set replacement policy to improve buffer cache performance
                    .loaders().passivation(false).addFileCacheStore().purgeOnStartup(true) //Disable passivation and adds a FileCacheStore that is Purged on Startup
                    .build(); //Builds the Configuration object
            manager = new DefaultCacheManager(glob, loc, true);
            log.info("=== Using DefaultCacheManager (library mode) ===");
        }
        return manager;
    }

    @PreDestroy
    public void cleanUp() {
        manager.stop();
        manager = null;
    }
}
