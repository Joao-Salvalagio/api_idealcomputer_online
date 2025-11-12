package com.idealcomputer.crud_basico.services;

import com.idealcomputer.crud_basico.dto.BuildRequestDTO;
import com.idealcomputer.crud_basico.dto.BuildResponseDTO;
import com.idealcomputer.crud_basico.models.*;
import com.idealcomputer.crud_basico.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildService {

    private final BuildRepository buildRepository;
    private final UserRepository userRepository;
    private final CpuRepository cpuRepository;
    private final PlacaMaeRepository placaMaeRepository;
    private final GpuRepository gpuRepository;
    private final MemoriaRamRepository memoriaRamRepository;
    private final ArmazenamentoRepository armazenamentoRepository;
    private final FonteRepository fonteRepository;
    private final GabineteRepository gabineteRepository;
    private final RefrigeracaoRepository refrigeracaoRepository;

    @Transactional
    public BuildResponseDTO salvarBuild(BuildRequestDTO dto, String emailUsuario) {
        UserModel usuario = userRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        CpuModel cpu = cpuRepository.findById(dto.getIdCpu())
                .orElseThrow(() -> new RuntimeException("CPU não encontrada"));

        PlacaMaeModel placaMae = placaMaeRepository.findById(dto.getIdPlacaMae())
                .orElseThrow(() -> new RuntimeException("Placa-mãe não encontrada"));

        GpuModel gpu = dto.getIdGpu() != null
                ? gpuRepository.findById(dto.getIdGpu()).orElse(null)
                : null;

        MemoriaRamModel memoriaRam = memoriaRamRepository.findById(dto.getIdMemoriaRam())
                .orElseThrow(() -> new RuntimeException("Memória RAM não encontrada"));

        ArmazenamentoModel armazenamento = armazenamentoRepository.findById(dto.getIdArmazenamento())
                .orElseThrow(() -> new RuntimeException("Armazenamento não encontrado"));

        FonteModel fonte = fonteRepository.findById(dto.getIdFonte())
                .orElseThrow(() -> new RuntimeException("Fonte não encontrada"));

        GabineteModel gabinete = gabineteRepository.findById(dto.getIdGabinete())
                .orElseThrow(() -> new RuntimeException("Gabinete não encontrado"));

        RefrigeracaoModel refrigeracao = dto.getIdRefrigeracao() != null
                ? refrigeracaoRepository.findById(dto.getIdRefrigeracao()).orElse(null)
                : null;

        BigDecimal precoTotal = calcularPrecoTotal(cpu, placaMae, gpu, memoriaRam,
                armazenamento, fonte, gabinete, refrigeracao);

        BuildModel build = BuildModel.builder()
                .nomeBuild(dto.getNomeBuild())
                .usuario(usuario)
                .cpu(cpu)
                .placaMae(placaMae)
                .gpu(gpu)
                .memoriaRam(memoriaRam)
                .armazenamento(armazenamento)
                .fonte(fonte)
                .gabinete(gabinete)
                .refrigeracao(refrigeracao)
                .usoPrincipal(dto.getUsoPrincipal())
                .detalhe(dto.getDetalhe())
                .orcamento(dto.getOrcamento())
                .precoTotal(precoTotal)
                .build();

        BuildModel buildSalva = buildRepository.save(build);
        return new BuildResponseDTO(buildSalva);
    }

    /**
     * Lista todas as builds do usuário autenticado
     * ✅ EAGER: Força carregamento de todos os componentes
     */
    @Transactional(readOnly = true)
    public List<BuildResponseDTO> listarMinhasBuilds(String emailUsuario) {
        UserModel usuario = userRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        List<BuildModel> builds = buildRepository.findByUsuarioOrderByDataCriacaoDesc(usuario);

        return builds.stream()
                .map(build -> {
                    // ✅ Força o carregamento de todos os componentes ANTES de criar o DTO
                    build.getCpu().getPreco(); // Força inicialização
                    build.getPlacaMae().getPreco();
                    if (build.getGpu() != null) build.getGpu().getPreco();
                    build.getMemoriaRam().getPreco();
                    build.getArmazenamento().getPreco();
                    build.getFonte().getPreco();
                    build.getGabinete().getPreco();
                    if (build.getRefrigeracao() != null) build.getRefrigeracao().getPreco();

                    return new BuildResponseDTO(build);
                })
                .collect(Collectors.toList());
    }

    /**
     * Busca uma build específica por ID
     * ✅ EAGER: Força carregamento de todos os componentes
     */
    @Transactional(readOnly = true)
    public BuildResponseDTO buscarBuildPorId(Long id, String emailUsuario) {
        BuildModel build = buildRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Build não encontrada"));

        // Verifica se a build pertence ao usuário
        if (!build.getUsuario().getEmail().equals(emailUsuario)) {
            throw new RuntimeException("Acesso negado: esta build não pertence ao usuário");
        }

        // ✅ Força o carregamento de todos os componentes ANTES de criar o DTO
        build.getCpu().getPreco();
        build.getPlacaMae().getPreco();
        if (build.getGpu() != null) build.getGpu().getPreco();
        build.getMemoriaRam().getPreco();
        build.getArmazenamento().getPreco();
        build.getFonte().getPreco();
        build.getGabinete().getPreco();
        if (build.getRefrigeracao() != null) build.getRefrigeracao().getPreco();

        return new BuildResponseDTO(build);
    }

    @Transactional
    public void deletarBuild(Long id, String emailUsuario) {
        BuildModel build = buildRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Build não encontrada"));

        if (!build.getUsuario().getEmail().equals(emailUsuario)) {
            throw new RuntimeException("Acesso negado: não é possível deletar build de outro usuário");
        }

        buildRepository.delete(build);
    }

    /**
     * Calcula o preço total da build
     * ✅ CORREÇÃO: Converte Double para BigDecimal
     */
    private BigDecimal calcularPrecoTotal(CpuModel cpu, PlacaMaeModel placaMae, GpuModel gpu,
                                          MemoriaRamModel memoriaRam, ArmazenamentoModel armazenamento,
                                          FonteModel fonte, GabineteModel gabinete,
                                          RefrigeracaoModel refrigeracao) {
        BigDecimal total = BigDecimal.ZERO;

        total = total.add(BigDecimal.valueOf(cpu.getPreco()));
        total = total.add(BigDecimal.valueOf(placaMae.getPreco()));
        if (gpu != null) total = total.add(BigDecimal.valueOf(gpu.getPreco()));
        total = total.add(BigDecimal.valueOf(memoriaRam.getPreco()));
        total = total.add(BigDecimal.valueOf(armazenamento.getPreco()));
        total = total.add(BigDecimal.valueOf(fonte.getPreco()));
        total = total.add(BigDecimal.valueOf(gabinete.getPreco()));
        if (refrigeracao != null) total = total.add(BigDecimal.valueOf(refrigeracao.getPreco()));

        return total;
    }
}