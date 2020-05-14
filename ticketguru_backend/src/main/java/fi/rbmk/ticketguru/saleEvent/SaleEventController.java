package fi.rbmk.ticketguru.saleEvent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;

import com.fasterxml.jackson.databind.JsonNode;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fi.rbmk.ticketguru.constraintViolationParser.ConstraintViolationParser;
import fi.rbmk.ticketguru.eventTicket.EventTicket;
import fi.rbmk.ticketguru.eventTicket.EventTicketRepository;
import fi.rbmk.ticketguru.saleRow.*;
import fi.rbmk.ticketguru.ticket.Ticket;
import fi.rbmk.ticketguru.ticket.TicketLinks;
import fi.rbmk.ticketguru.ticket.TicketRepository;
import fi.rbmk.ticketguru.ticket.TicketService;
import fi.rbmk.ticketguru.user.*;

@RestController
@RequestMapping(value = "/api/saleEvents", produces = "application/hal+json")
public class SaleEventController {

	@Autowired
	SaleEventRepository sERepository;
	@Autowired
	UserRepository uRepository;
	@Autowired
	SaleRowRepository sRRepository;
	@Autowired
	EventTicketRepository etRepository;
	@Autowired
	TicketRepository tRepository;
	@Autowired
	TicketService tService;

	private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Long getIdFromUri(String str) {
        try {
            URI uri = new URI(str);
            String[] uriParts = uri.getPath().split("/");
            if (uriParts.length < 2) {
                return null;
            }
            return Long.parseLong(uriParts[uriParts.length - 1]);
        } catch (URISyntaxException e) {
            return null;
        }
    }

	// @PostMapping(produces = "application/hal+json")
	// ResponseEntity<?> add(@Valid @RequestBody SaleEvent newSaleEvent) {
	// 	try {
	// 		if (newSaleEvent.getUser().getInvalid() != null) {
	// 			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot link User that is marked as deleted");
	// 		}
	// 		SaleEvent saleEvent = sERepository.save(newSaleEvent);
	// 		SaleEventLinks links = new SaleEventLinks(saleEvent);
	// 		saleEvent.add(links.getAll());
	// 		Resource<SaleEvent> resource = new Resource<SaleEvent>(saleEvent);
	// 		return ResponseEntity.created(URI.create(saleEvent.getId().getHref())).body(resource);
	// 	} catch (DataIntegrityViolationException e) {
	// 		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate entry");
	// 	}
	// }

