package com.challenge.interoperability.uphill.controller;

import com.challenge.interoperability.uphill.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/fhir/Patient", produces = "application/json")
public class PatientController {

    private final PatientService patientService;

    @Autowired
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<String> getPatient(@RequestParam(name = "system") String system, @RequestParam(name = "value") String value) {
        return patientService.getByIdentifier(system, value);
    }

}
