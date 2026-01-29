package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {

    List<Ticket> findByTenantId(String tenantId);

    List<Ticket> findByRoomId(String roomId);

    List<Ticket> findByHouseId(String houseId);

    List<Ticket> findByHouseIdAndStatus(String houseId, Ticket.TicketStatus status);

    @Query("SELECT t FROM Ticket t JOIN House h ON h.id = t.houseId WHERE h.ownerId = :landlordId")
    List<Ticket> findByLandlordId(@Param("landlordId") String landlordId);

    @Query("SELECT t FROM Ticket t JOIN House h ON h.id = t.houseId WHERE h.ownerId = :landlordId AND t.status = :status")
    List<Ticket> findByLandlordIdAndStatus(@Param("landlordId") String landlordId, @Param("status") Ticket.TicketStatus status);
}
