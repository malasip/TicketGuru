package fi.rbmk.ticketguru.ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fi.rbmk.ticketguru.constraintViolationParser.ConstraintViolationParser;
import fi.rbmk.ticketguru.eventTicket.EventTicket;
import fi.rbmk.ticketguru.saleRow.SaleRow;
import fi.rbmk.ticketguru.ticketStatus.TicketStatus;
import fi.rbmk.ticketguru.ticketStatus.TicketStatusRepository;

@Service
public class TicketService {

    @Autowired
    TicketRepository tRepository;
    @Autowired
    TicketStatusRepository tStatusRepository;

    private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public ResponseEntity<?> generateTickets(SaleRow saleRow, EventTicket eventTicket, Long count) {
        List<Ticket> ticketList = new ArrayList<Ticket>();
        TicketStatus ticketStatus = tStatusRepository.findById(Integer.toUnsignedLong(1)).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: 1"));
        // Create a test ticket to check that all required values have been passed
        Ticket tmpTicket = new Ticket(eventTicket, saleRow, ticketStatus);
        if (eventTicket.getTicketCount() - tRepository.findByEventTicket(eventTicket).size() <= count) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough tickets left to sell.");
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(tmpTicket);
        if (!violations.isEmpty()) {
            ConstraintViolationParser constraintViolationParser = new ConstraintViolationParser(violations, HttpStatus.BAD_REQUEST);
            return ResponseEntity.badRequest().body(constraintViolationParser.parse());
        }
        tmpTicket = null;
        for (int i = 0; i < count; i++) {
            Ticket ticket = new Ticket(eventTicket, saleRow, ticketStatus);
            ticketList.add(tRepository.save(ticket));
        }
        return ResponseEntity.ok(ticketList);
    }

    public List<Object> validate(String checksum) {
        List<Object> result = new ArrayList<Object>();
        Ticket ticket = null;
        ticket = tRepository.findByChecksum(checksum);
        if (ticket == null) {
            throw new ResourceNotFoundException("Invalid Checksum");
        }
        TicketLinks ticketLinks = new TicketLinks(ticket);
        ticket.add(ticketLinks.getAll());
        if (ticket.getInvalid() != null && ticket.getTicketStatus().getTicketStatus_id() != 1) {
            if (ticket.getInvalid() != null) {
                result.add(ticket);
                result.add("Ticket has been marked invalid");
                return result;
            } else {
                result.add(ticket);
                result.add("Invalid ticket status: " + ticket.getTicketStatus().getName());
                return result;
            }
        }
        ticket.setTicketStatus(tStatusRepository.findById(Integer.toUnsignedLong(2)).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: 1")));
        result.add(tRepository.save(ticket));
        return result;
    }
}