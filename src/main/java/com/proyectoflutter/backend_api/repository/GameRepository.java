package com.proyectoflutter.backend_api.repository;

import com.proyectoflutter.backend_api.models.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    // Aquí ya tenemos gratis: guardar, borrar, buscar por ID y ver todos los juegos.
}