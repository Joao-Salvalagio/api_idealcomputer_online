package com.idealcomputer.crud_basico.controllers;

import com.idealcomputer.crud_basico.models.UserModel;
import com.idealcomputer.crud_basico.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")  // ✅ CORRIGIDO: Adicionado /api
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService service) {
        this.userService = service;
    }

    // ===========================
    // GET ALL - Listar todos
    // ===========================
    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<List<UserModel>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    // ===========================
    // GET BY ID - Buscar por ID
    // ===========================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UserModel> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // ===========================
    // POST - Criar usuário
    // ===========================
    @PostMapping
    public ResponseEntity<UserModel> create(@RequestBody UserModel user) {
        UserModel newUser = userService.save(user);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();
        return ResponseEntity.created(uri).body(newUser);
    }

    // ===========================
    // PUT - Atualizar usuário
    // ===========================
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UserModel> update(@PathVariable Long id, @RequestBody UserModel user) {
        user.setId(id);
        UserModel updatedUser = userService.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    // ===========================
    // DELETE - Excluir usuário
    // ===========================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}