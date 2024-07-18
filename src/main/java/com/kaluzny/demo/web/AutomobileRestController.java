package com.kaluzny.demo.web;

import com.kaluzny.demo.domain.Automobile;
import com.kaluzny.demo.domain.AutomobileRepository;
import com.kaluzny.demo.exception.AutoWasDeletedException;
import com.kaluzny.demo.exception.ThereIsNoSuchAutoException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.PostConstruct;
import jakarta.jms.Topic;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Slf4j
public class AutomobileRestController implements AutomobileResource, AutomobileOpenApi, JMSPublisher {

    private final AutomobileService automobileService;


    @PostMapping("/automobiles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    //@RolesAllowed("ADMIN")
    public Automobile saveAutomobile(@Valid @RequestBody Automobile automobile) {
        log.info("saveAutomobile() - start: automobile = {}", automobile);
        Automobile savedAutomobile = automobileService.saveAuto(automobile);
        log.info("saveAutomobile() - end: savedAutomobile = {}", savedAutomobile.getId());
        return savedAutomobile;
    }

    @GetMapping("/automobiles")
    @ResponseStatus(HttpStatus.OK)
    //@Cacheable(value = "automobile", sync = true)
    @PreAuthorize("hasRole('USER')")
    public Collection<Automobile> getAllAutomobiles() {
        log.info("getAllAutomobiles() - start");
        Collection<Automobile> collection =automobileService.getAllAuto();
        log.info("getAllAutomobiles() - end");
        return collection;
    }

    @GetMapping("/automobiles/{id}")
    @ResponseStatus(HttpStatus.OK)
    //@Cacheable(value = "automobile", sync = true)
    //TODO: We do not have PERSON on the user map
    @PreAuthorize("hasRole('PERSON')")
    public Automobile getAutomobileById(@PathVariable Long id) {
        log.info("getAutomobileById() - start: id = {}", id);
        Automobile receivedAutomobile = automobileService.getAutoById(id);
        log.info("getAutomobileById() - end: Automobile = {}", receivedAutomobile.getId());
        return receivedAutomobile;
    }


    @Hidden
    @GetMapping(value = "/automobiles", params = {"name"})
    @ResponseStatus(HttpStatus.OK)
    public Collection<Automobile> findAutomobileByName(@RequestParam(value = "name") String name) {
        log.info("findAutomobileByName() - start: name = {}", name);
        Collection<Automobile> collection = automobileService.findAutoByName(name);
        log.info("findAutomobileByName() - end: collection = {}", collection);
        return collection;
    }

    @PutMapping("/automobiles/{id}")
    @ResponseStatus(HttpStatus.OK)
    //@CachePut(value = "automobile", key = "#id")
    public Automobile refreshAutomobile(@PathVariable Long id, @RequestBody Automobile automobile) {
        log.info("refreshAutomobile() - start: id = {}, automobile = {}", id, automobile);
        Automobile updatedAutomobile = automobileService.refreshAuto(id, automobile);
        log.info("refreshAutomobile() - end: updatedAutomobile = {}", updatedAutomobile);
        return updatedAutomobile;
    }

    @DeleteMapping("/automobiles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CacheEvict(value = "automobile", key = "#id")
    public String removeAutomobileById(@PathVariable Long id) {
        log.info("removeAutomobileById() - start: id = {}", id);
        String autoIsDeleted = automobileService.removeAutoById(id);
        log.info("removeAutomobileById() - end: id = {}", id);
        return autoIsDeleted;
    }

    @Hidden
    @DeleteMapping("/automobiles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAllAutomobiles() {
        log.info("removeAllAutomobiles() - start");
       automobileService.removeAllAuto();
        log.info("removeAllAutomobiles() - end");
    }

    @GetMapping(value = "/automobiles", params = {"color"})
    @ResponseStatus(HttpStatus.OK)
    public Collection<Automobile> findAutomobileByColor(
            @Parameter(description = "Name of the Automobile to be obtained. Cannot be empty.", required = true)
            @RequestParam(value = "color") String color) {
       return automobileService.findAutoByColor(color);
    }

    @GetMapping(value = "/automobiles", params = {"name", "color"})
    @ResponseStatus(HttpStatus.OK)
    public Collection<Automobile> findAutomobileByNameAndColor(
            @Parameter(description = "Name of the Automobile to be obtained. Cannot be empty.", required = true)
            @RequestParam(value = "name") String name, @RequestParam(value = "color") String color) {
        log.info("findAutomobileByNameAndColor() - start: name = {}, color = {}", name, color);
        Collection<Automobile> collection = automobileService.findAutoByNameAndColor(name, color);
        log.info("findAutomobileByNameAndColor() - end: collection = {}", collection);
        return collection;
    }

    @GetMapping(value = "/automobiles", params = {"colorStartsWith"})
    @ResponseStatus(HttpStatus.OK)
    public Collection<Automobile> findAutomobileByColorStartsWith(
            @RequestParam(value = "colorStartsWith") String colorStartsWith,
            @RequestParam(value = "page") int page,
            @RequestParam(value = "size") int size) {
        log.info("findAutomobileByColorStartsWith() - start: color = {}", colorStartsWith);
        Collection<Automobile> collection = automobileService.findAutoByColorStartsWith(colorStartsWith, page, size);
        log.info("findAutomobileByColorStartsWith() - end: collection = {}", collection);
        return collection;
    }

    @GetMapping("/automobiles-names")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getAllAutomobilesByName() {
        log.info("getAllAutomobilesByName() - start");

        List<String> collectionName = automobileService.getAllAutoByName();
        log.info("getAllAutomobilesByName() - end");
        return collectionName;
    }

    @Override
    @PostMapping("/message")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Automobile> pushMessage(@RequestBody Automobile automobile) {
        try {
            Automobile savedAutomobile = automobileService.push(automobile);
            return new ResponseEntity<>(savedAutomobile, HttpStatus.CREATED);

        } catch (Exception exception) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Override
    @GetMapping("/all-red-auto")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<Automobile>> getAllRedAuto() {
        return ResponseEntity.ok(automobileService.allRedAuto());
    }
}
