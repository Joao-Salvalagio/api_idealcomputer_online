package com.idealcomputer.crud_basico.models;

import com.idealcomputer.crud_basico.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_builds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_build")
    private Long id;

    @Column(name = "nome_build", nullable = false, length = 255)
    private String nomeBuild;

    // ========================================
    // RELACIONAMENTO COM USUÁRIO
    // ========================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UserModel usuario;

    // ========================================
    // RELACIONAMENTOS COM COMPONENTES
    // ========================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cpu", nullable = false)
    private CpuModel cpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_placa_mae", nullable = false)
    private PlacaMaeModel placaMae;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gpu")
    private GpuModel gpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_memoria_ram", nullable = false)
    private MemoriaRamModel memoriaRam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_armazenamento", nullable = false)
    private ArmazenamentoModel armazenamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fonte", nullable = false)
    private FonteModel fonte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gabinete", nullable = false)
    private GabineteModel gabinete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_refrigeracao")
    private RefrigeracaoModel refrigeracao;

    // ========================================
    // METADADOS DA BUILD
    // ========================================
    @Column(name = "uso_principal", length = 50)
    private String usoPrincipal; // Jogos, Trabalho, Estudos

    @Column(name = "detalhe", length = 100)
    private String detalhe; // Leves, Intermediários, Pesados, etc

    @Column(name = "orcamento", length = 50)
    private String orcamento; // Econômico, Intermediário, Alto, Extremo

    @Column(name = "preco_total", precision = 10, scale = 2)
    private BigDecimal precoTotal;

    // ========================================
    // TIMESTAMPS
    // ========================================
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}