package com.idealcomputer.crud_basico.controllers;

import com.idealcomputer.crud_basico.dto.BuildRequestDTO;
import com.idealcomputer.crud_basico.dto.BuildResponseDTO;
import com.idealcomputer.crud_basico.services.BuildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
// ❌ REMOVIDO: @CrossOrigin(origins = "http://localhost:5173")
public class BuildController {

    private final BuildService buildService;

    @PostMapping("/save")
    public ResponseEntity<BuildResponseDTO> salvarBuild(
            @Valid @RequestBody BuildRequestDTO dto,
            Authentication authentication) {

        String emailUsuario = authentication.getName();
        BuildResponseDTO response = buildService.salvarBuild(dto, emailUsuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-builds")
    public ResponseEntity<List<BuildResponseDTO>> listarMinhasBuilds(Authentication authentication) {
        String emailUsuario = authentication.getName();
        List<BuildResponseDTO> builds = buildService.listarMinhasBuilds(emailUsuario);

        return ResponseEntity.ok(builds);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildResponseDTO> buscarBuildPorId(
            @PathVariable Long id,
            Authentication authentication) {

        String emailUsuario = authentication.getName();
        BuildResponseDTO build = buildService.buscarBuildPorId(id, emailUsuario);

        return ResponseEntity.ok(build);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarBuild(
            @PathVariable Long id,
            Authentication authentication) {

        String emailUsuario = authentication.getName();
        buildService.deletarBuild(id, emailUsuario);

        return ResponseEntity.noContent().build();
    }
}