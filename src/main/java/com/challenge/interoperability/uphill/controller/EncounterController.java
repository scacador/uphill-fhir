package com.challenge.interoperability.uphill.controller;

import ca.uhn.fhir.parser.IParser;
import com.challenge.interoperability.uphill.domain.FhirR4ParserFactory;
import com.challenge.interoperability.uphill.service.EncounterService;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/fhir/Encounter", produces = "application/json")
public class EncounterController {

    private final EncounterService encounterService;

    IParser parser = FhirR4ParserFactory.getParser();

    @Autowired
    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @GetMapping
    public ResponseEntity<String> getEncounter(@RequestParam(name = "identifier") String identifier) {

        String system = "urn:uh-encounter-id";
        String value = "";

        if(identifier.contains("|")){
            String[] identifierParam = identifier.split("\\|" );
            system = identifierParam[0];
            if(identifierParam.length == 1){
                return new ResponseEntity<String>(parser.encodeResourceToString(createErrorOperationOutcome("An identifier should be provided")), HttpStatus.BAD_REQUEST);
            } else value = identifierParam[1];
        } else {
            value = identifier;
        }

        return encounterService.getByIdentifier(system, value);
    }

    private OperationOutcome createErrorOperationOutcome(String diagnostics) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                .setCode(OperationOutcome.IssueType.REQUIRED)
                .setDiagnostics(diagnostics);
        return outcome;
    }
}
