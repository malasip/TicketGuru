package fi.rbmk.ticketguru.ageLimit;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.rbmk.ticketguru.event.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping(value = "/api/ageLimits", produces = "application/hal+json")
public class AgeLimitController {
	
	@Autowired
	private AgeLimitRepository alRepository;
	
    @PostMapping(produces = "application/hal+json")
    public ResponseEntity<?> add(@Valid @RequestBody AgeLimit newAgeLimit) {
        try {
            AgeLimit ageLimit = alRepository.save(newAgeLimit);
            AgeLimitLinks links = new AgeLimitLinks(ageLimit);
            ageLimit.add(links.getAll());
            Resource<AgeLimit> resource = new Resource<AgeLimit>(ageLimit);
            return ResponseEntity.created(URI.create("/api/ageLimits/" + ageLimit.getAgeLimit_ID())).body(resource);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.badRequest().body("Duplicate entry");
        }
    }
    
    @PatchMapping(value = "/{id}", produces = "application/hal+json")
    public ResponseEntity<?> edit(@RequestBody AgeLimit newAgeLimit, @PathVariable Long id) {
        AgeLimit ageLimit = alRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (ageLimit.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify AgeLimit that is marked as deleted");
        }
        if (newAgeLimit.getName() != null && newAgeLimit.getName() != "" && newAgeLimit.getName() != ageLimit.getName()) {
            ageLimit.setName(newAgeLimit.getName());
        }
        if (newAgeLimit.getSpecifier() != null && newAgeLimit.getSpecifier() != ageLimit.getSpecifier()) {
            ageLimit.setSpecifier(newAgeLimit.getSpecifier());
        }
        alRepository.save(ageLimit);
        Resource<AgeLimit> resource = new Resource<AgeLimit>(ageLimit);
        return ResponseEntity.ok(resource);
    }
    
    @DeleteMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> delete(@PathVariable Long id) {
        AgeLimit ageLimit = alRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (ageLimit.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify AgeLimit that is marked as deleted");
        }
    	ageLimit.setInvalid();
    	alRepository.save(ageLimit);
    	return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = "application/hal+json")
    ResponseEntity<Resources<AgeLimit>> all() {
        List<AgeLimit> ageLimits = alRepository.findAll();
        Link link = linkTo(AgeLimitController.class).withSelfRel();
        if (ageLimits.size() != 0) {
            for (AgeLimit ageLimit : ageLimits) {
                AgeLimitLinks links = new AgeLimitLinks(ageLimit);
                ageLimit.add(links.getAll());
            }
            Resources<AgeLimit> resources = new Resources<AgeLimit>(ageLimits, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{id}", produces = "application/hal+json")
    public ResponseEntity<Resource<AgeLimit>> one(@PathVariable Long id) {
        AgeLimit ageLimit = alRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        AgeLimitLinks links = new AgeLimitLinks(ageLimit);
        ageLimit.add(links.getAll());
        Resource<AgeLimit> resource = new Resource<AgeLimit>(ageLimit);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/events", produces = "application/hal+json")
    public ResponseEntity<Resources<Event>> getEvents(@PathVariable Long id) {
        AgeLimit ageLimit = alRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        Link link = linkTo(AgeLimitController.class).withSelfRel();
        List<Event> events = ageLimit.getEvents();
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
}
