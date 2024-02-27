package com.challenge.interoperability.uphill.service;

import ca.uhn.fhir.parser.IParser;
import com.challenge.interoperability.uphill.domain.FhirR4ParserFactory;
import com.challenge.interoperability.uphill.repository.PatientRepository;
import com.challenge.interoperability.uphill.domain.entities.PatientEntity;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    IParser parser = FhirR4ParserFactory.getParser();

    public Optional<PatientEntity> getById(String id){
        return patientRepository.findLastVersionById(id);
    }

    public ResponseEntity<String> getByIdentifier(String system, String value) {
        List<PatientEntity> patientList = patientRepository.findByIdentifier(system, value);

        if(!patientList.isEmpty()) {
            Bundle bundleResponse = new Bundle();
            bundleResponse.setType(Bundle.BundleType.SEARCHSET);

            List<Bundle.BundleEntryComponent> patientEntries = new ArrayList<>();
            for(PatientEntity patientEntity : patientList){
                Patient patient = parser.parseResource(Patient.class, patientEntity.getPatientResource());
                Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
                patientEntry.setResource(patient);
                patientEntries.add(patientEntry);
            }
            bundleResponse.setEntry(patientEntries);

            return new ResponseEntity<>(parser.encodeResourceToString(bundleResponse), HttpStatus.ACCEPTED);
        } else {
            OperationOutcome outcome = new OperationOutcome();
            outcome.addIssue()
                    .setCode(OperationOutcome.IssueType.NOTFOUND)
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setDiagnostics(String.format("No Patient with identifier %s for system %s was found.", value, system));
            return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.NOT_FOUND);
        }

    }

    public PatientEntity createPatient(PatientEntity patient) {
        return patientRepository.save(patient);
    }

}
