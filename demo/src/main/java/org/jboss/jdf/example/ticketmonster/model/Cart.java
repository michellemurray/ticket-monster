package org.jboss.jdf.example.ticketmonster.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jboss.jdf.example.ticketmonster.rest.TicketRequest;

/**
 * @author Marius Bogoevici
 */
public class Cart implements Serializable  {

    private String id;

    private Performance performance;

    private ArrayList<SeatAllocation> seatAllocations = new ArrayList<SeatAllocation>();

    public static class SeatAllocation {
       private TicketRequest ticketRequest;
       private ArrayList<Seat> allocatedSeats;

        public SeatAllocation(TicketRequest ticketRequest, ArrayList<Seat> allocatedSeats) {
            this.ticketRequest = ticketRequest;
            this.allocatedSeats = allocatedSeats;
        }

        public TicketRequest getTicketRequest() {
            return ticketRequest;
        }

        public ArrayList<Seat> getAllocatedSeats() {
            return allocatedSeats;
        }
    }

    private Cart() {
    }

    private Cart(String id) {
        this.id = id;
    }

    public static Cart initialize() {
        return new Cart(UUID.randomUUID().toString());
    }

    public String getId() {
        return id;
    }

    public Performance getPerformance() {
        return performance;
    }

    public void setPerformance(Performance performance) {
        this.performance = performance;
    }

    public ArrayList<SeatAllocation> getSeatAllocations() {
        return seatAllocations;
    }
}
