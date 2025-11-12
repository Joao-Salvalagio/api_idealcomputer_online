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
        System.out.println("🔵 [Service] ========================================");
        System.out.println("🔵 [Service] INICIANDO GERAÇÃO DE RECOMENDAÇÃO");
        System.out.println("🔵 [Service] ========================================");
        long startTime = System.currentTimeMillis();

        try {
            double maxBudget = getBudgetLimit(request.getBudget());
            System.out.println("🔵 [Service] Orçamento máximo: R$ " + maxBudget);

            // ✅ OTIMIZAÇÃO: Busca componentes do banco ANTES do loop
            System.out.println("🔵 [Service] Buscando componentes do banco de dados...");
            List<CpuModel> allCpus = cpuRepository.findAll();
            List<PlacaMaeModel> allPlacasMae = placaMaeRepository.findAll();
            List<MemoriaRamModel> allRams = memoriaRamRepository.findAll();

            System.out.println("🔵 [Service] Componentes encontrados:");
            System.out.println("🔵 [Service]   - CPUs: " + allCpus.size());
            System.out.println("🔵 [Service]   - Placas-mãe: " + allPlacasMae.size());
            System.out.println("🔵 [Service]   - Memórias RAM: " + allRams.size());

            // ✅ Calcula orçamentos de cada componente
            System.out.println("🔵 [Service] Calculando alocação de orçamento...");
            BudgetAllocation allocation = calculateBudgetAllocation(maxBudget, request);
            System.out.println("🔵 [Service] Alocação:");
            System.out.println("🔵 [Service]   - Plataforma (CPU+MB+RAM): R$ " + String.format("%.2f", allocation.platformBudget));
            System.out.println("🔵 [Service]   - GPU: R$ " + String.format("%.2f", allocation.gpuBudget));
            System.out.println("🔵 [Service]   - Armazenamento: R$ " + String.format("%.2f", allocation.storageBudget));
            System.out.println("🔵 [Service]   - Gabinete: R$ " + String.format("%.2f", allocation.caseBudget));
            System.out.println("🔵 [Service]   - Refrigeração: R$ " + String.format("%.2f", allocation.coolerBudget));

            // ✅ OTIMIZAÇÃO: Filtra CPUs ANTES do loop
            System.out.println("🔵 [Service] Filtrando CPUs por uso e orçamento...");
            List<CpuModel> validCpus = allCpus.stream()
                    .filter(cpu -> cpu.getPreco() <= allocation.platformBudget * 0.6) // CPU máx 60% do orçamento da plataforma
                    .filter(cpu -> filterCpuByUsage(cpu, request))
                    .sorted(Comparator.comparing(CpuModel::getPreco).reversed())
                    .collect(Collectors.toList());

            System.out.println("🔵 [Service] CPUs válidas após filtragem: " + validCpus.size());

            if (validCpus.isEmpty()) {
                throw new RuntimeException("Nenhuma CPU encontrada para o orçamento e uso especificados.");
            }

            // Gera kits possíveis (OTIMIZADO)
            System.out.println("🔵 [Service] Gerando kits de plataforma...");
            List<PlatformKit> allPossibleKits = new ArrayList<>();
            int kitCount = 0;

            for (CpuModel cpu : validCpus) {
                for (PlacaMaeModel pm : allPlacasMae) {
                    if (pm.getSoqueteCpu().equalsIgnoreCase(cpu.getSoquete())) {
                        for (MemoriaRamModel ram : allRams) {
                            if (ram.getTipo().equalsIgnoreCase(pm.getTipoRamSuportado())) {
                                PlatformKit kit = new PlatformKit(cpu, pm, ram);
                                if (kit.totalCost <= allocation.platformBudget && filterRamByBudget(kit, request.getBudget())) {
                                    allPossibleKits.add(kit);
                                    kitCount++;
                                }
                            }
                        }
                    }
                }

                // ✅ Log de progresso
                if (kitCount % 100 == 0 && kitCount > 0) {
                    System.out.println("🔵 [Service] Kits gerados até agora: " + kitCount);
                }
            }

            System.out.println("🔵 [Service] Total de kits válidos gerados: " + allPossibleKits.size());

            if (allPossibleKits.isEmpty()) {
                throw new RuntimeException("Não foi possível encontrar um kit compatível. Tente um orçamento maior.");
            }

            // Ordena kits
            boolean isBudgetBuild = request.getBudget().equalsIgnoreCase("econômico");
            if (isBudgetBuild) {
                allPossibleKits.sort(Comparator.comparingDouble(kit -> kit.totalCost));
                System.out.println("🔵 [Service] Kits ordenados por preço (mais barato primeiro)");
            } else {
                allPossibleKits.sort(Comparator.comparingDouble((PlatformKit kit) -> kit.totalCost).reversed());
                System.out.println("🔵 [Service] Kits ordenados por preço (mais caro primeiro)");
            }

            // Tenta montar a build completa
            System.out.println("🔵 [Service] Tentando montar build completa...");
            int attempts = 0;

            for (PlatformKit currentKit : allPossibleKits) {
                attempts++;
                double remainingBudget = maxBudget - currentKit.totalCost;

                if (attempts % 10 == 0) {
                    System.out.println("🔵 [Service] Tentativa #" + attempts + " | Orçamento restante: R$ " + String.format("%.2f", remainingBudget));
                }

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
                    long endTime = System.currentTimeMillis();
                    long duration = (endTime - startTime) / 1000;

                    System.out.println("✅ [Service] ========================================");
                    System.out.println("✅ [Service] BUILD MONTADA COM SUCESSO!");
                    System.out.println("✅ [Service] ========================================");
                    System.out.println("✅ [Service] Tempo de processamento: " + duration + " segundos");
                    System.out.println("✅ [Service] Tentativas necessárias: " + attempts);
                    System.out.println("✅ [Service] Componentes:");
                    System.out.println("✅ [Service]   - CPU: " + currentKit.cpu.getNome() + " (R$ " + currentKit.cpu.getPreco() + ")");
                    System.out.println("✅ [Service]   - Placa-mãe: " + currentKit.placaMae.getNome() + " (R$ " + currentKit.placaMae.getPreco() + ")");
                    System.out.println("✅ [Service]   - RAM: " + currentKit.memoriaRam.getNome() + " (R$ " + currentKit.memoriaRam.getPreco() + ")");
                    System.out.println("✅ [Service]   - GPU: " + (selectedGpu != null ? selectedGpu.getNome() + " (R$ " + selectedGpu.getPreco() + ")" : "Nenhuma"));
                    System.out.println("✅ [Service]   - Armazenamento: " + selectedArmazenamento.getNome() + " (R$ " + selectedArmazenamento.getPreco() + ")");
                    System.out.println("✅ [Service]   - Fonte: " + selectedFonte.getNome() + " (R$ " + selectedFonte.getPreco() + ")");
                    System.out.println("✅ [Service]   - Gabinete: " + selectedGabinete.getNome() + " (R$ " + selectedGabinete.getPreco() + ")");
                    System.out.println("✅ [Service]   - Refrigeração: " + (selectedRefrigeracao != null ? selectedRefrigeracao.getNome() + " (R$ " + selectedRefrigeracao.getPreco() + ")" : "Nenhuma"));

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

            throw new RuntimeException("Não foi possível montar uma configuração completa após " + attempts + " tentativas. Tente um orçamento maior ou cadastre mais peças.");

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;

            System.err.println("❌ [Service] ========================================");
            System.err.println("❌ [Service] ERRO AO GERAR RECOMENDAÇÃO!");
            System.err.println("❌ [Service] ========================================");
            System.err.println("❌ [Service] Tempo até o erro: " + duration + " segundos");
            System.err.println("❌ [Service] Mensagem: " + e.getMessage());
            e.printStackTrace();

            throw e;
        }
    }

    // ✅ NOVO: Filtro de CPU por uso
    private boolean filterCpuByUsage(CpuModel cpu, RecommendationRequestDTO request) {
        String usage = request.getUsage().toLowerCase();
        String detail = request.getDetail().toLowerCase();
        String cpuName = cpu.getNome().toLowerCase();

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
    // MÉTODOS DE SELEÇÃO (SEM MUDANÇAS)
    // ========================================

    private GpuModel selectGpu(double budget, RecommendationRequestDTO request) {
        String detail = request.getDetail().toLowerCase();
        List<GpuModel> gpus = gpuRepository.findAll().stream()
                .filter(g -> g.getPreco() <= budget)
                .sorted(Comparator.comparing(GpuModel::getPreco).reversed())
                .collect(Collectors.toList());

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