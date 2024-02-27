package com.challenge.interoperability.uphill.controller;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import com.challenge.interoperability.uphill.domain.FhirR4ParserFactory;
import com.challenge.interoperability.uphill.service.MessageService;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/fhir", produces = "application/json", consumes = "application/json")
public class MessageController {

    private final MessageService messageService;

    IParser parser = FhirR4ParserFactory.getParser();

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/$process-message")
    public ResponseEntity<String> receiveMessage(@RequestBody String bundleMessage) throws DataFormatException {

        Bundle bundle = parser.parseResource(Bundle.class, bundleMessage);
        return messageService.receiveAndValidateMessage(bundle);

    }

}
