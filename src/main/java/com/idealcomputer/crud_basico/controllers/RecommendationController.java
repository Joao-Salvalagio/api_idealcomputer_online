package com.idealcomputer.crud_basico.controllers;

import com.idealcomputer.crud_basico.dto.RecommendationRequestDTO;
import com.idealcomputer.crud_basico.dto.RecommendationResponseDTO;
import com.idealcomputer.crud_basico.services.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping(value = "/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    // ✅ Formatter para exibir data/hora nos logs
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @PostMapping("/generate")
    public ResponseEntity<RecommendationResponseDTO> generate(@RequestBody RecommendationRequestDTO request) {
        // ✅ LOG: Início da requisição
        System.out.println("🔵 ========================================");
        System.out.println("🔵 RECEBIDA REQUISIÇÃO PARA GERAR RECOMENDAÇÃO");
        System.out.println("🔵 ========================================");
        System.out.println("🔵 Horário: " + LocalDateTime.now().format(formatter));
        System.out.println("🔵 Dados recebidos:");
        System.out.println("🔵   - Usage: " + request.getUsage());
        System.out.println("🔵   - Budget: " + request.getBudget());
        System.out.println("🔵   - Detail: " + request.getDetail());
        System.out.println("🔵 ----------------------------------------");

        long startTime = System.currentTimeMillis();

        try {
            // ✅ LOG: Chamando o serviço
            System.out.println("🔵 Chamando o serviço de recomendação...");

            RecommendationResponseDTO response = recommendationService.generateBuild(request);

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000; // Segundos

            // ✅ LOG: Sucesso
            System.out.println("✅ ========================================");
            System.out.println("✅ RECOMENDAÇÃO GERADA COM SUCESSO!");
            System.out.println("✅ ========================================");
            System.out.println("✅ Tempo de processamento: " + duration + " segundos");
            System.out.println("✅ Componentes retornados:");
            System.out.println("✅   - CPU: " + (response.getCpu() != null ? response.getCpu().getNome() : "Não encontrada"));
            System.out.println("✅   - GPU: " + (response.getGpu() != null ? response.getGpu().getNome() : "Nenhuma"));
            System.out.println("✅   - Placa-mãe: " + (response.getPlacaMae() != null ? response.getPlacaMae().getNome() : "Não encontrada"));
            System.out.println("✅   - Memória RAM: " + (response.getMemoriaRam() != null ? response.getMemoriaRam().getNome() : "Não encontrada"));
            System.out.println("✅   - Armazenamento: " + (response.getArmazenamento() != null ? response.getArmazenamento().getNome() : "Não encontrado"));
            System.out.println("✅   - Fonte: " + (response.getFonte() != null ? response.getFonte().getNome() : "Não encontrada"));
            System.out.println("✅   - Gabinete: " + (response.getGabinete() != null ? response.getGabinete().getNome() : "Não encontrado"));
            System.out.println("✅   - Refrigeração: " + (response.getRefrigeracao() != null ? response.getRefrigeracao().getNome() : "Nenhuma"));
            System.out.println("✅ ========================================");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // ✅ LOG: Erro de validação (400)
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;

            System.err.println("⚠️  ========================================");
            System.err.println("⚠️  ERRO DE VALIDAÇÃO!");
            System.err.println("⚠️  ========================================");
            System.err.println("⚠️  Tempo até o erro: " + duration + " segundos");
            System.err.println("⚠️  Mensagem: " + e.getMessage());
            System.err.println("⚠️  ========================================");

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null);

        } catch (RuntimeException e) {
            // ✅ LOG: Erro de runtime (componente não encontrado, etc.)
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;

            System.err.println("❌ ========================================");
            System.err.println("❌ ERRO AO GERAR RECOMENDAÇÃO!");
            System.err.println("❌ ========================================");
            System.err.println("❌ Tempo até o erro: " + duration + " segundos");
            System.err.println("❌ Tipo de erro: " + e.getClass().getSimpleName());
            System.err.println("❌ Mensagem: " + e.getMessage());
            System.err.println("❌ ========================================");
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);

        } catch (Exception e) {
            // ✅ LOG: Erro genérico (500)
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;

            System.err.println("❌ ========================================");
            System.err.println("❌ ERRO CRÍTICO AO GERAR RECOMENDAÇÃO!");
            System.err.println("❌ ========================================");
            System.err.println("❌ Tempo até o erro: " + duration + " segundos");
            System.err.println("❌ Tipo de erro: " + e.getClass().getSimpleName());
            System.err.println("❌ Mensagem: " + e.getMessage());
            System.err.println("❌ ========================================");
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}