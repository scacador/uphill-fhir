package com.challenge.interoperability.uphill.repository;

import com.challenge.interoperability.uphill.domain.entities.EncounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncounterRepository extends JpaRepository<EncounterEntity, UUID> {

    @Query(value = "SELECT * FROM public.encounters WHERE id = ?1 ORDER BY version DESC LIMIT 1", nativeQuery = true)
    Optional<EncounterEntity> findLastVersionById(String id);

    @Query(value = "SELECT * FROM encounters, jsonb_array_elements(encounter_resource->'identifier') WITH ORDINALITY arr(item_object, position) where arr.item_object\\:\\:json->>'value' = ?2 AND arr.item_object\\:\\:json->>'system' = ?1", nativeQuery = true)
    List<EncounterEntity> findByIdentifier(String system, String value);
}
