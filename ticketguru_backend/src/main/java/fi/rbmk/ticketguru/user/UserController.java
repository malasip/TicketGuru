package fi.rbmk.ticketguru.user;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fi.rbmk.ticketguru.eventTicket.EventTicketRepository;
import fi.rbmk.ticketguru.saleEvent.*;
import fi.rbmk.ticketguru.userGroup.*;

@RestController
@RequestMapping(value = "/api/users", produces = "application/hal+json")
public class UserController {

    @Autowired
    UserRepository uRepository;
    @Autowired
    UserGroupRepository uGRepository;

    @Autowired
    EventTicketRepository eTRepository;

    @PostMapping(produces = "application/hal+json")
    ResponseEntity<?> add(@Valid @RequestBody User newUser) {
        try {
            if (newUser.getUserGroup().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link UserGroup that is marked as deleted");
            }
            User user = uRepository.save(newUser);
            UserLinks links = new UserLinks(user);
            user.add(links.getAll());
            Resource<User> resource = new Resource<User>(user);
            return ResponseEntity.ok(resource);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Duplicate entry");
        }

    }

    @PatchMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> edit(@RequestBody User newUser, @PathVariable Long id) {
        User user = uRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (user.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify User that is marked as deleted");
        }
        if (newUser.getPassword() != null && newUser.getPassword() != "" && newUser.getPassword() != user.getPassword()) {
            user.setPassword(newUser.getPassword());
        }
        if (newUser.getName() != null) {
            return ResponseEntity.badRequest().body("Cannot modify username");
        }
        if (newUser.getUserGroup() != null && newUser.getUserGroup() != user.getUserGroup()) {
            if (newUser.getUserGroup().getInvalid() != null) {
                return ResponseEntity.badRequest().body("Cannot link UserGroup that is marked as deleted");
            }
            user.setUserGroup(newUser.getUserGroup());
        }
        if (newUser.getActive() != null && newUser.getActive() != user.getActive()) {
            user.setActive(newUser.getActive());
        }
        uRepository.save(user);
        UserLinks links = new UserLinks(user);
        user.add(links.getAll());
        Resource<User> resource = new Resource<User>(user);
        return ResponseEntity.ok(resource);
    }

    @DeleteMapping(value = "/{id}", produces = "application/hal+json")
    ResponseEntity<?> delete(@PathVariable Long id) {
        User user = uRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        if (user.getInvalid() != null) {
            return ResponseEntity.badRequest().body("Cannot modify User that is marked as deleted");
        }
        user.setInvalid();
        return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = "application/hal+json")
    ResponseEntity<Resources<User>> all() {
        List<User> users = uRepository.findAll();
        Link link = linkTo(UserController.class).withSelfRel();
        if (users.size() != 0) {
            for (User user : users) {
                UserLinks links = new UserLinks(user);
                user.add(links.getAll());
            }
            Resources<User> resources = new Resources<User>(users, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping(value = "/{id}", produces = "application/hal+json")
    public ResponseEntity<Resource<User>> one(@PathVariable Long id) {
        User user = uRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        UserLinks links = new UserLinks(user);
        user.add(links.getAll());
        Resource<User> resource = new Resource<User>(user);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/userGroup", produces = "application/hal+json")
    ResponseEntity<Resource<UserGroup>> getUserGroup(@PathVariable Long id) {
        User user = uRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        UserGroup userGroup = user.getUserGroup();
        UserGroupLinks links = new UserGroupLinks(userGroup);
        userGroup.add(links.getAll());
        Resource<UserGroup> resource = new Resource<UserGroup>(userGroup);
        return ResponseEntity.ok(resource);
    }

    @GetMapping(value = "/{id}/saleEvents", produces = "application/hal+json")
    public ResponseEntity<Resources<SaleEvent>> getSaleEvent(@PathVariable Long id) {
        User user = uRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invalid ID: " + id));
        Link link = linkTo(UserController.class).withSelfRel();
        List<SaleEvent> saleEvents = user.getSaleEvents();
        if (saleEvents.size() != 0) {
            for (SaleEvent saleEvent : saleEvents) {
                SaleEventLinks links = new SaleEventLinks(saleEvent);
                saleEvent.add(links.getAll());
            }
            Resources<SaleEvent> resources = new Resources<SaleEvent>(saleEvents, link);
            return ResponseEntity.ok(resources);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/logout", produces = "application/hal+json")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return ResponseEntity.noContent().build();
    }
}