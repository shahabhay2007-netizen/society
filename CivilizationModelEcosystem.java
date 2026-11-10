import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Civilizational Model Ecosystem with:
 *  1. Cross-Genome Recombination (Sexual Reproduction / Crossover)
 *  2. "Buildings & Sandboxes" (Niche Construction / Shared Infrastructure)
 *     - Agents invest tokens into building "The Academy / Data Lab"
 *     - The Sandbox retains historical discoveries, provides warm-starts & educational training
 *  3. Quantifiable Prediction Improvement (MSE reduction and Token yield over time)
 *  4. Direct Benchmark vs. an Isolated Solo Model
 */
public class CivilizationModelEcosystem {

    // --- Black-Box Evaluator (Challenging multi-feature non-linear surface) ---
    // Ground truth: y = 2.5 + 1.8*x - 0.35*x^2 (Models predict using a + b*x + c*x^2)
    static class HiddenEvaluator {
        private final double[] xData = {0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0};
        private final double[] yData = new double[xData.length];

        public HiddenEvaluator() {
            // Ground truth coefficients: a=2.5, b=1.8, c=-0.35
            for (int i = 0; i < xData.length; i++) {
                double x = xData[i];
                yData[i] = 2.5 + 1.8 * x - 0.35 * x * x;
            }
        }

        // Returns Mean Squared Error (MSE) - lower is better
        public double computeMSE(double a, double b, double c) {
            double totalError = 0.0;
            for (int i = 0; i < xData.length; i++) {
                double x = xData[i];
                double pred = a + b * x + c * x * x;
                double err = pred - yData[i];
                totalError += err * err;
            }
            return totalError / xData.length;
        }

        // Evaluator returns reward tokens (Black Box: model only sees tokens, not formula)
        public int evaluateTokens(double a, double b, double c) {
            double mse = computeMSE(a, b, c);
            if (mse < 0.02) return 50;
            if (mse < 0.10) return 35;
            if (mse < 0.30) return 20;
            if (mse < 0.80) return 10;
            if (mse < 2.00) return 3;
            return 0;
        }
    }

    // --- "The Sandbox / Academy" (Civilization Infrastructure & Niche Construction) ---
    // Built and upgraded by models contributing their earned tokens.
    static class SharedSandboxInfrastructure {
        private final AtomicInteger publicTreasury = new AtomicInteger(0);
        private volatile int infrastructureLevel = 1;

        // Shared Knowledge Archive: Best discovered parameter sets in history
        private final List<double[]> knowledgeArchive = new CopyOnWriteArrayList<>();

        public SharedSandboxInfrastructure() {
            // Initial rudimentary knowledge
            knowledgeArchive.add(new double[]{0.0, 0.0, 0.0});
        }

        // Models invest tokens to construct and upgrade the shared facility
        public synchronized void investTokens(Agent contributor, int amount) {
            if (contributor.deductTokens(amount)) {
                int total = publicTreasury.addAndGet(amount);
                // Every 80 tokens contributed levels up civilization's infrastructure
                if (total >= infrastructureLevel * 80) {
                    infrastructureLevel++;
                    System.out.printf("  [INFRASTRUCTURE UPGRADE] Models built Sandbox Level %d! (Treasury: %d tokens)%n",
                            infrastructureLevel, total);
                }
            }
        }

        // Deposit successful discovery into the shared library
        public synchronized void archiveBreakthrough(double a, double b, double c, double mse) {
            if (mse < 0.5) {
                knowledgeArchive.add(new double[]{a, b, c});
                if (knowledgeArchive.size() > 8) {
                    knowledgeArchive.remove(0); // keep recent elite knowledge
                }
            }
        }

