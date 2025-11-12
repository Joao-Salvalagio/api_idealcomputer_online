package com.idealcomputer.crud_basico.dto;

import com.idealcomputer.crud_basico.models.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildResponseDTO {

    private Long id;
    private String nomeBuild;

    // ✅ Usar campos simples ao invés de Models
    private ComponenteDTO cpu;
    private ComponenteDTO placaMae;
    private ComponenteDTO gpu;
    private ComponenteDTO memoriaRam;
    private ComponenteDTO armazenamento;
    private ComponenteDTO fonte;
    private ComponenteDTO gabinete;
    private ComponenteDTO refrigeracao;

    // Metadados
    private String usoPrincipal;
    private String detalhe;
    private String orcamento;
    private BigDecimal precoTotal;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    // Construtor a partir do BuildModel
    public BuildResponseDTO(BuildModel build) {
        this.id = build.getId();
        this.nomeBuild = build.getNomeBuild();

        // ✅ Converter Models para DTOs simples
        this.cpu = new ComponenteDTO(
                build.getCpu().getId(),
                build.getCpu().getNome(),
                build.getCpu().getMarca(),
                build.getCpu().getPreco()
        );

        this.placaMae = new ComponenteDTO(
                build.getPlacaMae().getId(),
                build.getPlacaMae().getNome(),
                build.getPlacaMae().getMarca(),
                build.getPlacaMae().getPreco()
        );

        if (build.getGpu() != null) {
            this.gpu = new ComponenteDTO(
                    build.getGpu().getId(),
                    build.getGpu().getNome(),
                    build.getGpu().getMarca(),
                    build.getGpu().getPreco()
            );
        }

        this.memoriaRam = new ComponenteDTO(
                build.getMemoriaRam().getId(),
                build.getMemoriaRam().getNome(),
                build.getMemoriaRam().getMarca(),
                build.getMemoriaRam().getPreco()
        );

        this.armazenamento = new ComponenteDTO(
                build.getArmazenamento().getId(),
                build.getArmazenamento().getNome(),
                build.getArmazenamento().getMarca(),
                build.getArmazenamento().getPreco()
        );

        this.fonte = new ComponenteDTO(
                build.getFonte().getId(),
                build.getFonte().getNome(),
                build.getFonte().getMarca(),
                build.getFonte().getPreco()
        );

        this.gabinete = new ComponenteDTO(
                build.getGabinete().getId(),
                build.getGabinete().getNome(),
                build.getGabinete().getMarca(),
                build.getGabinete().getPreco()
        );

        if (build.getRefrigeracao() != null) {
            this.refrigeracao = new ComponenteDTO(
                    build.getRefrigeracao().getId(),
                    build.getRefrigeracao().getNome(),
                    build.getRefrigeracao().getMarca(),
                    build.getRefrigeracao().getPreco()
            );
        }

        this.usoPrincipal = build.getUsoPrincipal();
        this.detalhe = build.getDetalhe();
        this.orcamento = build.getOrcamento();
        this.precoTotal = build.getPrecoTotal();
        this.dataCriacao = build.getDataCriacao();
        this.dataAtualizacao = build.getDataAtualizacao();
    }

    // ✅ DTO interno para componentes
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponenteDTO {
        private Long id;
        private String nome;
        private String marca;
        private Double preco;
    }
}