package com.kaluzny.demo.web;

import com.kaluzny.demo.domain.Automobile;
import com.kaluzny.demo.domain.AutomobileRepository;
import com.kaluzny.demo.exception.AutoWasDeletedException;
import com.kaluzny.demo.exception.ThereIsNoSuchAutoException;
import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import jakarta.jms.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomobileService {
    private final AutomobileRepository repository;
    private final JmsTemplate jmsTemplate;

    public static double getTiming(Instant start, Instant end) {
        return Duration.between(start, end).toMillis();
    }

    @Transactional
    @PostConstruct
    public void init() {
        repository.save(new Automobile(1L, "Ford", "Green", LocalDateTime.now(), LocalDateTime.now(), true, false));
    }

    public Automobile saveAuto(Automobile automobile) {
        return repository.save(automobile);
    }

    public Collection<Automobile> getAllAuto() {
        return repository.findAll();
    }

    public Automobile getAutoById(Long id) {
        Automobile receivedAutomobile = repository.findById(id)
                .orElseThrow(ThereIsNoSuchAutoException::new);
        if (receivedAutomobile.getDeleted()) {
            throw new AutoWasDeletedException();
        }
        return receivedAutomobile;
    }

    public Collection<Automobile> findAutoByName(String name) {
        Collection<Automobile> collection = repository.findByName(name);
        return collection;
    }

    public Automobile refreshAuto(Long id, Automobile automobile) {

        Automobile updatedAutomobile = repository.findById(id)
                .map(entity -> {
                    entity.checkColor(automobile);
                    entity.setName(automobile.getName());
                    entity.setColor(automobile.getColor());
                    entity.setUpdateDate(automobile.getUpdateDate());
                    if (entity.getDeleted()) {
                        throw new AutoWasDeletedException();
                    }
                    return repository.save(entity);
                })

                .orElseThrow(ThereIsNoSuchAutoException::new);
        return updatedAutomobile;
    }

    public String removeAutoById(Long id) {
        Automobile deletedAutomobile = repository.findById(id)
                .orElseThrow(ThereIsNoSuchAutoException::new);
        deletedAutomobile.setDeleted(Boolean.TRUE);
        repository.save(deletedAutomobile);
        return "Deleted";
    }

    public void removeAllAuto() {
        repository.deleteAll();
    }

    public Collection<Automobile> findAutoByColor(String color) {
        Instant start = Instant.now();
        log.info("findAutomobileByColor() - start: time = {}", start);
        log.info("findAutomobileByColor() - start: color = {}", color);
        Collection<Automobile> collection = repository.findByColor(color);
        Instant end = Instant.now();
        log.info("findAutomobileByColor() - end: milliseconds = {}", getTiming(start, end));
        log.info("findAutomobileByColor() - end: collection = {}", collection);
        return collection;
    }

    public Collection<Automobile> findAutoByNameAndColor(String name, String color) {
        Collection<Automobile> collection = repository.findByNameAndColor(name, color);
        return collection;
    }

    public Collection<Automobile> findAutoByColorStartsWith(
            String colorStartsWith, int page, int size) {
        Collection<Automobile> collection = repository
                .findByColorStartsWith(colorStartsWith, PageRequest.of(page, size, Sort.by("color")));

        return collection;
    }
       public List<String> getAllAutoByName() {
            List<Automobile> collection = repository.findAll();
            List<String> collectionName = collection.stream()
                    .map(Automobile::getName)
                    .sorted()
                    .collect(Collectors.toList());

            return collectionName;
        }
            public Automobile push( Automobile automobile) throws JMSException {
                Automobile savedAutomobile = repository.save(automobile);
                log.info("\u001B[32m" + "Sending Automobile with id: " + savedAutomobile.getId() + "\u001B[0m");

                if ("Red".equals(automobile.getColor())) {
                    jmsTemplate.convertAndSend("AutoTopicRed", savedAutomobile);
                    jmsTemplate.convertAndSend("AutoTopic", savedAutomobile);
                } else {
                    jmsTemplate.convertAndSend("AutoTopic", savedAutomobile);
                }

                return savedAutomobile;

//        Topic autoTopic = Objects.requireNonNull(jmsTemplate
//                        .getConnectionFactory())
//                        .createConnection()
//                        .createSession()
//                        .createTopic("AutoTopic");
//                Automobile savedAutomobile = repository.save(automobile);
//                log.info("\u001B[32m" + "Sending Automobile with id: " + savedAutomobile.getId() + "\u001B[0m");
//                jmsTemplate.convertAndSend(autoTopic, savedAutomobile);
//                return savedAutomobile;


        }

        public List<Automobile> allRedAuto(){
        List<Automobile> collection = repository.findByColor("Red");
        for(Automobile automobile : collection){
            jmsTemplate.convertAndSend("AutoTopicRed", automobile);
        }
        return collection;

        }

            }


