package org.jboss.jdf.example.ticketmonster.service;

import java.util.List;

import org.jboss.jdf.example.ticketmonster.model.Performance;
import org.jboss.jdf.example.ticketmonster.model.Seat;
import org.jboss.jdf.example.ticketmonster.model.Section;

/**
 * @author Marius Bogoevici
 */
public interface SeatAllocationService {

    AllocatedSeats allocateSeats(Section section, Performance performance, int seatCount, boolean contiguous);

    void deallocateSeats(Section section, Performance performance, List<Seat> seats);

    void finalizeAllocation(AllocatedSeats allocatedSeats);

    void finalizeAllocation(Performance performance, List<Seat> allocatedSeats);
}
