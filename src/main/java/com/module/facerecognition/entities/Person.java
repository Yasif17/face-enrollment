package com.module.facerecognition.entities;

import com.module.facerecognition.auth.entities.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "person")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(
            mappedBy = "person",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<FaceEmbedding> faceEmbeddings =
            new ArrayList<>();

    public Person() {
    }

    public Person(String name) {

        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<FaceEmbedding> getFaceEmbeddings() {
        return faceEmbeddings;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addFaceEmbedding(
            FaceEmbedding faceEmbedding) {

        faceEmbeddings.add(faceEmbedding);
        faceEmbedding.setPerson(this);
    }

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private AppUser user;

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }


}