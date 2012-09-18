package org.jboss.jdf.example.ticketmonster.datagrid;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.JBossTransactionManagerLookup;
import org.jboss.jdf.example.ticketmonster.model.*;
import org.jboss.jdf.example.ticketmonster.service.AllocatedSeats;
import org.jboss.jdf.example.ticketmonster.service.SeatAllocationService;

/**
 * @author Marius Bogoevici
 */
@Alternative
public class DatagridSeatAllocationService implements SeatAllocationService {


    public static final String ALLOCATIONS = "TICKETMONSTER_ALLOCATIONS";

    private Cache<SectionAllocationKey, SectionAllocation> cache;

    @Inject
    public DatagridSeatAllocationService(EmbeddedCacheManager manager) {

        Configuration allocation = new ConfigurationBuilder()
                .transaction().transactionMode(TransactionMode.TRANSACTIONAL)
                .transactionManagerLookup(new JBossTransactionManagerLookup()).lockingMode(LockingMode.PESSIMISTIC)
                .loaders().addFileCacheStore().purgeOnStartup(true)
                .build();
        manager.defineConfiguration("allocation", allocation);
        this.cache = manager.getCache("allocation");
    }

    @Override
    public AllocatedSeats allocateSeats(Section section, Performance performance, int seatCount, boolean contiguous) {
        SectionAllocationKey sectionAllocationKey = SectionAllocationKey.of(section, performance);
        cache.putIfAbsent(sectionAllocationKey, new SectionAllocation(performance, section));
        cache.getAdvancedCache().lock(sectionAllocationKey);
        SectionAllocation allocation = cache.get(sectionAllocationKey);
        ArrayList<Seat> seats = allocation.allocateSeats(seatCount, contiguous);
        cache.put(sectionAllocationKey, allocation);
        return new AllocatedSeats(allocation, seats);
    }

    @Override
    public void deallocateSeats(Section section, Performance performance, List<Seat> seats) {
        cache.putIfAbsent(SectionAllocationKey.of(section, performance), new SectionAllocation());
        SectionAllocation sectionAllocation = cache.get(SectionAllocationKey.of(section, performance));
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
            SectionAllocation sectionAllocation = cache.get(SectionAllocationKey.of(allocatedSeats.get(0).getSection(), performance));
            sectionAllocation.markOccupied(allocatedSeats);
    }
}
