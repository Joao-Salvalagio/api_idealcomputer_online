package com.idealcomputer.crud_basico.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildRequestDTO {

    @NotBlank(message = "Nome da build é obrigatório")
    @JsonProperty("nome_build")
    private String nomeBuild;

    @NotNull(message = "ID da CPU é obrigatório")
    @JsonProperty("id_cpu")
    private Long idCpu;

    @NotNull(message = "ID da Placa-Mãe é obrigatório")
    @JsonProperty("id_placamae")
    private Long idPlacaMae;

    @JsonProperty("id_gpu")
    private Long idGpu;

    @NotNull(message = "ID da Memória RAM é obrigatório")
    @JsonProperty("id_ram")
    private Long idMemoriaRam;

    @NotNull(message = "ID do Armazenamento é obrigatório")
    @JsonProperty("id_armazenamento")
    private Long idArmazenamento;

    @NotNull(message = "ID da Fonte é obrigatório")
    @JsonProperty("id_fonte")
    private Long idFonte;

    @NotNull(message = "ID do Gabinete é obrigatório")
    @JsonProperty("id_gabinete")
    private Long idGabinete;

    @JsonProperty("id_refrigeracao")
    private Long idRefrigeracao;

    @JsonProperty("uso_principal")
    private String usoPrincipal;

    @JsonProperty("detalhe")
    private String detalhe;

    @JsonProperty("orcamento")
    private String orcamento;
}