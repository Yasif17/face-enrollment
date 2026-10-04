package com.module.facerecognition.entities;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_embedding")
public class FaceEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "person_id",
            nullable = false
    )
    private Person person;

    @Column(
            name = "embedding",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String embedding;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FaceEmbedding() {
    }

    public FaceEmbedding(String embedding) {

        this.embedding = embedding;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public String getEmbedding() {
        return embedding;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setPerson(Person person) {

        this.person = person;
    }

    public void setEmbedding(String embedding) {

        this.embedding = embedding;
    }
}