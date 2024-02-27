package com.challenge.interoperability.uphill.repository;

import com.challenge.interoperability.uphill.domain.entities.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, String> {

    @Query(value = "SELECT * FROM public.patients WHERE id = ?1 ORDER BY version DESC LIMIT 1", nativeQuery = true)
    Optional<PatientEntity> findLastVersionById(String id);

    @Query(value = "SELECT id, version, patient_resource FROM ( SELECT id, version, patient_resource, ROW_NUMBER() OVER (PARTITION BY id ORDER BY version DESC) AS row_num FROM (SELECT * FROM patients, jsonb_array_elements(patient_resource->'identifier') WITH ORDINALITY arr(item_object, position) WHERE arr.item_object\\:\\:json->>'value' = ?2 AND arr.item_object\\:\\:json->>'system' = ?1) AS test ) AS ranked WHERE row_num = 1", nativeQuery = true)
    List<PatientEntity> findByIdentifier(String system, String value);

}