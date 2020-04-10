package fi.rbmk.ticketguru.ticket;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.rbmk.ticketguru.eventTicket.EventTicket;
import fi.rbmk.ticketguru.eventTicket.EventTicketLinks;
import fi.rbmk.ticketguru.eventTicket.EventTicketRepository;
import fi.rbmk.ticketguru.saleRow.SaleRow;
import fi.rbmk.ticketguru.saleRow.SaleRowLinks;
import fi.rbmk.ticketguru.ticketStatus.*;

@RestController
@RequestMapping(value = "/api/tickets", produces = "application/hal+json")
public class TicketController {

    @Autowired
    TicketRepository tRepository;
    @Autowired
    TicketStatusRepository tSRepository;
    @Autowired
    EventTicketRepository etRepository;
    @Autowired
    TicketService tService;

    @PatchMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> edit(@RequestBody Ticket newTicket, @PathVariable Long id) {
        Ticket ticket = tRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (ticket.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify Ticket that is marked as deleted");
        }
        if (newTicket.getTicketStatus() != null && newTicket.getTicketStatus() != ticket.getTicketStatus()) {
            ticket.setTicketStatus(newTicket.getTicketStatus());
        }
        tRepository.save(ticket);
        TicketLinks links = new TicketLinks(ticket);
        ticket.add(links.getAll());
        Resource<Ticket> resource = new Resource<Ticket>(ticket);
        return ResponseEntity.ok(resource);
    }

    @DeleteMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> delete(@PathVariable Long id) {
        Ticket ticket = tRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (ticket.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify Ticket that is marked as deleted");
        }
        ticket.setInvalid();
        tRepository.save(ticket);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = "application/hal+json")
    ResponseEntity<Resources<Ticket>> all() {
        List<Ticket> tickets = tRepository.findAll();
        Link link = linkTo(TicketController.class).withSelfRel();
        if (tickets.size() != 0) {
            for (Ticket ticket : tickets) {
                TicketLinks links = new TicketLinks(ticket);
                ticket.add(links.getAll());
            }
            Resources<Ticket> resources = new Resources<Ticket>(tickets, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping(value = "/{s}", produces = "application/hal+json")
    public ResponseEntity<Resource<Ticket>> one(@PathVariable String s) {
        Ticket ticket = null;
        try {
            Long id = Long.parseLong(s);
            ticket = tRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        } catch (NumberFormatException e) {
            ticket = tRepository.findByCheckSum(s);
            if (ticket == null) {
                throw new ResourceNotFoundException("Invalid Checksum: " + s);
            }
        }
        TicketLinks ticketLinks = new TicketLinks(ticket);
        ticket.add(ticketLinks.getAll());
        Resource<Ticket> resource = new Resource<Ticket>(ticket);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/ticketStatus", produces = "application/hal+json")
    ResponseEntity<Resource<TicketStatus>> getTicketStatus(@PathVariable Long id) {
        Ticket ticket = tRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        TicketStatus ticketStatus = ticket.getTicketStatus();
        TicketStatusLinks links = new TicketStatusLinks(ticketStatus);
        ticketStatus.add(links.getAll());
        Resource<TicketStatus> resource = new Resource<TicketStatus>(ticketStatus);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/eventTicket", produces = "application/hal+json")
    ResponseEntity<Resource<EventTicket>> getEventTicket(@PathVariable Long id) {
        Ticket ticket = tRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        EventTicket eventTicket = ticket.getEventTicket();
        EventTicketLinks links = new EventTicketLinks(eventTicket);
        eventTicket.add(links.getAll());
        Resource<EventTicket> resource = new Resource<EventTicket>(eventTicket);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/validate/{checksum}", produces = "application/hal+json")
    public ResponseEntity<Resource<Ticket>> validate(@PathVariable String checksum) {
        List<Object> result = tService.validate(checksum);
        if (result.size() > 1) {
            Resource<Ticket> resource = new Resource<Ticket> ((Ticket) result.get(0));
            return ResponseEntity.badRequest().header("ErrorMsg", result.get(1).toString()).body(resource);
        }
        Resource<Ticket> resource = new Resource<Ticket>((Ticket) result.get(0));
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/saleRow", produces = "application/hal+json")
    ResponseEntity<Resource<SaleRow>> getSaleRow(@PathVariable Long id) {
        Ticket ticket = tRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        SaleRow saleRow = ticket.getSaleRow();
        SaleRowLinks links = new SaleRowLinks(saleRow);
        saleRow.add(links.getAll());
        Resource<SaleRow> resource = new Resource<SaleRow>(saleRow);
        return ResponseEntity.ok(resource);
    }
}