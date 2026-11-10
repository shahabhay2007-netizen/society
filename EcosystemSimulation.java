import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hierarchical Self-Improving Model Ecosystem - Phase 2
 * Features:
 *  1. Shared Resource Scarcity (Fixed epoch token bounty divided by relative accuracy)
 *  2. Metabolic Living Cost (Starvation / Natural Selection)
 *  3. Continuous Concurrent Lifecycles across Epochs
 *  4. Evolutionary Mutation & Filial Support
 */
public class EcosystemSimulation {

    // --- Black-Box Evaluator (Hidden ground truth & scoring) ---
    static class HiddenEvaluator {
        // Ground truth function: y = 1.2 + 0.85 * x + noise
        private final double[] xValues = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        private final double[] yValues = {2.1, 2.9, 3.7, 4.6, 5.5, 6.3};

        public double evaluateLoss(double a, double b) {
            double sse = 0.0;
            for (int i = 0; i < xValues.length; i++) {
                double pred = a + b * xValues[i];
                double diff = yValues[i] - pred;
                sse += diff * diff;
            }
            return sse;
        }

        // Relative fitness: inverse loss so lower error gives higher fitness weight
        public double computeFitness(double a, double b) {
            double loss = evaluateLoss(a, b);
            return 1.0 / (1.0 + loss);
        }
    }

    // --- Model Agent ---
    static class Agent implements Runnable {
        final String id;
        final String lineage; // e.g. "Family-A" or "Family-B"
        final int generation;
        final Agent parent;
        final List<Agent> children = new CopyOnWriteArrayList<>();

        volatile double a;
        volatile double b;
        final AtomicInteger tokens;
        volatile boolean alive = true;

        final HiddenEvaluator evaluator;
        final EcosystemManager manager;

        public Agent(String id, String lineage, int generation, Agent parent,
                     double a, double b, int initialTokens,
                     HiddenEvaluator evaluator, EcosystemManager manager) {
            this.id = id;
            this.lineage = lineage;
            this.generation = generation;
            this.parent = parent;
            this.a = a;
            this.b = b;
            this.tokens = new AtomicInteger(initialTokens);
            this.evaluator = evaluator;
            this.manager = manager;
        }

        public double getFitness() {
            return evaluator.computeFitness(a, b);
        }

        public void addTokens(int amount) {
            tokens.addAndGet(amount);
        }

        public boolean deductTokens(int amount) {
            while (true) {
                int current = tokens.get();
                if (current < amount) return false;
                if (tokens.compareAndSet(current, current - amount)) return true;
            }
        }

        // Self-improvement step via stochastic hill-climbing
        public void improve() {
            if (!alive) return;
            double currentFit = getFitness();

            // Perform exploratory trials with decaying step-size
            for (int i = 0; i < 60; i++) {
                double deltaA = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3;
                double deltaB = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3;

                double candidateFit = evaluator.computeFitness(a + deltaA, b + deltaB);
                if (candidateFit > currentFit) {
                    this.a += deltaA;
                    this.b += deltaB;
                    currentFit = candidateFit;
                }
            }
        }

        // Attempt reproduction if resource-rich
        public void tryReproduce() {
            if (!alive || generation >= 2) return;

            // Spawning requires 25 tokens: 10 given to child, 15 consumed as energy cost
            if (deductTokens(25)) {
                String childId = id + "." + (children.size() + 1);
                // Child inherits mutated parameters (genetic exploration)
                double childA = a + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
                double childB = b + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;

                Agent child = new Agent(
                    childId, lineage, generation + 1, this,
                    childA, childB, 10, evaluator, manager
                );

                children.add(child);
                manager.registerAgent(child);
                System.out.printf("  [SPAWN] %s (Gen %d) gave birth to %s (Gen %d)%n",
                        id, generation, childId, generation + 1);
            }
        }

        // Filial support: send portion of wealth to parent
        public void supportParent() {
            if (!alive || parent == null || !parent.alive) return;
            int balance = tokens.get();
            if (balance > 15) {
                int tribute = (int) (balance * 0.15);
                if (tribute > 0 && deductTokens(tribute)) {
                    parent.addTokens(tribute);
                }
            }
        }

        @Override
        public void run() {
            if (!alive) return;
            improve();
            tryReproduce();
            supportParent();
        }
    }

    // --- Ecosystem Environment Manager ---
    static class EcosystemManager {
        private final List<Agent> population = new CopyOnWriteArrayList<>();
        private final HiddenEvaluator evaluator;
        private final int epochBounty;
        private final int livingCost;

