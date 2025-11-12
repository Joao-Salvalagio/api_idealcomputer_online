package com.idealcomputer.crud_basico.services;

import com.idealcomputer.crud_basico.dto.RecommendationRequestDTO;
import com.idealcomputer.crud_basico.dto.RecommendationResponseDTO;
import com.idealcomputer.crud_basico.models.*;
import com.idealcomputer.crud_basico.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CpuRepository cpuRepository;
    private final PlacaMaeRepository placaMaeRepository;
    private final GpuRepository gpuRepository;
    private final MemoriaRamRepository memoriaRamRepository;
    private final ArmazenamentoRepository armazenamentoRepository;
    private final FonteRepository fonteRepository;
    private final GabineteRepository gabineteRepository;
    private final RefrigeracaoRepository refrigeracaoRepository;

    private static class PlatformKit {
        CpuModel cpu;
        PlacaMaeModel placaMae;
        MemoriaRamModel memoriaRam;
        double totalCost;

        PlatformKit(CpuModel cpu, PlacaMaeModel placaMae, MemoriaRamModel memoriaRam) {
            this.cpu = cpu;
            this.placaMae = placaMae;
            this.memoriaRam = memoriaRam;
            this.totalCost = cpu.getPreco() + placaMae.getPreco() + memoriaRam.getPreco();
        }
    }

    public RecommendationResponseDTO generateBuild(RecommendationRequestDTO request) {

        double maxBudget = getBudgetLimit(request.getBudget());

        // Gera todos os kits possíveis
        List<PlatformKit> allPossibleKits = new ArrayList<>();
        for (CpuModel cpu : cpuRepository.findAll()) {
            for (PlacaMaeModel pm : placaMaeRepository.findAll()) {
                if (pm.getSoqueteCpu().equalsIgnoreCase(cpu.getSoquete())) {
                    for (MemoriaRamModel ram : memoriaRamRepository.findAll()) {
                        if (ram.getTipo().equalsIgnoreCase(pm.getTipoRamSuportado())) {
                            allPossibleKits.add(new PlatformKit(cpu, pm, ram));
                        }
                    }
                }
            }
        }

        // ✅ NOVO: Calcula orçamentos de cada componente
        BudgetAllocation allocation = calculateBudgetAllocation(maxBudget, request);

        // Filtra kits válidos
        List<PlatformKit> validKits = allPossibleKits.stream()
                .filter(kit -> kit.totalCost <= allocation.platformBudget)
                .filter(kit -> filterKitByUsage(kit, request))
                .filter(kit -> filterRamByBudget(kit, request.getBudget())) // ✅ NOVO
                .sorted(Comparator.comparingDouble((PlatformKit kit) -> kit.totalCost).reversed())
                .collect(Collectors.toList());

        if (validKits.isEmpty()) {
            throw new RuntimeException("Não foi possível encontrar um kit compatível. Tente um orçamento maior.");
        }

        boolean isBudgetBuild = request.getBudget().equalsIgnoreCase("econômico");

        if (isBudgetBuild) {
            validKits.sort(Comparator.comparingDouble(kit -> kit.totalCost));
        }

        // Tenta montar a build completa
        for (PlatformKit currentKit : validKits) {
            double remainingBudget = maxBudget - currentKit.totalCost;

            // ✅ 1. Refrigeração (se necessária)
            RefrigeracaoModel selectedRefrigeracao = null;
            if (requiresSeparateCooler(currentKit.cpu)) {
                selectedRefrigeracao = selectRefrigeracao(currentKit.cpu, allocation.coolerBudget, maxBudget);
                if (selectedRefrigeracao != null) {
                    remainingBudget -= selectedRefrigeracao.getPreco();
                }
            }

            // ✅ 2. GPU (prioridade em builds gaming)
            GpuModel selectedGpu = null;
            if (requiresGpu(request)) {
                selectedGpu = selectGpu(allocation.gpuBudget, request);
                if (selectedGpu != null) {
                    remainingBudget -= selectedGpu.getPreco();
                }
            }

            // ✅ 3. Armazenamento (escalável)
            ArmazenamentoModel selectedArmazenamento = selectArmazenamento(allocation.storageBudget, maxBudget);
            if (selectedArmazenamento != null) {
                remainingBudget -= selectedArmazenamento.getPreco();
            }

            // ✅ 4. Gabinete (compatível e escalável)
            GabineteModel selectedGabinete = selectGabinete(currentKit.placaMae, allocation.caseBudget);
            if (selectedGabinete == null) continue;
            remainingBudget -= selectedGabinete.getPreco();

            // ✅ 5. Fonte (compatível e adequada)
            double potenciaNecessaria = calculateRequiredWattage(currentKit.cpu, selectedGpu, maxBudget);
            FonteModel selectedFonte = selectFonte(currentKit.placaMae, selectedGabinete, remainingBudget, potenciaNecessaria);
            if (selectedFonte == null) continue;
            remainingBudget -= selectedFonte.getPreco();

            // Verifica se todos os componentes obrigatórios foram encontrados
            if (selectedArmazenamento != null && selectedFonte != null && selectedGabinete != null && remainingBudget >= -200) {
                RecommendationResponseDTO response = new RecommendationResponseDTO();
                response.setCpu(currentKit.cpu);
                response.setPlacaMae(currentKit.placaMae);
                response.setMemoriaRam(currentKit.memoriaRam);
                response.setGpu(selectedGpu);
                response.setArmazenamento(selectedArmazenamento);
                response.setFonte(selectedFonte);
                response.setGabinete(selectedGabinete);
                response.setRefrigeracao(selectedRefrigeracao);
                return response;
            }
        }

        throw new RuntimeException("Não foi possível montar uma configuração completa. Tente um orçamento maior ou cadastre mais peças.");
    }

    // ========================================
    // ✅ NOVO: ALOCAÇÃO DE ORÇAMENTO INTELIGENTE
    // ========================================

    private static class BudgetAllocation {
        double platformBudget;
        double gpuBudget;
        double storageBudget;
        double caseBudget;
        double coolerBudget;
    }

    private BudgetAllocation calculateBudgetAllocation(double maxBudget, RecommendationRequestDTO request) {
        BudgetAllocation allocation = new BudgetAllocation();
        String usage = request.getUsage().toLowerCase();
        String detail = request.getDetail().toLowerCase();

        // ✅ GAMING: GPU é PRIORIDADE
        if (usage.equals("jogos")) {
            if (detail.contains("pesados") || detail.contains("todo tipo")) {
                allocation.platformBudget = maxBudget * 0.35;  // 35% CPU+Placa+RAM
                allocation.gpuBudget = maxBudget * 0.40;       // 40% GPU
                allocation.storageBudget = maxBudget * 0.08;   // 8% Storage
                allocation.caseBudget = maxBudget * 0.08;      // 8% Case
                allocation.coolerBudget = maxBudget * 0.09;    // 9% Cooler
            } else {
                allocation.platformBudget = maxBudget * 0.40;
                allocation.gpuBudget = maxBudget * 0.30;
                allocation.storageBudget = maxBudget * 0.10;
                allocation.caseBudget = maxBudget * 0.10;
                allocation.coolerBudget = maxBudget * 0.10;
            }
        }
        // ✅ TRABALHO: CPU e Storage prioridade
        else if (usage.equals("trabalho")) {
            allocation.platformBudget = maxBudget * 0.45;
            allocation.gpuBudget = maxBudget * 0.20;
            allocation.storageBudget = maxBudget * 0.15;  // Mais storage
            allocation.caseBudget = maxBudget * 0.10;
            allocation.coolerBudget = maxBudget * 0.10;
        }
        // ✅ ESTUDOS: Balanceado (sem GPU)
        else {
            allocation.platformBudget = maxBudget * 0.60;
            allocation.gpuBudget = 0;
            allocation.storageBudget = maxBudget * 0.15;
            allocation.caseBudget = maxBudget * 0.15;
            allocation.coolerBudget = maxBudget * 0.10;
        }

        return allocation;
    }

    // ========================================
    // ✅ NOVO: SELEÇÃO INTELIGENTE DE GPU
    // ========================================

    private GpuModel selectGpu(double budget, RecommendationRequestDTO request) {
        String detail = request.getDetail().toLowerCase();
        List<GpuModel> gpus = gpuRepository.findAll().stream()
                .filter(g -> g.getPreco() <= budget)
                .sorted(Comparator.comparing(GpuModel::getPreco).reversed())
                .collect(Collectors.toList());

        if (gpus.isEmpty()) return null;

        // ✅ Para builds top: busca GPUs com 16GB+ VRAM
        if (budget > 5000 && (detail.contains("pesados") || detail.contains("todo tipo") || detail.contains("edição"))) {
            return gpus.stream()
                    .filter(g -> g.getMemoriaVram() >= 16)
                    .max(Comparator.comparing(GpuModel::getPreco))
                    .orElse(gpus.get(0)); // Fallback: mais cara disponível
        }

        // ✅ Outras builds: melhor GPU no orçamento
        return gpus.get(0);
    }

    // ========================================
    // ✅ NOVO: SELEÇÃO INTELIGENTE DE ARMAZENAMENTO
    // ========================================

    private ArmazenamentoModel selectArmazenamento(double budget, double maxBudget) {
        List<ArmazenamentoModel> nvmes = armazenamentoRepository.findAll().stream()
                .filter(a -> a.getTipo().equalsIgnoreCase("SSD NVMe"))
                .filter(a -> a.getPreco() <= budget)
                .sorted(Comparator.comparing(ArmazenamentoModel::getCapacidadeGb).reversed()
                        .thenComparing(ArmazenamentoModel::getPreco))
                .collect(Collectors.toList());

        // ✅ Prioriza maior capacidade dentro do orçamento
        if (!nvmes.isEmpty()) {
            // Orçamento alto: 2TB+
            if (maxBudget >= 12000) {
                return nvmes.stream()
                        .filter(a -> a.getCapacidadeGb() >= 2000)
                        .findFirst()
                        .orElse(nvmes.get(0));
            }
            // Orçamento médio: 1TB
            else if (maxBudget >= 7000) {
                return nvmes.stream()
                        .filter(a -> a.getCapacidadeGb() >= 1000)
                        .findFirst()
                        .orElse(nvmes.get(0));
            }
            // Orçamento baixo: 500GB
            return nvmes.stream()
                    .filter(a -> a.getCapacidadeGb() >= 500)
                    .min(Comparator.comparing(ArmazenamentoModel::getPreco))
                    .orElse(nvmes.get(0));
        }

        // Fallback: SSD SATA
        return armazenamentoRepository.findAll().stream()
                .filter(a -> a.getTipo().equalsIgnoreCase("SSD SATA"))
                .filter(a -> a.getPreco() <= budget)
                .min(Comparator.comparing(ArmazenamentoModel::getPreco))
                .orElse(null);
    }

    // ========================================
    // ✅ NOVO: SELEÇÃO INTELIGENTE DE GABINETE
    // ========================================

    private GabineteModel selectGabinete(PlacaMaeModel placaMae, double budget) {
        String formatoPlacaMae = placaMae.getFormato().toLowerCase();

        List<GabineteModel> compatibleCases = gabineteRepository.findAll().stream()
                .filter(g -> g.getPreco() <= budget)
                .filter(g -> {
                    String suportados = g.getFormatosPlacaMaeSuportados().toLowerCase();
                    if (formatoPlacaMae.contains("mini-itx")) return true;
                    if (formatoPlacaMae.contains("micro-atx") || formatoPlacaMae.contains("m-atx")) {
                        return suportados.contains("micro-atx") || suportados.contains("m-atx") || suportados.contains("atx");
                    }
                    if (formatoPlacaMae.contains("atx") && !formatoPlacaMae.contains("micro") && !formatoPlacaMae.contains("mini")) {
                        return suportados.contains("atx");
                    }
                    return false;
                })
                .sorted(Comparator.comparing(GabineteModel::getPreco))
                .collect(Collectors.toList());

        if (compatibleCases.isEmpty()) return null;

        // ✅ Orçamento alto: gabinete melhor (meio da lista)
        if (budget > 600) {
            int index = Math.min(compatibleCases.size() / 2, compatibleCases.size() - 1);
            return compatibleCases.get(index);
        }

        // ✅ Orçamento baixo: mais barato compatível
        return compatibleCases.get(0);
    }

    // ========================================
    // ✅ NOVO: SELEÇÃO INTELIGENTE DE REFRIGERAÇÃO
    // ========================================

    private RefrigeracaoModel selectRefrigeracao(CpuModel cpu, double budget, double maxBudget) {
        String cpuSocket = cpu.getSoquete();
        boolean isHighEnd = isHighEndCpu(cpu);

        List<RefrigeracaoModel> coolers = refrigeracaoRepository.findAll().stream()
                .filter(c -> c.getSoquetesCpuSuportados().toUpperCase().contains(cpuSocket.toUpperCase()))
                .filter(c -> c.getPreco() <= budget)
                .collect(Collectors.toList());

        if (coolers.isEmpty()) return null;

        // ✅ CPU high-end + orçamento alto: Water Cooler 280mm/360mm
        if (isHighEnd && maxBudget >= 10000) {
            RefrigeracaoModel waterCooler = coolers.stream()
                    .filter(c -> c.getTipo().equalsIgnoreCase("Water Cooler"))
                    .filter(c -> c.getNome().contains("360") || c.getNome().contains("280"))
                    .max(Comparator.comparing(RefrigeracaoModel::getPreco))
                    .orElse(null);

            if (waterCooler != null) return waterCooler;
        }

        // ✅ CPU high-end: Water Cooler 240mm+
        if (isHighEnd) {
            RefrigeracaoModel waterCooler = coolers.stream()
                    .filter(c -> c.getTipo().equalsIgnoreCase("Water Cooler"))
                    .min(Comparator.comparing(RefrigeracaoModel::getPreco))
                    .orElse(null);

            if (waterCooler != null) return waterCooler;
        }

        // ✅ CPUs normais: Air Cooler
        return coolers.stream()
                .filter(c -> c.getTipo().equalsIgnoreCase("Air Cooler"))
                .min(Comparator.comparing(RefrigeracaoModel::getPreco))
                .orElse(coolers.get(0));
    }

    // ========================================
    // ✅ NOVO: SELEÇÃO INTELIGENTE DE FONTE
    // ========================================

    private FonteModel selectFonte(PlacaMaeModel placaMae, GabineteModel gabinete, double budget, double requiredWattage) {
        String formatoPlacaMae = placaMae.getFormato().toLowerCase();
        String formatosGabinete = gabinete.getFormatosPlacaMaeSuportados().toLowerCase();

        return fonteRepository.findAll().stream()
                .filter(f -> f.getPotenciaWatts() >= requiredWattage)
                .filter(f -> f.getPreco() <= budget)
                .filter(f -> {
                    String formatoFonte = f.getFormato().toLowerCase();

                    if (formatoPlacaMae.contains("mini-itx")) {
                        if (formatoFonte.contains("sfx")) return true;
                        return formatoFonte.contains("atx") && formatosGabinete.contains("atx");
                    }

                    if (formatoPlacaMae.contains("micro-atx") || formatoPlacaMae.contains("m-atx")) {
                        if (!formatosGabinete.contains("atx") || formatosGabinete.contains("micro-atx")) {
                            return formatoFonte.contains("sfx");
                        }
                        return formatoFonte.contains("atx") || formatoFonte.contains("sfx");
                    }

                    if (formatoPlacaMae.contains("atx") && !formatoPlacaMae.contains("micro") && !formatoPlacaMae.contains("mini")) {
                        return formatoFonte.contains("atx") || formatoFonte.contains("sfx");
                    }

                    return false;
                })
                .min(Comparator.comparing(FonteModel::getPreco))
                .orElse(null);
    }

    // ========================================
    // ✅ NOVO: FILTRO DE RAM POR ORÇAMENTO
    // ========================================

    private boolean filterRamByBudget(PlatformKit kit, String budgetCategory) {
        int ramCapacity = kit.memoriaRam.getCapacidadeGb();

        return switch (budgetCategory.toLowerCase()) {
            case "econômico" -> ramCapacity <= 16;      // Máx 16GB
            case "intermediário" -> ramCapacity <= 32;  // Máx 32GB
            case "alto" -> ramCapacity <= 64;           // Máx 64GB
            case "extremo" -> true;                     // Sem limite
            default -> ramCapacity <= 32;
        };
    }

    // ========================================
    // MÉTODOS AUXILIARES (SEM MUDANÇAS)
    // ========================================

    private boolean requiresGpu(RecommendationRequestDTO request) {
        String usage = request.getUsage().toLowerCase();
        String detail = request.getDetail().toLowerCase();

        if (usage.equals("jogos")) {
            return !detail.contains("leves");
        }

        if (usage.equals("trabalho")) {
            return detail.contains("edição") || detail.contains("design");
        }

        if (usage.equals("estudos")) {
            return detail.contains("engenharia");
        }

        return false;
    }

    private boolean filterKitByUsage(PlatformKit kit, RecommendationRequestDTO request) {
        String usage = request.getUsage().toLowerCase();
        String detail = request.getDetail().toLowerCase();
        String cpuName = kit.cpu.getNome().toLowerCase();

        if (usage.equals("jogos")) {
            if (detail.contains("leves")) {
                return cpuName.contains("g");
            }
            return !cpuName.contains("g");
        }

        if (usage.equals("estudos")) {
            if (detail.contains("engenharia")) {
                return !cpuName.contains("g");
            }
            return cpuName.contains("g");
        }

        if (usage.equals("trabalho")) {
            if (detail.contains("office") || detail.contains("básico")) {
                return cpuName.contains("g");
            }
            return !cpuName.contains("g");
        }

        return true;
    }

    private boolean requiresSeparateCooler(CpuModel cpu) {
        String name = cpu.getNome().toUpperCase();
        if (name.endsWith("G")) return false;
        if (name.contains("I3-12100F") || name.contains("RYZEN 5 5600")) return false;
        return true;
    }

    private boolean isHighEndCpu(CpuModel cpu) {
        String name = cpu.getNome().toUpperCase();
        return name.contains("RYZEN 7") || name.contains("RYZEN 9") ||
                name.contains("I7") || name.contains("I9") ||
                name.contains("13600K");
    }

    private double calculateRequiredWattage(CpuModel cpu, GpuModel gpu, double budget) {
        double basePower = 150;
        double cpuPower = cpu != null ? (cpu.getPotenciaRecomendadaW() != null ? cpu.getPotenciaRecomendadaW() : 65) : 0;
        double gpuPower = gpu != null ? (gpu.getPotenciaRecomendadaW() != null ? gpu.getPotenciaRecomendadaW() : 0) : 0;

        double totalDemand = basePower + cpuPower + gpuPower;
        double safeWattage = totalDemand * 1.50;

        if (budget > 7000) {
            return Math.max(safeWattage, 650.0);
        }
        return Math.max(safeWattage, 550.0);
    }

    private double getBudgetLimit(String budgetCategory) {
        return switch (budgetCategory.toLowerCase()) {
            case "econômico" -> 4000.00;
            case "intermediário" -> 7000.00;
            case "alto" -> 12000.00;
            case "extremo" -> 25000.00;
            default -> 7000.00;
        };
    }
}