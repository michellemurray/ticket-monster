package org.jboss.jdf.example.ticketmonster.rest;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.jdf.example.ticketmonster.datagrid.CartEntryListener;
import org.jboss.jdf.example.ticketmonster.datagrid.CartStore;
import org.jboss.jdf.example.ticketmonster.model.Booking;
import org.jboss.jdf.example.ticketmonster.model.Cart;
import org.jboss.jdf.example.ticketmonster.model.Performance;
import org.jboss.jdf.example.ticketmonster.model.Seat;
import org.jboss.jdf.example.ticketmonster.model.Ticket;
import org.jboss.jdf.example.ticketmonster.model.TicketPrice;
import org.jboss.jdf.example.ticketmonster.monitor.client.shared.qualifier.Created;
import org.jboss.jdf.example.ticketmonster.service.AllocatedSeats;
import org.jboss.jdf.example.ticketmonster.service.SeatAllocationService;

/**
 * @author Marius Bogoevici
 */
@Path("/carts")
@Stateless
public class CartService {

    public static final String CARTS_CACHE = "CARTS";

    @Inject
    private CartStore cartStore;

    @Inject
    private EntityManager entityManager;

    @Inject
    private BookingService bookingService;

    @Inject
    private SeatAllocationService seatAllocationService;

    @Inject @Created
    private javax.enterprise.event.Event<Booking> newBookingEvent;


    @POST
    public Cart openCart(Map<String, String> data) {
        Cart cart = Cart.initialize();
        cart.setPerformance(entityManager.find(Performance.class, Long.parseLong(data.get("performance"))));
        cartStore.saveCart(cart);
        return cart;
    }

    @GET
    @Path("/{id}")
    public Cart getCart(String id) {
      return cartStore.getCart(id);
    }

    @POST
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Cart addTicketRequest(@PathParam("id") String id, TicketReservationRequest... ticketRequests){
        Cart cart = cartStore.getCart(id);

        for (TicketReservationRequest ticketRequest : ticketRequests) {
            TicketPrice ticketPrice = entityManager.find(TicketPrice.class, ticketRequest.getTicketPrice());
            Iterator<Cart.SeatAllocation> iterator = cart.getSeatAllocations().iterator();
            while (iterator.hasNext()) {
                Cart.SeatAllocation seatAllocation = iterator.next();
                if (seatAllocation.getTicketRequest().getTicketPrice().getId().equals(ticketRequest.getTicketPrice())){
                    seatAllocationService.deallocateSeats(ticketPrice.getSection(), cart.getPerformance(), seatAllocation.getAllocatedSeats());
                    ticketRequest.setQuantity(ticketRequest.getQuantity() + seatAllocation.getTicketRequest().getQuantity());
                    iterator.remove();
                }
            }
            if (ticketRequest.getQuantity() > 0 ) {
            AllocatedSeats allocatedSeats = seatAllocationService.allocateSeats(ticketPrice.getSection(), cart.getPerformance(), ticketRequest.getQuantity(), true);
            cart.getSeatAllocations().add(new Cart.SeatAllocation(new TicketRequest(ticketPrice, ticketRequest.getQuantity()), allocatedSeats.getSeats()));
            }
        }
        return cart;
    }


    /**
     * <p>
     * Create a booking. Data is contained in the bookingRequest object
     * </p>
     *
     * @param cartId
     * @param data
     * @return
     */
    @SuppressWarnings("unchecked")
    @POST
    /**
     * <p> Data is received in JSON format. For easy handling, it will be unmarshalled in the support
     * {@link BookingRequest} class.
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}/checkout")
    public Response createBookingFromCart(@PathParam("id") String cartId, Map<String, String> data) {
        try {
            // identify the ticket price categories in this request


            Cart cart = cartStore.getCart(cartId);

            // load the entities that make up this booking's relationships

            // Now, start to create the booking from the posted data
            // Set the simple stuff first!
            Booking booking = new Booking();
            booking.setContactEmail(data.get("email"));
            booking.setPerformance(cart.getPerformance());
            booking.setCancellationCode("abc");

            List<Seat> seats = new ArrayList<Seat>();
            for (Cart.SeatAllocation seatAllocation : cart.getSeatAllocations()) {
                for (Seat seat : seatAllocation.getAllocatedSeats()) {
                    TicketPrice ticketPrice = seatAllocation.getTicketRequest().getTicketPrice();
                    booking.getTickets().add(new Ticket(seat, ticketPrice.getTicketCategory(), ticketPrice.getPrice()));
                }
                seatAllocationService.finalizeAllocation(cart.getPerformance(), seatAllocation.getAllocatedSeats());
            }

            booking.setCancellationCode("abc");
            entityManager.persist(booking);
            cartStore.delete(cart);
            newBookingEvent.fire(booking);
            return Response.ok().entity(booking).type(MediaType.APPLICATION_JSON_TYPE).build();

        } catch (ConstraintViolationException e) {
            // If validation of the data failed using Bean Validation, then send an error
            Map<String, Object> errors = new HashMap<String, Object>();
            List<String> errorMessages = new ArrayList<String>();
            for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                errorMessages.add(constraintViolation.getMessage());
            }
            errors.put("errors", errorMessages);
            // A WebApplicationException can wrap a response
            // Throwing the exception causes an automatic rollback
            throw new RestServiceException(Response.status(Response.Status.BAD_REQUEST).entity(errors).build());
        } catch (Exception e) {
            // Finally, handle unexpected exceptions
            Map<String, Object> errors = new HashMap<String, Object>();
            errors.put("errors", Collections.singletonList(e.getMessage()));
            // A WebApplicationException can wrap a response
            // Throwing the exception causes an automatic rollback
            throw new RestServiceException(Response.status(Response.Status.BAD_REQUEST).entity(errors).build());
        }
    }
}
