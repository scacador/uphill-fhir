package com.challenge.interoperability.uphill.domain.entities;

import com.challenge.interoperability.uphill.domain.compositeKeys.PatientId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;


@Entity(name = "patients")
@Table(name = "patients", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@IdClass(PatientId.class)
public class PatientEntity {

    @Id
    @Column(nullable = false)
    private String id;

    @Id
    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String patientResource;

}
