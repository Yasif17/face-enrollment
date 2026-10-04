package com.module.facerecognition.repositories;


import com.module.facerecognition.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface PersonRepository
        extends JpaRepository<Person, Long> {

    @Query("""
            SELECT DISTINCT p
            FROM Person p
            LEFT JOIN FETCH p.faceEmbeddings
            """)
    List<Person> findAllWithFaceEmbeddings();

    Optional<Person> findByUserId(Long userId);

}