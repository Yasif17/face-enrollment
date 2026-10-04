package com.module.facerecognition.repositories;

import com.module.facerecognition.entities.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaceEmbeddingRepository
        extends JpaRepository<FaceEmbedding, Long> {

    List<FaceEmbedding> findByPersonId(Long personId);
}