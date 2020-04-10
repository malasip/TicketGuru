package fi.rbmk.ticketguru.eventOrganizer;

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

import fi.rbmk.ticketguru.postcode.*;
import fi.rbmk.ticketguru.event.*;

@RestController
@RequestMapping(value = "/api/eventOrganizers", produces = "application/hal+json")
public class EventOrganizerController {

    @Autowired EventOrganizerRepository eoRepository;
    @Autowired PostcodeRepository pRepository;
    @Autowired EventRepository eRepository;

    @PostMapping(produces = "application/hal+json")
    ResponseEntity<?> add(@Valid @RequestBody EventOrganizer newEventOrganizer) {
        try {
            if (newEventOrganizer.getPostcode().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link entity that is marked as deleted");
            }
            EventOrganizer eventOrganizer = eoRepository.save(newEventOrganizer);
            EventOrganizerLinks links = new EventOrganizerLinks(eventOrganizer);
            eventOrganizer.add(links.getAll());
            Resource<EventOrganizer> resource = new Resource<EventOrganizer>(eventOrganizer);
            return ResponseEntity.ok(resource);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Duplicate entry");
        }
    }

    @PatchMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> edit(@RequestBody EventOrganizer newEventOrganizer, @PathVariable Long id) {
        EventOrganizer eventOrganizer = eoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (eventOrganizer.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify EventOrganizer that is marked as deleted");
        }
        if(newEventOrganizer.getName() != null && newEventOrganizer.getName() != "" && newEventOrganizer.getName() != eventOrganizer.getName()) {
            eventOrganizer.setName(newEventOrganizer.getName());
        }
        if(newEventOrganizer.getStreetAddress() != null && newEventOrganizer.getStreetAddress() != eventOrganizer.getStreetAddress()) {
            eventOrganizer.setStreetAddress(newEventOrganizer.getStreetAddress());
        }
        if(newEventOrganizer.getPostcode() != null && newEventOrganizer.getPostcode() != eventOrganizer.getPostcode()) {
            if (newEventOrganizer.getPostcode().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link Postcode that is marked as deleted");
            }
            eventOrganizer.setPostcode(newEventOrganizer.getPostcode());
        }
        if(newEventOrganizer.getTel() != null && newEventOrganizer.getTel() != eventOrganizer.getTel()) {
            eventOrganizer.setTel(newEventOrganizer.getTel());
        }
        if(newEventOrganizer.getEmail() != null && newEventOrganizer.getEmail() != eventOrganizer.getEmail()) {
            eventOrganizer.setEmail(newEventOrganizer.getEmail());
        }
        if(newEventOrganizer.getWWW() != null && newEventOrganizer.getWWW() != eventOrganizer.getWWW()) {
            eventOrganizer.setWWW(newEventOrganizer.getWWW());
        }
        if(newEventOrganizer.getContactPerson() != null && newEventOrganizer.getContactPerson() != eventOrganizer.getContactPerson()) {
            eventOrganizer.setContactPerson(newEventOrganizer.getContactPerson());
        }
        eoRepository.save(eventOrganizer);
        EventOrganizerLinks links = new EventOrganizerLinks(eventOrganizer);
        eventOrganizer.add(links.getAll());
        Resource<EventOrganizer> resource = new Resource<EventOrganizer>(eventOrganizer);
        return ResponseEntity.ok(resource);
    }

    @DeleteMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> delete(@PathVariable Long id) {
        EventOrganizer eventOrganizer = eoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (eventOrganizer.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify EventOrganizer that is marked as deleted");
        }
        eventOrganizer.setInvalid();
        eoRepository.save(eventOrganizer);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = "application/hal+json")
    ResponseEntity<Resources<EventOrganizer>> all() {
        List<EventOrganizer> eventOrganizers = eoRepository.findAll();
        Link link = linkTo(EventOrganizerController.class).withSelfRel();
        if (eventOrganizers.size() != 0) {
            for (EventOrganizer eventOrganizer : eventOrganizers) {
                EventOrganizerLinks links = new EventOrganizerLinks(eventOrganizer);
                eventOrganizer.add(links.getAll());
            }
            Resources<EventOrganizer> resources = new Resources<EventOrganizer>(eventOrganizers, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping(value = "/{id}", produces = "application/hal+json")
    public ResponseEntity<Resource<EventOrganizer>> one(@PathVariable Long id) {
        EventOrganizer eventOrganizer = eoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        EventOrganizerLinks links = new EventOrganizerLinks(eventOrganizer);
        eventOrganizer.add(links.getAll());
        Resource<EventOrganizer> resource = new Resource<EventOrganizer>(eventOrganizer);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/postcode", produces = "application/hal+json")
    public ResponseEntity<Resource<Postcode>> getPostcode(@PathVariable Long id) {
        EventOrganizer eventOrganizer = eoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        Link link = linkTo(EventOrganizerController.class).withSelfRel();
        Postcode postcode = eventOrganizer.getPostcode();
        PostcodeLinks links = new PostcodeLinks(postcode);
        postcode.add(links.getAll());
        Resource<Postcode> resource = new Resource<Postcode>(postcode, link);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/events", produces = "application/hal+json")
    public ResponseEntity<Resources<Event>> getEvents(@PathVariable Long id) {
        EventOrganizer eventOrganizer = eoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        Link link = linkTo(EventOrganizerController.class).withSelfRel();
        List<Event> events = eventOrganizer.getEvents();
        if (events.size() != 0) {
            for (Event event : events) {
                EventLinks links = new EventLinks(event);
                event.add(links.getAll());
            }
            Resources<Event> resources = new Resources<Event>(events, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.noContent().build();
        }
    }
}