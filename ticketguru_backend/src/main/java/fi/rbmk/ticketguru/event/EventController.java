package fi.rbmk.ticketguru.event;

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

import fi.rbmk.ticketguru.eventType.*;
import fi.rbmk.ticketguru.eventOrganizer.*;
import fi.rbmk.ticketguru.eventTicket.*;
import fi.rbmk.ticketguru.venue.*;
import fi.rbmk.ticketguru.ageLimit.*;

@RestController
@RequestMapping(value = "/api/events", produces = "application/hal+json")
public class EventController {

    @Autowired
    EventRepository eRepository;
    @Autowired
    EventTypeRepository etRepository;
    @Autowired
    EventOrganizerRepository eoGroupRepository;
    @Autowired
    VenueRepository vRepository;
    @Autowired
    AgeLimitRepository alRepository;
    @Autowired
    EventTicketRepository eventTicketRepository;

    @PostMapping(produces = "application/hal+json")
    ResponseEntity<?> add(@Valid @RequestBody Event newEvent) {
        try {
            if (newEvent.getAgeLimit().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link AgeLimit that is marked as deleted");
            }
            if (newEvent.getEventType().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link EventType that is marked as deleted");
            }
            if (newEvent.getEventOrganizer().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link EventOrganizer that is marked as deleted");
            }
            if (newEvent.getVenue().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link Venue that is marked as deleted");
            }
            Event event = eRepository.save(newEvent);
            EventLinks links = new EventLinks(event);
            event.add(links.getAll());
            Resource<Event> resource = new Resource<Event>(event);
            return ResponseEntity.ok(resource);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Duplicate entry");
        }
    }

    @PatchMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> edit(@RequestBody Event newEvent, @PathVariable Long id) {
        Event event = eRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (event.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify Event that is marked as deleted");
        }
        if (newEvent.getName() != null && newEvent.getName() != "" && newEvent.getName() != event.getName()) {
            event.setName(newEvent.getName());
        }
        if (newEvent.getEventType() != null && newEvent.getEventType() != event.getEventType()) {
            if (newEvent.getEventType().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link EventType that is marked as deleted");
            }
            event.setEventType(newEvent.getEventType());
        }
        if (newEvent.getDateTime() != null && newEvent.getDateTime() != event.getDateTime()) {
            event.setDateTime(newEvent.getDateTime());
        }
        if (newEvent.getEventOrganizer() != null && newEvent.getEventOrganizer() != event.getEventOrganizer()) {
            if (newEvent.getEventOrganizer().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link EventOrganizer that is marked as deleted");
            }
            event.setEventOrganizer(newEvent.getEventOrganizer());
        }
        if (newEvent.getVenue() != null && newEvent.getVenue() != event.getVenue()) {
            if (newEvent.getVenue().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link Venue that is marked as deleted");
            }
            event.setVenue(newEvent.getVenue());
        }
        if (newEvent.getTicketCapacity() != null && newEvent.getTicketCapacity() != event.getTicketCapacity()) {
            event.setTicketCapacity(newEvent.getTicketCapacity());
        }
        if (newEvent.getAgeLimit() != null && newEvent.getAgeLimit() != event.getAgeLimit()) {
            if (newEvent.getAgeLimit().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link AgeLimit that is marked as deleted");
            }
            event.setAgeLimit(newEvent.getAgeLimit());
        }
        if (newEvent.getInfo() != null && newEvent.getInfo() != event.getInfo()) {
            event.setInfo(newEvent.getInfo());
        }
        eRepository.save(event);
        EventLinks links = new EventLinks(event);
        event.add(links.getAll());
        Resource<Event> resource = new Resource<Event>(event);
        return ResponseEntity.ok(resource);
    }

    @DeleteMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> delete(@PathVariable Long id) {
        Event event = eRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (event.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify Event that is marked as deleted");
        }
        event.setInvalid();
        eRepository.save(event);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = "application/hal+json")
    ResponseEntity<Resources<Event>> all() {
        List<Event> events = eRepository.findAll();
        Link link = linkTo(EventController.class).withSelfRel();
        if (events.size() != 0) {
            for (Event event : events) {
                EventLinks links = new EventLinks(event);
                event.add(links.getAll());
            }
            Resources<Event> resources = new Resources<Event>(events, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<Resource<Event>> one(@PathVariable Long id) {
        Event event = eRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        EventLinks links = new EventLinks(event);
        event.add(links.getAll());
        Resource<Event> resource = new Resource<Event>(event);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/eventType", produces = "application/hal+json")
    ResponseEntity<Resource<EventType>> getEventType(@PathVariable Long id) {
        Event event = eRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        EventType eventType = event.getEventType();
        EventTypeLinks links = new EventTypeLinks(eventType);
        eventType.add(links.getAll());
        Resource<EventType> resource = new Resource<EventType>(eventType);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/eventOrganizer", produces = "application/hal+json")
    ResponseEntity<Resource<EventOrganizer>> getEventOrganizer(@PathVariable Long id) {
        Event event = eRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        EventOrganizer eventOrganizer = event.getEventOrganizer();
        EventOrganizerLinks links = new EventOrganizerLinks(eventOrganizer);
        eventOrganizer.add(links.getAll());
        Resource<EventOrganizer> resource = new Resource<EventOrganizer>(eventOrganizer);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/venue", produces = "application/hal+json")
    ResponseEntity<Resource<Venue>> getVenue(@PathVariable Long id) {
        Event event = eRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        Venue venue = event.getVenue();
        VenueLinks links = new VenueLinks(venue);
        venue.add(links.getAll());
        Resource<Venue> resource = new Resource<Venue>(venue);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/ageLimit", produces = "application/hal+json")
    ResponseEntity<Resource<AgeLimit>> getAgeLimit(@PathVariable Long id) {
        Event event = eRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        AgeLimit ageLimit = event.getAgeLimit();
        AgeLimitLinks links = new AgeLimitLinks(ageLimit);
        ageLimit.add(links.getAll());
        Resource<AgeLimit> resource = new Resource<AgeLimit>(ageLimit);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/eventTickets", produces = "application/hal+json")
    ResponseEntity<?> getEventTickets(@PathVariable Long id) {
        Event event = eRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        Link link = linkTo(EventController.class).withSelfRel();
        List<EventTicket> eventTickets = event.getEventTickets();
        if (eventTickets.size() != 0) {
            for (EventTicket eventTicket : eventTickets) {
                EventTicketLinks links = new EventTicketLinks(eventTicket);
                eventTicket.add(links.getAll());
            }
            Resources<EventTicket> resources = new Resources<EventTicket>(eventTickets, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}