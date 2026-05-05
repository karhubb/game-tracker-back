package com.proyectoflutter.backend_api.repository;

import com.proyectoflutter.backend_api.models.Game;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    @Override
    @EntityGraph(attributePaths = "notes")
    @Query("select g from Game g")
    List<Game> findAll();

    @Override
    @EntityGraph(attributePaths = "notes")
    Optional<Game> findById(Long id);
}