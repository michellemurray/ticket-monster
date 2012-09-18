package org.jboss.jdf.example.ticketmonster.datagrid;

import org.jboss.jdf.example.ticketmonster.model.Cart;

/**
 * @author Marius Bogoevici
 */
public interface CartStore {

    public Cart getCart(String id);

    void saveCart(Cart cart);

    void delete(Cart cart);
}
