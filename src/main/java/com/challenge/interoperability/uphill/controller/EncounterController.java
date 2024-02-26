package com.challenge.interoperability.uphill.controller;

import com.challenge.interoperability.uphill.service.EncounterService;
import com.challenge.interoperability.uphill.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/fhir/Encounter", produces = "application/json")
public class EncounterController {

    private final EncounterService encounterService;

    @Autowired
    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @GetMapping
    public ResponseEntity<String> getEncounter(@RequestParam(name = "system") String system, @RequestParam(name = "value") String value) {
        return encounterService.getByIdentifier(system, value);
    }

}
