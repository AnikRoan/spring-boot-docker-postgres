package com.kaluzny.demo.web;

import com.kaluzny.demo.domain.Automobile;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface JMSPublisher {

    ResponseEntity<Automobile> pushMessage(Automobile automobile);
     ResponseEntity<List<Automobile>> getAllRedAuto();
}
