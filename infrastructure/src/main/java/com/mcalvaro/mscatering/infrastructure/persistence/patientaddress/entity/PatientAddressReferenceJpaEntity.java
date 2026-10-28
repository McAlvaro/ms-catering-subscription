package com.mcalvaro.mscatering.infrastructure.persistence.patientaddress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "patient_address_references")
public class PatientAddressReferenceJpaEntity {

    @Id
    @Column(name = "address_id", nullable = false, updatable = false)
    private UUID addressId;
    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;
    @Column(name = "street", nullable = false)
    private String street;
    @Column(name = "reference", nullable = false)
    private String reference;
    @Column(name = "city", nullable = false)
    private String city;
    @Column(name = "latitude", nullable = false)
    private Double latitude;
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    public PatientAddressReferenceJpaEntity() {
    }

    public PatientAddressReferenceJpaEntity(UUID addressId, UUID patientId, String street, String reference,
            String city, double latitude, double longitude) {
        this.addressId = addressId;
        this.patientId = patientId;
        this.street = street;
        this.reference = reference;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public UUID getAddressId() {
        return addressId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getStreet() {
        return street;
    }

    public String getReference() {
        return reference;
    }

    public String getCity() {
        return city;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
