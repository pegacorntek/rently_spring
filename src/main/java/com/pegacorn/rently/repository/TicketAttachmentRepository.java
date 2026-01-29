package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, String> {

    List<TicketAttachment> findByTicketId(String ticketId);

    @Modifying
    @Query("DELETE FROM TicketAttachment ta WHERE ta.ticketId = :ticketId")
    void deleteByTicketId(@Param("ticketId") String ticketId);
}