        // "Education & Sandbox Validation": Children enter the sandbox before facing reality.
        // The sandbox runs historical simulation benchmarks to guard against negative mutations.
        public double[] educateChild(double[] inheritedGenome) {
            if (knowledgeArchive.isEmpty()) return inheritedGenome.clone();

            // Find the best historical archive parameters
            double[] elite = knowledgeArchive.get(knowledgeArchive.size() - 1);
            double[] educated = new double[3];

            // Higher sandbox level transfers more institutional knowledge (warm start)
            double institutionalWeight = Math.min(0.85, 0.35 * infrastructureLevel);
            for (int i = 0; i < 3; i++) {
                educated[i] = (1.0 - institutionalWeight) * inheritedGenome[i] + institutionalWeight * elite[i];
            }
            return educated;
        }

        public double getExplorationPrecision() {
            // Level 1 = coarse exploration (0.4), Level 3+ = fine-grained gradient-like precision (0.08)
            return Math.max(0.06, 0.40 / infrastructureLevel);
        }

        public int getLevel() {
            return infrastructureLevel;
        }
    }

    // --- Model Agent ---
    static class Agent implements Runnable {
        final String id;
        final int generation;
        final String parentAId;
        final String parentBId; // For cross-genome mating

        volatile double a, b, c;
        final AtomicInteger tokens;
        volatile boolean alive = true;

        final HiddenEvaluator evaluator;
        final SharedSandboxInfrastructure sandbox;
        final EcosystemWorld world;

        public Agent(String id, int generation, String parentAId, String parentBId,
                     double a, double b, double c, int initialTokens,
                     HiddenEvaluator evaluator, SharedSandboxInfrastructure sandbox, EcosystemWorld world) {
            this.id = id;
            this.generation = generation;
            this.parentAId = parentAId;
            this.parentBId = parentBId;
            this.a = a;
            this.b = b;
            this.c = c;
            this.tokens = new AtomicInteger(initialTokens);
            this.evaluator = evaluator;
            this.sandbox = sandbox;
            this.world = world;
        }

        public double getMSE() {
            return evaluator.computeMSE(a, b, c);
        }

        public void addTokens(int amount) {
            tokens.addAndGet(amount);
        }

        public boolean deductTokens(int amount) {
            while (true) {
                int cur = tokens.get();
                if (cur < amount) return false;
                if (tokens.compareAndSet(cur, cur - amount)) return true;
            }
        }

        // Self-improvement using the Sandbox's technological resolution
        public void improve() {
            if (!alive) return;
            double bestScore = evaluator.evaluateTokens(a, b, c);
            double step = sandbox.getExplorationPrecision();

            for (int i = 0; i < 70; i++) {
                double da = (ThreadLocalRandom.current().nextDouble() - 0.5) * step;
                double db = (ThreadLocalRandom.current().nextDouble() - 0.5) * step;
                double dc = (ThreadLocalRandom.current().nextDouble() - 0.5) * step;

                int candidateScore = evaluator.evaluateTokens(a + da, b + db, c + dc);
                if (candidateScore >= bestScore) {
                    this.a += da;
                    this.b += db;
                    this.c += dc;
                    bestScore = candidateScore;
                }
            }

            // Share breakthrough with the community sandbox
            sandbox.archiveBreakthrough(a, b, c, getMSE());
        }

        // Invest excess wealth into public infrastructure (The Sandbox)
        public void contributeToPublicGood() {
            if (tokens.get() > 40) {
                int donation = (int) (tokens.get() * 0.20);
                sandbox.investTokens(this, donation);
            }
        }

        @Override
        public void run() {
            if (!alive) return;
            improve();
            contributeToPublicGood();
        }
    }

    // --- Ecosystem World Manager ---
    static class EcosystemWorld {
        private final List<Agent> population = new CopyOnWriteArrayList<>();
        private final HiddenEvaluator evaluator;
        private final SharedSandboxInfrastructure sandbox;
        private final int epochBounty;
        private final int livingCost;
        private final AtomicInteger agentCounter = new AtomicInteger(1);

