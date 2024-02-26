package com.challenge.interoperability.uphill.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.challenge.interoperability.uphill.domain.entities.EncounterEntity;
import com.challenge.interoperability.uphill.domain.entities.PatientEntity;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageService {

    FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();

    @Autowired
    private PatientService patientService;

    @Autowired
    private EncounterService encounterService;

    public ResponseEntity<String> receiveAndValidateMessage(Bundle receivedMessage) {

        List<Resource> resources = new ArrayList<>();
        for(Bundle.BundleEntryComponent entry : receivedMessage.getEntry()){
            resources.add(entry.getResource());
        }

        OperationOutcome outcome;

        List<Resource> messageHeaders = resources.stream().filter(r -> r.getResourceType().toString().equals("MessageHeader")).collect(Collectors.toList());
        if(messageHeaders.isEmpty()){
            outcome = createErrorOperationOutcome("No MessageHeader resource found in message.");
            return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.BAD_REQUEST);
        } else {
            for(Resource header : messageHeaders){
                MessageHeader messageHeader = (MessageHeader) header;
                if(!isMessageHeaderValid(messageHeader)){
                    outcome = createErrorOperationOutcome("MessageHeader's focus must be an Encounter resource.");
                    return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.BAD_REQUEST);
                }
            }
        }

        List<Resource> patients = resources.stream().filter(r -> r.getResourceType().toString().equals("Patient")).collect(Collectors.toList());
        if(patients.isEmpty()){
            outcome = createErrorOperationOutcome("No Patient resource found in message.");
            return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.BAD_REQUEST);
        } else {
            for(Resource pat : patients){
                Patient patient = (Patient) pat;
                if(!isPatientValid(patient)){
                    outcome = createErrorOperationOutcome("Patient resource is invalid. Should have a 'urn:uh-patient-id' identifier, a name, a contact, a gender and a birthdate");
                    return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.BAD_REQUEST);
                }
            }
        }

        List<Resource> encounters = resources.stream().filter(r -> r.getResourceType().toString().equals("Encounter")).collect(Collectors.toList());
        if(encounters.isEmpty()){
            outcome = createErrorOperationOutcome("No Encounter resource found in message.");
            return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.BAD_REQUEST);
        } else {
            for(Resource enc : encounters){
                Encounter encounter = (Encounter) enc;
                if(!isEncounterValid(encounter)){
                    outcome = createErrorOperationOutcome("Encounter resource is invalid. Should have a 'urn:uh-patient-id' identifier, a status, a service type and a subject");
                    return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.BAD_REQUEST);
                }
            }
        }

        for (Resource patientResource : patients){
            Optional<PatientEntity> existingPatient = patientService.getById(patientResource.getIdPart());
            Patient patient = (Patient) patientResource;
            PatientEntity patientEntity = new PatientEntity();
            patientEntity.setId(patient.getIdPart()); //check what to do if there is no id in Patient received
            patientEntity.setPatientResource(parser.encodeResourceToString(patient));

            if(existingPatient.isPresent()){
                patientEntity.setVersion(existingPatient.get().getVersion()+1);
            } else {
                patientEntity.setVersion(1);
            }
            patientService.createPatient(patientEntity);
        }

        for (Resource encounterResource : encounters){
            Optional<EncounterEntity> existingEncounter = encounterService.getById(encounterResource.getIdPart());
            Encounter encounter = (Encounter) encounterResource;
            EncounterEntity encounterEntity = new EncounterEntity();
            encounterEntity.setId(encounter.getIdPart()); //check what to do if there is no id in Patient received
            encounterEntity.setEncounterResource(parser.encodeResourceToString(encounter));

            if(existingEncounter.isPresent()){
                encounterEntity.setVersion(existingEncounter.get().getVersion()+1);
            } else {
                encounterEntity.setVersion(1);
            }
            encounterService.createEncounter(encounterEntity);
        }

        outcome = createSuccessOperationOutcome();
        return new ResponseEntity<>(parser.encodeResourceToString(outcome), HttpStatus.CREATED);
    }

    private boolean isMessageHeaderValid(MessageHeader messageHeader) {
        return messageHeader.getFocus().stream().anyMatch(f -> f.getReference().contains("Encounter"));
    }

    private boolean isPatientValid(Patient patient) {
        boolean patientIdentifier = patient.getIdentifier().stream().anyMatch(i -> i.getSystem().equals("urn:uh-patient-id"));
        return patientIdentifier && patient.hasName() && patient.hasTelecom() && patient.hasGender() && patient.hasBirthDate();

    }

    private boolean isEncounterValid(Encounter encounter) {
        boolean encounterIdentifier = encounter.hasIdentifier() && encounter.getIdentifier().stream().anyMatch(i -> i.getSystem().equals("urn:uh-patient-id"));
        boolean serviceType = encounter.hasServiceType() && encounter.getServiceType().getCoding().stream().anyMatch(c -> c.getSystem().equals("http://hl7.org/fhir/ValueSet/service-type"));
        return encounterIdentifier && encounter.hasStatus() && serviceType && encounter.hasSubject() && encounter.getSubject().getReference().contains("Patient");
    }

    private OperationOutcome createErrorOperationOutcome(String diagnostics) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                .setCode(OperationOutcome.IssueType.REQUIRED)
                .setDiagnostics(diagnostics);
        return outcome;
    }

    private OperationOutcome createSuccessOperationOutcome() {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                .setCode(OperationOutcome.IssueType.INFORMATIONAL)
                .setDiagnostics("Message accepted.");
        return outcome;
    }

}