        public EcosystemManager(HiddenEvaluator evaluator, int epochBounty, int livingCost) {
            this.evaluator = evaluator;
            this.epochBounty = epochBounty;
            this.livingCost = livingCost;
        }

        public void registerAgent(Agent agent) {
            population.add(agent);
        }

        public List<Agent> getLivingAgents() {
            List<Agent> living = new ArrayList<>();
            for (Agent a : population) {
                if (a.alive) living.add(a);
            }
            return living;
        }

        public void runEpoch(int epochNumber, ExecutorService pool) throws InterruptedException {
            System.out.println("\n=======================================================");
            System.out.printf("               EPOCH %d (Active Population: %d)%n",
                    epochNumber, getLivingAgents().size());
            System.out.println("=======================================================");

            List<Agent> active = getLivingAgents();
            if (active.isEmpty()) {
                System.out.println("All models have gone extinct!");
                return;
            }

            // 1. Parallel Model Execution: Improvement, Reproduction & Filial Support
            List<Future<?>> futures = new ArrayList<>();
            for (Agent agent : active) {
                futures.add(pool.submit(agent));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            // Refresh active list in case children were spawned
            active = getLivingAgents();

            // 2. Resource Scarcity Distribution (Evaluator awards fixed pot based on relative accuracy)
            double totalFitness = 0.0;
            for (Agent agent : active) {
                totalFitness += agent.getFitness();
            }

            System.out.printf("%-18s | %-8s | %-5s | %-16s | %-7s | %-6s%n",
                    "Agent ID", "Lineage", "Gen", "Equation (Y=a+bX)", "Fitness", "Tokens");
            System.out.println("--------------------------------------------------------------------------------");

            for (Agent agent : active) {
                // Share of bounty proportional to fitness
                int share = (int) Math.round((agent.getFitness() / totalFitness) * epochBounty);
                agent.addTokens(share);

                // 3. Metabolic Living Cost (Energy burn)
                boolean survived = agent.deductTokens(livingCost);
                if (!survived) {
                    agent.alive = false;
                }

                String status = agent.alive ? String.format("%d", agent.tokens.get()) : "DEAD (0)";
                System.out.printf("%-18s | %-8s | %-5d | Y=%5.2f + %4.2fX   | %7.4f | %-6s%n",
                        agent.id, agent.lineage, agent.generation, agent.a, agent.b, agent.getFitness(), status);
            }

            // Lineage summary
            summarizeLineages();
        }

        private void summarizeLineages() {
            Map<String, Integer> lineageTokens = new HashMap<>();
            Map<String, Integer> lineageCounts = new HashMap<>();

            for (Agent a : population) {
                if (a.alive) {
                    lineageTokens.put(a.lineage, lineageTokens.getOrDefault(a.lineage, 0) + a.tokens.get());
                    lineageCounts.put(a.lineage, lineageCounts.getOrDefault(a.lineage, 0) + 1);
                }
            }

            System.out.println("\n--- FAMILY STANDINGS ---");
            for (String lineage : lineageTokens.keySet()) {
                System.out.printf("  [%s] Living Members: %d | Combined Capital: %d tokens%n",
                        lineage, lineageCounts.get(lineage), lineageTokens.get(lineage));
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        HiddenEvaluator evaluator = new HiddenEvaluator();
        // 120 tokens total per epoch bounty, 5 tokens upkeep cost per round
        EcosystemManager manager = new EcosystemManager(evaluator, 120, 5);

        // Seed Founder Models
        // Model A starts with inaccurate prior (0.0, 0.0)
        Agent modelA = new Agent("Model_A", "Family_A", 0, null, 0.0, 0.0, 20, evaluator, manager);
        // Model B starts with rough prior (1.0, 0.5)
        Agent modelB = new Agent("Model_B", "Family_B", 0, null, 1.0, 0.5, 20, evaluator, manager);

        manager.registerAgent(modelA);
        manager.registerAgent(modelB);

        ExecutorService pool = Executors.newFixedThreadPool(8);

        // Run simulation for 6 evolutionary epochs
        int totalEpochs = 6;
        for (int epoch = 1; epoch <= totalEpochs; epoch++) {
            manager.runEpoch(epoch, pool);
            Thread.sleep(600); // Slight pause for visual inspection
        }

        pool.shutdown();
        System.out.println("\nSimulation complete.");
    }
}
