package com.challenge.interoperability.uphill.domain.entities;

import com.challenge.interoperability.uphill.domain.compositeKeys.EncounterId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

@Entity(name = "encounters")
@Table(name = "encounters", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@IdClass(EncounterId.class)
public class EncounterEntity {

    @Id
    @Column(nullable = false)
    private String id;

    @Id
    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String encounterResource;

}
