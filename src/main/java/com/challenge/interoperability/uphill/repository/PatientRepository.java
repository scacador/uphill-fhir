package com.challenge.interoperability.uphill.repository;

import com.challenge.interoperability.uphill.domain.compositeKeys.PatientId;
import com.challenge.interoperability.uphill.domain.entities.PatientEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, String> {

    @Query(value = "SELECT * FROM public.patients WHERE id = ?1 ORDER BY version DESC LIMIT 1", nativeQuery = true)
    Optional<PatientEntity> findLastVersionById(String id);

    @Query(value = "SELECT * FROM patients, jsonb_array_elements(patient_resource->'identifier') WITH ORDINALITY arr(item_object, position) where arr.item_object\\:\\:json->>'value' = ?2 AND arr.item_object\\:\\:json->>'system' = ?1", nativeQuery = true)
    List<PatientEntity> findByIdentifier(String system, String value);

}
