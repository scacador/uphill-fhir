package com.challenge.interoperability.uphill.service;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import com.challenge.interoperability.uphill.domain.FhirR4ParserFactory;
import com.challenge.interoperability.uphill.domain.entities.EncounterEntity;
import com.challenge.interoperability.uphill.domain.entities.PatientEntity;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    IParser parser = FhirR4ParserFactory.getParser();

    @Autowired
    private PatientService patientService;

    @Autowired
    private EncounterService encounterService;

    public ResponseEntity<String> receiveAndValidateMessage(Bundle receivedMessage) throws DataFormatException {

        List<Resource> resources = new ArrayList<>();
        for(Bundle.BundleEntryComponent entry : receivedMessage.getEntry()){
            resources.add(entry.getResource());
        }

        //OperationOutcome to return in case of insuccess
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

        //Create Bundle and MessageHeader for success response
        Bundle bundleResponse = new Bundle();
        bundleResponse.setType(Bundle.BundleType.MESSAGE);

        MessageHeader responseHeader = createResponseMessageHeader();
        bundleResponse.addEntry(new Bundle.BundleEntryComponent().setResource(responseHeader));

        //save Patients
        for (Resource receivedPatientResource : patients){

            Patient patient = (Patient) receivedPatientResource;

            //check if received id of patient already exists
            Optional<PatientEntity> existingPatient = patientService.getById(receivedPatientResource.getIdPart());

            //create PatientEntity
            PatientEntity patientEntity = new PatientEntity();
            if(patient.getIdPart() == null){
                UUID generatedId = UUID.randomUUID();
                patientEntity.setId(String.valueOf(generatedId));
            } else patientEntity.setId(patient.getIdPart());
            patientEntity.setPatientResource(parser.encodeResourceToString(patient));
            //set version to PatientEntity according to previous existence
            if(existingPatient.isPresent()){
                patientEntity.setVersion(existingPatient.get().getVersion()+1);
            } else {
                patientEntity.setVersion(1);
            }

            //save PatientEntity
            PatientEntity createdPatientEntity = patientService.createPatient(patientEntity);
            Patient createdPatient = parser.parseResource(Patient.class, createdPatientEntity.getPatientResource());

            //Create Meta for the created Patient
            Meta patientMeta = new Meta();
            patientMeta.setVersionId(String.valueOf(createdPatientEntity.getVersion()));
            createdPatient.setMeta(patientMeta);
            createdPatient.setId(createdPatientEntity.getId());

            //Create BundleEntryComponent of the created Patient
            Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
            patientEntry.setResource(createdPatient).setFullUrl("http://localhost:8080/fhir/Patient/"+createdPatientEntity.getId());

            //Add created Patient to response Bundle and add focus to MessageHeader
            bundleResponse.addEntry(patientEntry);
            responseHeader.addFocus(new Reference().setReference("Patient/"+createdPatientEntity.getId()));
        }

        //save Encounters
        for (Resource encounterResource : encounters){

            Encounter encounter = (Encounter) encounterResource;

            //check if received id of patient already exists
            Optional<EncounterEntity> existingEncounter = encounterService.getById(encounterResource.getIdPart());

            //create EncounterEntity
            EncounterEntity encounterEntity = new EncounterEntity();
            if(encounter.getIdPart() == null){
                UUID generatedId = UUID.randomUUID();
                encounterEntity.setId(String.valueOf(generatedId));
            } else encounterEntity.setId(encounter.getIdPart());
            encounterEntity.setEncounterResource(parser.encodeResourceToString(encounter));
            //set version to EncounterEntity according to previous existence
            if(existingEncounter.isPresent()){
                encounterEntity.setVersion(existingEncounter.get().getVersion()+1);
            } else {
                encounterEntity.setVersion(1);
            }

            //save EncounterEntity
            EncounterEntity createdEncounterEntity = encounterService.createEncounter(encounterEntity);
            Encounter createdEncounter = parser.parseResource(Encounter.class, createdEncounterEntity.getEncounterResource());

            //Create Meta for the created Encounter
            Meta encounterMeta = new Meta();
            encounterMeta.setVersionId(String.valueOf(createdEncounterEntity.getVersion()));
            createdEncounter.setMeta(encounterMeta);
            createdEncounter.setId(createdEncounterEntity.getId());

            //Create BundleEntryComponent of the created Encounter
            Bundle.BundleEntryComponent encounterEntry = new Bundle.BundleEntryComponent();
            encounterEntry.setResource(createdEncounter).setFullUrl("http://localhost:8080/fhir/Encounter/"+createdEncounterEntity.getId());

            //Add created Encounter to response Bundle and add focus to MessageHeader
            bundleResponse.addEntry(encounterEntry);
            responseHeader.addFocus(new Reference().setReference("Encounter/"+createdEncounterEntity.getId()));
        }

        bundleResponse.setTimestamp(new Date());
        return new ResponseEntity<>(parser.encodeResourceToString(bundleResponse), HttpStatus.CREATED);
    }

    private boolean isMessageHeaderValid(MessageHeader messageHeader) {
        return messageHeader.getFocus().stream().anyMatch(f -> f.getReference().contains("Encounter"));
    }

    private boolean isPatientValid(Patient patient) {
        boolean patientIdentifier = patient.getIdentifier().stream().anyMatch(i -> i.getSystem().equals("urn:uh-patient-id"));
        return patientIdentifier && patient.hasName() && patient.hasTelecom() && patient.hasGender() && patient.hasBirthDate();
    }

    private boolean isEncounterValid(Encounter encounter) {
        boolean encounterIdentifier = encounter.hasIdentifier() && encounter.getIdentifier().stream().anyMatch(i -> i.getSystem().equals("urn:uh-encounter-id"));
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

    private MessageHeader createResponseMessageHeader() {
        MessageHeader responseHeader = new MessageHeader();
        MessageHeader.MessageHeaderResponseComponent messageHeaderResponseComponent = new MessageHeader.MessageHeaderResponseComponent();
        messageHeaderResponseComponent.setCode(MessageHeader.ResponseType.OK);
        responseHeader.setResponse(messageHeaderResponseComponent);
        return responseHeader;
    }

}
