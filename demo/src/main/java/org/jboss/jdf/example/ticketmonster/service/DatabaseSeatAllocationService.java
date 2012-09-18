package org.jboss.jdf.example.ticketmonster.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;

import org.jboss.jdf.example.ticketmonster.model.Performance;
import org.jboss.jdf.example.ticketmonster.model.Seat;
import org.jboss.jdf.example.ticketmonster.model.SeatAllocationException;
import org.jboss.jdf.example.ticketmonster.model.Section;
import org.jboss.jdf.example.ticketmonster.model.SectionAllocation;

/**
 * Default implementation of the {@link SeatAllocationService}
 *
 * @author Marius Bogoevici
 */
@SuppressWarnings("serial")
public class DatabaseSeatAllocationService implements Serializable, SeatAllocationService {

    @Inject
    EntityManager entityManager;

    @Override
    public AllocatedSeats allocateSeats(Section section, Performance performance, int seatCount, boolean contiguous) {
        SectionAllocation sectionAllocation = retrieveSectionAllocationExclusively(section, performance);
        ArrayList<Seat> seats = sectionAllocation.allocateSeats(seatCount, contiguous);
        return new AllocatedSeats(sectionAllocation, seats);
    }

    @Override
    public void deallocateSeats(Section section, Performance performance, List<Seat> seats) {
        SectionAllocation sectionAllocation = retrieveSectionAllocationExclusively(section, performance);
        for (Seat seat : seats) {
            if (!seat.getSection().equals(section)) {
                throw new SeatAllocationException("All seats must be in the same section!");
            }
            sectionAllocation.deallocate(seat);
        }
    }

    @Override
    public void finalizeAllocation(AllocatedSeats allocatedSeats) {
       allocatedSeats.markOccupied();
    }

    @Override
    public void finalizeAllocation(Performance performance, List<Seat> allocatedSeats) {
        SectionAllocation sectionAllocation = retrieveSectionAllocationExclusively(allocatedSeats.get(0).getSection(), performance);
        sectionAllocation.markOccupied(allocatedSeats);
    }

    private SectionAllocation retrieveSectionAllocationExclusively(Section section, Performance performance) {
        SectionAllocation sectionAllocationStatus = (SectionAllocation) entityManager.createQuery(
													"select s from SectionAllocation s where " +
													"s.performance.id = :performanceId and " +
													"s.section.id = :sectionId")
													.setParameter("performanceId", performance.getId())
													.setParameter("sectionId", section.getId())
													.getSingleResult();
        entityManager.lock(sectionAllocationStatus, LockModeType.PESSIMISTIC_WRITE);
        return sectionAllocationStatus;
    }
}