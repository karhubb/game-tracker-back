package com.proyectoflutter.backend_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.proyectoflutter.backend_api.models.EReaction;
import com.proyectoflutter.backend_api.models.Reaction;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

	Optional<Reaction> findByDescription(EReaction description);

}
