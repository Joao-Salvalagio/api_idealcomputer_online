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
        System.out.println("🔵 ========================================");
        System.out.println("🔵 INICIANDO GERAÇÃO DE RECOMENDAÇÃO");
        System.out.println("🔵 ========================================");
        System.out.println("🔵 Usage: " + request.getUsage());
        System.out.println("🔵 Detail: " + request.getDetail());
        System.out.println("🔵 Budget: " + request.getBudget());
        System.out.println("🔵 ========================================");

        double maxBudget = getBudgetLimit(request.getBudget());
        System.out.println("🔵 Orçamento máximo: R$ " + maxBudget);

        // Gera todos os kits possíveis
        System.out.println("🔵 Gerando kits de plataforma...");
        List<PlatformKit> allPossibleKits = new ArrayList<>();
        List<CpuModel> allCpus = cpuRepository.findAll();
        List<PlacaMaeModel> allPlacasMae = placaMaeRepository.findAll();
        List<MemoriaRamModel> allRams = memoriaRamRepository.findAll();

        System.out.println("🔵 Componentes no banco:");
        System.out.println("🔵   - CPUs: " + allCpus.size());
        System.out.println("🔵   - Placas-mãe: " + allPlacasMae.size());
        System.out.println("🔵   - RAMs: " + allRams.size());

        for (CpuModel cpu : allCpus) {
            for (PlacaMaeModel pm : allPlacasMae) {
                if (pm.getSoqueteCpu().equalsIgnoreCase(cpu.getSoquete())) {
                    for (MemoriaRamModel ram : allRams) {
                        if (ram.getTipo().equalsIgnoreCase(pm.getTipoRamSuportado())) {
                            allPossibleKits.add(new PlatformKit(cpu, pm, ram));
                        }
                    }
                }
            }
        }

        System.out.println("🔵 Total de kits gerados: " + allPossibleKits.size());

        if (allPossibleKits.isEmpty()) {
            throw new RuntimeException("Nenhum kit compatível de CPU/Placa-mãe/RAM foi encontrado no banco. Verifique as compatibilidades.");
        }

        // ✅ Calcula orçamentos de cada componente
        BudgetAllocation allocation = calculateBudgetAllocation(maxBudget, request);
        System.out.println("🔵 Alocação de orçamento:");
        System.out.println("🔵   - Plataforma: R$ " + String.format("%.2f", allocation.platformBudget));
        System.out.println("🔵   - GPU: R$ " + String.format("%.2f", allocation.gpuBudget));
        System.out.println("🔵   - Armazenamento: R$ " + String.format("%.2f", allocation.storageBudget));
        System.out.println("🔵   - Gabinete: R$ " + String.format("%.2f", allocation.caseBudget));
        System.out.println("🔵   - Refrigeração: R$ " + String.format("%.2f", allocation.coolerBudget));

        // ✅ FALLBACK: Tenta com filtros estritos, depois relaxa gradualmente
        RecommendationResponseDTO response = tryBuildWithFallback(allPossibleKits, request, maxBudget, allocation);

        if (response != null) {
            return response;
        }

        throw new RuntimeException("Não foi possível montar uma configuração completa. Tente aumentar o orçamento ou adicionar mais peças ao banco.");
    }

    // ========================================
    // ✅ FALLBACK INTELIGENTE
    // ========================================

    private RecommendationResponseDTO tryBuildWithFallback(
            List<PlatformKit> allKits,
            RecommendationRequestDTO request,
            double maxBudget,
            BudgetAllocation allocation) {

        System.out.println("🔵 ----------------------------------------");
        System.out.println("🔵 TENTATIVA 1: Filtros estritos");

        // ✅ Tentativa 1: Filtros estritos
        List<PlatformKit> validKits = filterKits(allKits, request, allocation, true, true);
        System.out.println("🔵 Kits válidos: " + validKits.size());

        RecommendationResponseDTO response = tryBuildFromKits(validKits, request, maxBudget, allocation);
        if (response != null) return response;

        // ✅ Tentativa 2: Sem filtro de usage (aceita qualquer CPU)
        System.out.println("🔵 ----------------------------------------");
        System.out.println("🔵 TENTATIVA 2: Relaxando filtro de CPU");
        validKits = filterKits(allKits, request, allocation, false, true);
        System.out.println("🔵 Kits válidos: " + validKits.size());

        response = tryBuildFromKits(validKits, request, maxBudget, allocation);
        if (response != null) return response;

        // ✅ Tentativa 3: Sem filtro de RAM (aceita qualquer capacidade)
        System.out.println("🔵 ----------------------------------------");
        System.out.println("🔵 TENTATIVA 3: Relaxando filtro de RAM");
        validKits = filterKits(allKits, request, allocation, false, false);
        System.out.println("🔵 Kits válidos: " + validKits.size());

        response = tryBuildFromKits(validKits, request, maxBudget, allocation);
        if (response != null) return response;

        // ✅ Tentativa 4: Aumentando orçamento de cada componente em 20%
        System.out.println("🔵 ----------------------------------------");
        System.out.println("🔵 TENTATIVA 4: Aumentando orçamentos em 20%");
        BudgetAllocation relaxedAllocation = new BudgetAllocation();
        relaxedAllocation.platformBudget = allocation.platformBudget * 1.2;
        relaxedAllocation.gpuBudget = allocation.gpuBudget * 1.2;
        relaxedAllocation.storageBudget = allocation.storageBudget * 1.2;
        relaxedAllocation.caseBudget = allocation.caseBudget * 1.2;
        relaxedAllocation.coolerBudget = allocation.coolerBudget * 1.2;

        validKits = filterKits(allKits, request, relaxedAllocation, false, false);
        System.out.println("🔵 Kits válidos: " + validKits.size());

        response = tryBuildFromKits(validKits, request, maxBudget * 1.2, relaxedAllocation);
        if (response != null) return response;

        return null;
    }

    private List<PlatformKit> filterKits(
            List<PlatformKit> allKits,
            RecommendationRequestDTO request,
            BudgetAllocation allocation,
            boolean applyUsageFilter,
            boolean applyRamFilter) {

        return allKits.stream()
                .filter(kit -> kit.totalCost <= allocation.platformBudget)
                .filter(kit -> !applyUsageFilter || filterKitByUsage(kit, request))
                .filter(kit -> !applyRamFilter || filterRamByBudget(kit, request.getBudget()))
                .sorted(Comparator.comparingDouble((PlatformKit kit) -> kit.totalCost).reversed())
                .collect(Collectors.toList());
    }

    private RecommendationResponseDTO tryBuildFromKits(
            List<PlatformKit> validKits,
            RecommendationRequestDTO request,
            double maxBudget,
            BudgetAllocation allocation) {

        if (validKits.isEmpty()) return null;

        boolean isBudgetBuild = request.getBudget().equalsIgnoreCase("econômico");
        if (isBudgetBuild) {
            validKits.sort(Comparator.comparingDouble(kit -> kit.totalCost));
        }

        // Tenta montar a build completa
        int attempts = 0;
        for (PlatformKit currentKit : validKits) {
            attempts++;
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
            if (selectedArmazenamento == null) continue;
            remainingBudget -= selectedArmazenamento.getPreco();

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
            if (remainingBudget >= -500) { // ✅ Tolera até R$ 500 de estouro
                double totalPrice = maxBudget - remainingBudget;
                System.out.println("✅ ========================================");
                System.out.println("✅ BUILD ENCONTRADA!");
                System.out.println("✅ ========================================");
                System.out.println("✅ Tentativas necessárias: " + attempts);
                System.out.println("✅ Preço total: R$ " + String.format("%.2f", totalPrice));
                System.out.println("✅ Orçamento usado: " + String.format("%.2f%%", (totalPrice / maxBudget) * 100));
                System.out.println("✅ Componentes:");
                System.out.println("✅   - CPU: " + currentKit.cpu.getNome());
                System.out.println("✅   - Placa-mãe: " + currentKit.placaMae.getNome());
                System.out.println("✅   - RAM: " + currentKit.memoriaRam.getNome());
                System.out.println("✅   - GPU: " + (selectedGpu != null ? selectedGpu.getNome() : "Nenhuma"));
                System.out.println("✅   - Armazenamento: " + selectedArmazenamento.getNome());
                System.out.println("✅   - Fonte: " + selectedFonte.getNome());
                System.out.println("✅   - Gabinete: " + selectedGabinete.getNome());
                System.out.println("✅   - Refrigeração: " + (selectedRefrigeracao != null ? selectedRefrigeracao.getNome() : "Nenhuma"));

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

        return null;
    }

    // ========================================
    // ✅ ALOCAÇÃO DE ORÇAMENTO INTELIGENTE
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

        if (usage.equals("jogos")) {
            if (detail.contains("pesados") || detail.contains("todo tipo")) {
                allocation.platformBudget = maxBudget * 0.35;
                allocation.gpuBudget = maxBudget * 0.40;
                allocation.storageBudget = maxBudget * 0.08;
                allocation.caseBudget = maxBudget * 0.08;
                allocation.coolerBudget = maxBudget * 0.09;
            } else {
                allocation.platformBudget = maxBudget * 0.40;
                allocation.gpuBudget = maxBudget * 0.30;
                allocation.storageBudget = maxBudget * 0.10;
                allocation.caseBudget = maxBudget * 0.10;
                allocation.coolerBudget = maxBudget * 0.10;
            }
        } else if (usage.equals("trabalho")) {
            allocation.platformBudget = maxBudget * 0.45;
            allocation.gpuBudget = maxBudget * 0.20;
            allocation.storageBudget = maxBudget * 0.15;
            allocation.caseBudget = maxBudget * 0.10;
            allocation.coolerBudget = maxBudget * 0.10;
        } else {
            allocation.platformBudget = maxBudget * 0.60;
            allocation.gpuBudget = 0;
            allocation.storageBudget = maxBudget * 0.15;
            allocation.caseBudget = maxBudget * 0.15;
            allocation.coolerBudget = maxBudget * 0.10;
        }

        return allocation;
    }

    // ========================================
    // ✅ SELEÇÃO INTELIGENTE DE GPU (COM LOGS)
    // ========================================

    private GpuModel selectGpu(double budget, RecommendationRequestDTO request) {
        String detail = request.getDetail().toLowerCase();

        List<GpuModel> allGpus = gpuRepository.findAll();
        System.out.println("🔵 [GPU] Total no banco: " + allGpus.size());

        List<GpuModel> gpus = allGpus.stream()
                .filter(g -> g.getPreco() <= budget)
                .sorted(Comparator.comparing(GpuModel::getPreco).reversed())
                .collect(Collectors.toList());

        System.out.println("🔵 [GPU] Dentro do orçamento (R$ " + String.format("%.2f", budget) + "): " + gpus.size());

        if (gpus.isEmpty()) return null;

        if (budget > 5000 && (detail.contains("pesados") || detail.contains("todo tipo") || detail.contains("edição"))) {
            return gpus.stream()
                    .filter(g -> g.getMemoriaVram() >= 16)
                    .max(Comparator.comparing(GpuModel::getPreco))
                    .orElse(gpus.get(0));
        }

        return gpus.get(0);
    }

    private ArmazenamentoModel selectArmazenamento(double budget, double maxBudget) {
        List<ArmazenamentoModel> nvmes = armazenamentoRepository.findAll().stream()
                .filter(a -> a.getTipo().equalsIgnoreCase("SSD NVMe"))
                .filter(a -> a.getPreco() <= budget)
                .sorted(Comparator.comparing(ArmazenamentoModel::getCapacidadeGb).reversed()
                        .thenComparing(ArmazenamentoModel::getPreco))
                .collect(Collectors.toList());

        if (!nvmes.isEmpty()) {
            if (maxBudget >= 12000) {
                return nvmes.stream()
                        .filter(a -> a.getCapacidadeGb() >= 2000)
                        .findFirst()
                        .orElse(nvmes.get(0));
            }
            else if (maxBudget >= 7000) {
                return nvmes.stream()
                        .filter(a -> a.getCapacidadeGb() >= 1000)
                        .findFirst()
                        .orElse(nvmes.get(0));
            }
            return nvmes.stream()
                    .filter(a -> a.getCapacidadeGb() >= 500)
                    .min(Comparator.comparing(ArmazenamentoModel::getPreco))
                    .orElse(nvmes.get(0));
        }

        return armazenamentoRepository.findAll().stream()
                .filter(a -> a.getTipo().equalsIgnoreCase("SSD SATA"))
                .filter(a -> a.getPreco() <= budget)
                .min(Comparator.comparing(ArmazenamentoModel::getPreco))
                .orElse(null);
    }

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

        if (budget > 600) {
            int index = Math.min(compatibleCases.size() / 2, compatibleCases.size() - 1);
            return compatibleCases.get(index);
        }

        return compatibleCases.get(0);
    }

    private RefrigeracaoModel selectRefrigeracao(CpuModel cpu, double budget, double maxBudget) {
        String cpuSocket = cpu.getSoquete();
        boolean isHighEnd = isHighEndCpu(cpu);

        List<RefrigeracaoModel> coolers = refrigeracaoRepository.findAll().stream()
                .filter(c -> c.getSoquetesCpuSuportados().toUpperCase().contains(cpuSocket.toUpperCase()))
                .filter(c -> c.getPreco() <= budget)
                .collect(Collectors.toList());

        if (coolers.isEmpty()) return null;

        if (isHighEnd && maxBudget >= 10000) {
            RefrigeracaoModel waterCooler = coolers.stream()
                    .filter(c -> c.getTipo().equalsIgnoreCase("Water Cooler"))
                    .filter(c -> c.getNome().contains("360") || c.getNome().contains("280"))
                    .max(Comparator.comparing(RefrigeracaoModel::getPreco))
                    .orElse(null);

            if (waterCooler != null) return waterCooler;
        }

        if (isHighEnd) {
            RefrigeracaoModel waterCooler = coolers.stream()
                    .filter(c -> c.getTipo().equalsIgnoreCase("Water Cooler"))
                    .min(Comparator.comparing(RefrigeracaoModel::getPreco))
                    .orElse(null);

            if (waterCooler != null) return waterCooler;
        }

        return coolers.stream()
                .filter(c -> c.getTipo().equalsIgnoreCase("Air Cooler"))
                .min(Comparator.comparing(RefrigeracaoModel::getPreco))
                .orElse(coolers.get(0));
    }

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

    private boolean filterRamByBudget(PlatformKit kit, String budgetCategory) {
        int ramCapacity = kit.memoriaRam.getCapacidadeGb();

        return switch (budgetCategory.toLowerCase()) {
            case "econômico" -> ramCapacity <= 16;
            case "intermediário" -> ramCapacity <= 32;
            case "alto" -> ramCapacity <= 64;
            case "extremo" -> true;
            default -> ramCapacity <= 32;
        };
    }

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