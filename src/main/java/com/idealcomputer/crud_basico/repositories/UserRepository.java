package com.idealcomputer.crud_basico.repositories;

import com.idealcomputer.crud_basico.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {

    // Método para login (buscar por email)
    Optional<UserModel> findByEmail(String email);

    // Método para busca na tela de admin (buscar por nome ou email)
    List<UserModel> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
}