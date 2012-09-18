package org.jboss.jdf.example.ticketmonster.datagrid;

import javax.inject.Inject;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.jdf.example.ticketmonster.model.Cart;

import java.util.concurrent.TimeUnit;

/**
 * @author Marius Bogoevici
 */
public class DatagridCartStore implements CartStore {

    public static final String CARTS_CACHE = "carts";

    private final Cache<String, Cart> cartsCache;

    @Inject
    public DatagridCartStore(EmbeddedCacheManager manager) {

        this.cartsCache = manager.getCache(CARTS_CACHE);
        this.cartsCache.addListener(new CartEntryListener());
    }

    @Override
    public Cart getCart(String cartId) {
        return this.cartsCache.get(cartId);
    }

    @Override
    public void saveCart(Cart cart) {

        this.cartsCache.put(cart.getId(), cart, 10, TimeUnit.MINUTES);
    }

    @Override
    public void delete(Cart cart) {
        this.cartsCache.remove(cart.getId());
    }
}