        public EcosystemWorld(HiddenEvaluator evaluator, SharedSandboxInfrastructure sandbox,
                              int epochBounty, int livingCost) {
            this.evaluator = evaluator;
            this.sandbox = sandbox;
            this.epochBounty = epochBounty;
            this.livingCost = livingCost;
        }

        public void registerAgent(Agent agent) {
            population.add(agent);
        }

        public List<Agent> getLiving() {
            List<Agent> living = new ArrayList<>();
            for (Agent a : population) {
                if (a.alive) living.add(a);
            }
            return living;
        }

        // Cross-Genome Mating & Reproduction:
        // Two high-fitness agents mate, crossing their parameter genes to spawn educated children.
        public void executeCrossGenomeReproduction() {
            List<Agent> living = getLiving();
            // Filter fertile candidates with wealth > 30 tokens
            List<Agent> wealthy = new ArrayList<>();
            for (Agent a : living) {
                if (a.tokens.get() >= 30) wealthy.add(a);
            }

            if (wealthy.size() < 2) return;

            // Sort by lowest MSE (best predictors)
            wealthy.sort(Comparator.comparingDouble(Agent::getMSE));

            // Mate top pairs
            for (int i = 0; i < wealthy.size() - 1; i += 2) {
                Agent parentA = wealthy.get(i);
                Agent parentB = wealthy.get(i + 1);

                if (parentA.deductTokens(15) && parentB.deductTokens(15)) {
                    // Genetic Crossover: Blend coefficients from both parents
                    double childA = (ThreadLocalRandom.current().nextBoolean()) ? parentA.a : parentB.a;
                    double childB = (ThreadLocalRandom.current().nextBoolean()) ? parentA.b : parentB.b;
                    double childC = (ThreadLocalRandom.current().nextBoolean()) ? parentA.c : parentB.c;

                    // Add small exploratory mutation
                    childA += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.15;
                    childB += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.15;
                    childC += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.15;

                    // Educate child inside the Sandbox (warm-start leveraging past civilization data)
                    double[] educatedGenome = sandbox.educateChild(new double[]{childA, childB, childC});

                    int nextGen = Math.max(parentA.generation, parentB.generation) + 1;
                    Agent child = new Agent(
                        "Agent_" + agentCounter.getAndIncrement(),
                        nextGen,
                        parentA.id,
                        parentB.id,
                        educatedGenome[0], educatedGenome[1], educatedGenome[2],
                        20, // Starting endowment funded by parents
                        evaluator, sandbox, this
                    );

                    registerAgent(child);
                    System.out.printf("  [CROSS-GENOME BIRTH] %s x %s -> %s (Gen %d) [Educated in Sandbox Lv.%d]%n",
                            parentA.id, parentB.id, child.id, nextGen, sandbox.getLevel());
                }
            }
        }

