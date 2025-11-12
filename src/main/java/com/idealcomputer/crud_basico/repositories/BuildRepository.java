package com.idealcomputer.crud_basico.repositories;

import com.idealcomputer.crud_basico.models.BuildModel;
import com.idealcomputer.crud_basico.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildRepository extends JpaRepository<BuildModel, Long> {

    // Busca todas as builds de um usuário
    List<BuildModel> findByUsuario(UserModel usuario);

    // Busca builds de um usuário ordenadas por data de criação (mais recentes primeiro)
    List<BuildModel> findByUsuarioOrderByDataCriacaoDesc(UserModel usuario);
}