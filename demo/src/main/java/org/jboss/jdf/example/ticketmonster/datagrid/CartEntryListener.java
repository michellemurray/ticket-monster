package org.jboss.jdf.example.ticketmonster.datagrid;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.jboss.jdf.example.ticketmonster.model.Cart;
import org.jboss.jdf.example.ticketmonster.model.Event;

@Listener
public class CartEntryListener {

    @CacheEntryCreated
    public void onCreation(CacheEntryCreatedEvent<String, Cart> entry) {
        System.out.println(entry.getKey());
    }

    @CacheEntryRemoved
    public void onRemoval(CacheEntryRemovedEvent<String, Cart> event) {
         System.out.println("Entry removed from da cache " + event.getKey()) ;
    }


    @CacheEntriesEvicted
    public void onEviction(CacheEntriesEvictedEvent<String, Cart> event) {
        System.out.println("Entries evicted from da cache ");
        for (Cart cart : event.getEntries().values()) {
            System.out.println(cart.getId());
        }
    }
}