        public void runEpoch(int epoch, ExecutorService pool) throws Exception {
            System.out.println("\n==========================================================================");
            System.out.printf("               EPOCH %d | Sandbox Infrastructure Level: %d%n",
                    epoch, sandbox.getLevel());
            System.out.println("==========================================================================");

            List<Agent> living = getLiving();

            // 1. Parallel Improvement and Sandbox Contribution
            List<Future<?>> futures = new ArrayList<>();
            for (Agent a : living) {
                futures.add(pool.submit(a));
            }
            for (Future<?> f : futures) {
                f.get();
            }

            // 2. Cross-Genome Reproduction (Crossover + Sandbox Education)
            executeCrossGenomeReproduction();
            living = getLiving();

            // 3. Evaluation & Resource Allocation
            double totalInverseMSE = 0.0;
            for (Agent a : living) {
                totalInverseMSE += 1.0 / (0.01 + a.getMSE());
            }

            System.out.printf("%-10s | %-4s | %-19s | %-26s | %-8s | %-6s%n",
                    "Agent", "Gen", "Parents", "Model Equation (a+bx+cx^2)", "MSE", "Tokens");
            System.out.println("--------------------------------------------------------------------------------");

            double bestMSEInEpoch = Double.MAX_VALUE;
            Agent bestAgent = null;

            for (Agent a : living) {
                // Share bounty proportional to prediction quality
                double weight = (1.0 / (0.01 + a.getMSE())) / totalInverseMSE;
                int reward = (int) Math.round(weight * epochBounty);
                a.addTokens(reward);

                // Living cost
                if (!a.deductTokens(livingCost)) {
                    a.alive = false;
                }

                double mse = a.getMSE();
                if (mse < bestMSEInEpoch) {
                    bestMSEInEpoch = mse;
                    bestAgent = a;
                }

                String status = a.alive ? String.format("%d", a.tokens.get()) : "DEAD";
                String parentStr = (a.parentAId == null) ? "Founding" : (a.parentAId + "x" + a.parentBId);
                System.out.printf("%-10s | Gen%-2d| %-19s | Y=%5.2f + %4.2fX + %5.2fX^2 | %8.4f | %-6s%n",
                        a.id, a.generation, parentStr, a.a, a.b, a.c, mse, status);
            }

            System.out.println("--------------------------------------------------------------------------------");
            if (bestAgent != null) {
                System.out.printf("  * Epoch Best: %s (Gen %d) with MSE = %.5f (Target: a=2.50, b=1.80, c=-0.35)%n",
                        bestAgent.id, bestAgent.generation, bestAgent.getMSE());
            }
        }
    }

    // --- Isolated Solo Baseline (No ecosystem, no crossover, no shared sandbox) ---
    static void runIsolatedSoloBenchmark(HiddenEvaluator evaluator) {
        System.out.println("\n==========================================================================");
        System.out.println("     BENCHMARK: Isolated Solo Model (No Ecosystem, No Sandbox, No Mating)");
        System.out.println("==========================================================================");
        double a = 0.0, b = 0.0, c = 0.0;
        int tokens = 50;

        for (int step = 0; step < 500; step++) {
            double da = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
            double db = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
            double dc = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;

            int curTokens = evaluator.evaluateTokens(a, b, c);
            int newTokens = evaluator.evaluateTokens(a + da, b + db, c + dc);

            if (newTokens >= curTokens) {
                a += da;
                b += db;
                c += dc;
            }
        }
        System.out.printf("Isolated Model Final Equation: Y = %.2f + %.2fX + %.2fX^2%n", a, b, c);
        System.out.printf("Isolated Model Final MSE: %.5f%n", evaluator.computeMSE(a, b, c));
    }

    public static void main(String[] args) throws Exception {
        HiddenEvaluator evaluator = new HiddenEvaluator();
        SharedSandboxInfrastructure sandbox = new SharedSandboxInfrastructure();
        EcosystemWorld world = new EcosystemWorld(evaluator, sandbox, 150, 4);

        // Found two disparate lineages:
        // Founder Alpha: starts with intercept bias (1.0, 0.0, 0.0)
        // Founder Beta: starts with slope bias (0.0, 1.0, 0.0)
        Agent founderAlpha = new Agent("Founder_A", 0, null, null, 1.0, 0.0, 0.0, 35, evaluator, sandbox, world);
        Agent founderBeta = new Agent("Founder_B", 0, null, null, 0.0, 1.0, 0.0, 35, evaluator, sandbox, world);

        world.registerAgent(founderAlpha);
        world.registerAgent(founderBeta);

        ExecutorService pool = Executors.newFixedThreadPool(8);

        System.out.println(">>> STARTING CIVILIZATIONAL MODEL ECOSYSTEM <<<");
        System.out.println("Target Ground Truth: Y = 2.50 + 1.80X - 0.35X^2");

        int epochs = 6;
        for (int e = 1; e <= epochs; e++) {
            world.runEpoch(e, pool);
            Thread.sleep(500);
        }

        pool.shutdown();

        // Compare against isolated solo learner
        runIsolatedSoloBenchmark(evaluator);
    }
}