    @PostMapping(produces = "application/hal+json")
    ResponseEntity<?> add(@RequestBody JsonNode requestBody) {
		User user = null;
        if (requestBody.has("user")) {
            user = uRepository.findById(getIdFromUri(requestBody.get("user").textValue()))
                .orElseThrow(() -> new ResourceNotFoundException("Invalid user ID"));
            if (user.getInvalid() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create SaleEvent for User that is marked as deleted");
            }
        } else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing user from json body");
		}
        try {
			SaleEvent newSaleEvent = new SaleEvent(user);
			SaleEvent saleEvent = sERepository.save(newSaleEvent);
            if (requestBody.has("saleRows")) {
                JsonNode saleRowNode = requestBody.get("saleRows");
                for (JsonNode jsonNode : saleRowNode) {
                    EventTicket eventTicket = null;
                    if (jsonNode.has("eventTicket")) {
                        eventTicket = etRepository.findById(getIdFromUri(jsonNode.get("eventTicket").textValue()))
                            .orElseThrow(() -> new ResourceNotFoundException("Invalid eventTicket ID"));
                        if (eventTicket.getInvalid() != null) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create SaleRow with EventTicket that is marked as deleted");
                        }
                    }
                    Long discount = jsonNode.has("discount") ? jsonNode.get("discount").asLong() : 0;
                    SaleRow newSaleRow = new SaleRow(saleEvent, discount);
                    Set<ConstraintViolation<Object>> violations = validator.validate(newSaleRow);
                    if (!violations.isEmpty()) {
                        ConstraintViolationParser constraintViolationParser = new ConstraintViolationParser(violations, HttpStatus.BAD_REQUEST);
                        return ResponseEntity.badRequest().body(constraintViolationParser.parse());
                    }
                    SaleRow saleRow = sRRepository.save(newSaleRow);
                    ResponseEntity<?> response = tService.generateTickets(saleRow, eventTicket, jsonNode.get("count").asLong());
                    if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
						saleRow.setInvalid();
						sRRepository.save(saleRow);
                        return response;
                    }
				}
				SaleEventLinks links = new SaleEventLinks(saleEvent);
				saleEvent.add(links.getAll());
                Resource<SaleEvent> resource = new Resource<SaleEvent>(saleEvent);
            return ResponseEntity.ok(resource);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing saleRow from json body");
            }
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate entry");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID");
        }
    }

	@PatchMapping(value = "/{id}", produces = "application/hal+json")
	ResponseEntity<?> edit(@Valid @RequestBody SaleEvent newSaleEvent, @PathVariable Long id) {
		SaleEvent saleEvent = sERepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
		if (saleEvent.getInvalid() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify SaleEvent that is marked as deleted");
		}
        Set<ConstraintViolation<Object>> violations = validator.validate(newSaleEvent);
        if (!violations.isEmpty()) {
            ConstraintViolationParser constraintViolationParser = new ConstraintViolationParser(violations, HttpStatus.BAD_REQUEST);
            return ResponseEntity.badRequest().body(constraintViolationParser.parse());
        }
		if (newSaleEvent.getUser() != null && newSaleEvent.getUser() != saleEvent.getUser()) {
			saleEvent.setUser(newSaleEvent.getUser());
		}
		sERepository.save(saleEvent);
		SaleEventLinks links = new SaleEventLinks(saleEvent);
		saleEvent.add(links.getAll());
		Resource<SaleEvent> resource = new Resource<SaleEvent>(saleEvent);
		return ResponseEntity.ok(resource);
	}

	@DeleteMapping(value = "/{id}", produces = "application/hal+json")
	ResponseEntity<?> delete(@PathVariable Long id) {
		SaleEvent saleEvent = sERepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
		if (saleEvent.getInvalid() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify SaleEvent that is marked as deleted");
		}
		saleEvent.setInvalid();
		sERepository.save(saleEvent);
		for (SaleRow saleRow : saleEvent.getSaleRows()) {
            saleRow.setInvalid();
			sRRepository.save(saleRow);
			for (Ticket ticket : saleRow.getTickets()) {
				ticket.setInvalid();
				tRepository.save(ticket);
			}
        }
		return ResponseEntity.noContent().build();
	}

	@GetMapping(produces = "application/hal+json")
	ResponseEntity<Resources<SaleEvent>> all() {
		List<SaleEvent> users = sERepository.findAll();
		Link link = linkTo(SaleEventController.class).withSelfRel();
		if (users.size() != 0) {
			for (SaleEvent saleEvent : users) {
				SaleEventLinks links = new SaleEventLinks(saleEvent);
				saleEvent.add(links.getAll());
			}
			Resources<SaleEvent> resources = new Resources<SaleEvent>(users, link);
			return ResponseEntity.ok(resources);
		} else {
			return ResponseEntity.noContent().build();
		}
	}

	@GetMapping(value = "/{id}", produces = "application/hal+json")
	public ResponseEntity<Resource<SaleEvent>> one(@PathVariable Long id) {
		SaleEvent saleEvent = sERepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
		SaleEventLinks links = new SaleEventLinks(saleEvent);
		saleEvent.add(links.getAll());
		Resource<SaleEvent> resource = new Resource<SaleEvent>(saleEvent);
		return ResponseEntity.ok(resource);
	}

	@GetMapping(value = "/{id}/user", produces = "application/hal+json")
	ResponseEntity<Resource<User>> getUser(@PathVariable Long id) {
		SaleEvent saleEvent = sERepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
		User user = saleEvent.getUser();
		UserLinks userLinks = new UserLinks(user);
		user.add(userLinks.getAll());
		Resource<User> resource = new Resource<User>(user);
		return ResponseEntity.ok(resource);
	}

	@GetMapping(value = "/{id}/saleRows", produces = "application/hal+json")
	public ResponseEntity<Resources<SaleRow>> getSaleRows(@PathVariable Long id) {
		SaleEvent saleEvent = sERepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
		Link link = linkTo(SaleEventController.class).withSelfRel();
		List<SaleRow> saleRows = saleEvent.getSaleRows();
		if (saleRows.size() != 0) {
			for (SaleRow saleRow : saleRows) {
				SaleRowLinks links = new SaleRowLinks(saleRow);
				saleRow.add(links.getAll());
			}
			Resources<SaleRow> resources = new Resources<SaleRow>(saleRows, link);
			return ResponseEntity.ok(resources);
		} else {
			return ResponseEntity.noContent().build();
		}
	}

	@GetMapping(value = "/{id}/tickets", produces = "application/hal+json")
	public ResponseEntity<Resources<Ticket>> getTickets(@PathVariable Long id) {
		SaleEvent saleEvent = sERepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
		Link link = linkTo(SaleEventController.class).withSelfRel();
		List<Ticket> tickets = tRepository.findBySaleRow_SaleEvent(saleEvent);
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

}