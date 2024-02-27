package com.challenge.interoperability.uphill.controller;

import ca.uhn.fhir.parser.IParser;
import com.challenge.interoperability.uphill.domain.FhirR4ParserFactory;
import com.challenge.interoperability.uphill.service.PatientService;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/fhir/Patient", produces = "application/json")
public class PatientController {

    private final PatientService patientService;

    IParser parser = FhirR4ParserFactory.getParser();

    @Autowired
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<String> getPatient(@RequestParam(name = "identifier") String identifier) {

        String system = "urn:uh-patient-id";
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

        return patientService.getByIdentifier(system, value);
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
