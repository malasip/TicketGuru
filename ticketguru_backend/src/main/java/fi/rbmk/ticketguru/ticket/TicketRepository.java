package fi.rbmk.ticketguru.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import fi.rbmk.ticketguru.eventTicket.EventTicket;
import fi.rbmk.ticketguru.saleEvent.SaleEvent;

public interface TicketRepository extends CrudRepository<Ticket, Long> {

    Ticket findByChecksum(String checksum);
    List<Ticket> findAll();
    List<Ticket> findBySaleRow_SaleEvent(SaleEvent saleEvent);
    List<Ticket> findByEventTicket(EventTicket eventTicket);

    @Query("select t from Ticket t where t.invalid is null")
    List<Ticket> findAllValid();
}