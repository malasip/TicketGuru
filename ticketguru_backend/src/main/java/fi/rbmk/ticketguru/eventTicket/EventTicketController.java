package fi.rbmk.ticketguru.eventTicket;

import java.util.List;

import javax.validation.Valid;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.rbmk.ticketguru.event.*;
import fi.rbmk.ticketguru.ticket.*;
import fi.rbmk.ticketguru.ticketType.*;

@RestController
@RequestMapping(value = "/api/eventTickets", produces = "application/hal+json")
public class EventTicketController {

    @Autowired
    EventTicketRepository eTRepository;
    @Autowired
    EventRepository eRepository;
    @Autowired
    TicketRepository tRepository;
    @Autowired
    TicketTypeRepository ttGroupRepository;

    @PostMapping(produces = "application/hal+json")
    ResponseEntity<?> add(@Valid @RequestBody EventTicket newEventTicket) {
        try {
            if (newEventTicket.getEvent().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link Event that is marked as deleted");
            }
            if (newEventTicket.getTicketType().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link TicketType that is marked as deleted");
            }
            EventTicket eventTicket = eTRepository.save(newEventTicket);
            EventTicketLinks links = new EventTicketLinks(eventTicket);
            eventTicket.add(links.getAll());
            Resource<EventTicket> resource = new Resource<EventTicket>(eventTicket);
            return ResponseEntity.ok(resource);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Duplicate entry");
        }
    }

    @PatchMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> edit(@RequestBody EventTicket newEventTicket, @PathVariable Long id) {
        EventTicket eventTicket = eTRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (eventTicket.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify EventTicket that is marked as deleted");
        }
        if (newEventTicket.getEvent() != null && newEventTicket.getEvent() != eventTicket.getEvent()) {
            if (newEventTicket.getEvent().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link Event that is marked as deleted");
            }
            eventTicket.setEvent(newEventTicket.getEvent());
        }
        if (newEventTicket.getTicketType() != null && newEventTicket.getTicketType() != eventTicket.getTicketType()) {
            if (newEventTicket.getTicketType().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link TicketType that is marked as deleted");
            }
            eventTicket.setTicketType(newEventTicket.getTicketType());
        }
        if (newEventTicket.getTicketCount() != null && newEventTicket.getTicketCount() != eventTicket.getTicketCount()) {
            eventTicket.setTicketCount(newEventTicket.getTicketCount());
        }
        if (newEventTicket.getPrice() != null && newEventTicket.getPrice() != eventTicket.getPrice()) {
            eventTicket.setPrice(newEventTicket.getPrice());
        }
        eTRepository.save(eventTicket);
        EventTicketLinks links = new EventTicketLinks(eventTicket);
        eventTicket.add(links.getAll());
        Resource<EventTicket> resource = new Resource<EventTicket>(eventTicket);
        return ResponseEntity.ok(resource);
    }

    @DeleteMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> delete(@PathVariable Long id) {
        EventTicket eventTicket = eTRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (eventTicket.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify EventTicket that is marked as deleted");
        }
        eventTicket.setInvalid();
        eTRepository.save(eventTicket);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = "application/hal+json")
    ResponseEntity<Resources<EventTicket>> all() {
        List<EventTicket> eventTickets = eTRepository.findAll();
        Link link = linkTo(EventTicketController.class).withSelfRel();
        if (eventTickets.size() != 0) {
            for (EventTicket eventTicket : eventTickets) {
                EventTicketLinks links = new EventTicketLinks(eventTicket);
                eventTicket.add(links.getAll());
            }
            Resources<EventTicket> resources = new Resources<EventTicket>(eventTickets, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping(value = "/{id}", produces = "application/hal+json")
    public ResponseEntity<Resource<EventTicket>> one(@PathVariable Long id) {
        EventTicket eventTicket = eTRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
            EventTicketLinks links = new EventTicketLinks(eventTicket);
            eventTicket.add(links.getAll());
        Resource<EventTicket> resource = new Resource<EventTicket>(eventTicket);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/event", produces = "application/hal+json")
    ResponseEntity<Resource<Event>> getEvent(@PathVariable Long id) {
        EventTicket eventTicket = eTRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        Event event = eventTicket.getEvent();
        EventLinks links = new EventLinks(event);
        event.add(links.getAll());
        Resource<Event> resource = new Resource<Event>(event);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/ticketType", produces = "application/hal+json")
    ResponseEntity<Resource<TicketType>> getTicketType(@PathVariable Long id) {
        EventTicket eventTicket = eTRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        TicketType ticketType = eventTicket.getTicketType();
        TicketTypeLinks links = new TicketTypeLinks(ticketType);
        ticketType.add(links.getAll());
        Resource<TicketType> resource = new Resource<TicketType>(ticketType);
        return ResponseEntity.ok(resource);
    }
}