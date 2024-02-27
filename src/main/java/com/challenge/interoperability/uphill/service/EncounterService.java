package com.challenge.interoperability.uphill.service;

import ca.uhn.fhir.parser.IParser;
import com.challenge.interoperability.uphill.domain.FhirR4ParserFactory;
import com.challenge.interoperability.uphill.domain.entities.EncounterEntity;
import com.challenge.interoperability.uphill.repository.EncounterRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EncounterService {

    @Autowired
    private EncounterRepository encounterRepository;

    IParser parser = FhirR4ParserFactory.getParser();

    public Optional<EncounterEntity> getById(String id){
        return encounterRepository.findLastVersionById(id);
    }

    public ResponseEntity<String> getByIdentifier(String system, String value) {
        List<EncounterEntity> encounterList = encounterRepository.findByIdentifier(system, value);

        if(!encounterList.isEmpty()) {
            Bundle bundleResponse = new Bundle();
            bundleResponse.setType(Bundle.BundleType.SEARCHSET);

            List<Bundle.BundleEntryComponent> encounterEntries = new ArrayList<>();
            for(EncounterEntity encounterEntity : encounterList){
                Encounter encounter = parser.parseResource(Encounter.class, encounterEntity.getEncounterResource());
                Meta encounterMeta = new Meta();
                encounterMeta.setVersionId(String.valueOf(encounterEntity.getVersion()));
                encounter.setMeta(encounterMeta);
                Bundle.BundleEntryComponent encounterEntry = new Bundle.BundleEntryComponent();
                encounterEntry.setResource(encounter);
                encounterEntries.add(encounterEntry);
            }
            bundleResponse.setEntry(encounterEntries);

            return new ResponseEntity<>(parser.encodeResourceToString(bundleResponse), HttpStatus.ACCEPTED);
        } else {
            OperationOutcome outcome = new OperationOutcome();
            outcome.addIssue()
                    .setCode(OperationOutcome.IssueType.NOTFOUND)
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setDiagnostics(String.format("No Encounter with identifier %s for system %s was found.", value, system));
            return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.NOT_FOUND);
        }
    }

    public EncounterEntity createEncounter(EncounterEntity encounter) {
        return encounterRepository.save(encounter);
    }

}
