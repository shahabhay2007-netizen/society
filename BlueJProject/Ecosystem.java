import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ecosystem
 *
 * The world manager. Each epoch it:
 *   1. Finds the current best MSE (social benchmark for emotion comparison)
 *   2. Updates every agent's emotion based on its situation
 *   3. Runs all living agents in parallel (improvement + sandbox contribution)
 *   4. Runs cross-genome reproduction (filtered by each agent's emotional readiness)
 *   5. Distributes the fixed epoch bounty proportional to prediction quality
 *   6. Charges metabolic living cost (starvation / natural selection)
 *   7. Prints full status table including each agent's emotion
 */
public class Ecosystem
{
    private List<Agent>     population   = new CopyOnWriteArrayList<>();
    private HiddenEvaluator evaluator;
    private SharedSandbox   sandbox;
    private int             epochBounty;
    private int             livingCost;
    private AtomicInteger   agentCounter = new AtomicInteger(1);

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------
    public Ecosystem(HiddenEvaluator evaluator, SharedSandbox sandbox,
                     int epochBounty, int livingCost)
    {
        this.evaluator   = evaluator;
        this.sandbox     = sandbox;
        this.epochBounty = epochBounty;
        this.livingCost  = livingCost;
    }

    public void registerAgent(Agent agent) { population.add(agent); }

    public List<Agent> getLiving()
    {
        List<Agent> living = new ArrayList<>();
        for (Agent a : population)
            if (a.alive) living.add(a);
        return living;
    }

    // -------------------------------------------------------
    // Cross-Genome Reproduction
    //
    // Eligibility is emotional: each agent's own reproductionTokenThreshold()
    // decides whether it wants to invest in a child right now.
    //   - Content agents reproduce eagerly (threshold = 20 tokens)
    //   - Anxious agents won't reproduce until survival is secured (threshold = 60)
    //   - Dejected agents barely try (threshold = 50)
    // -------------------------------------------------------
    public void crossGenomeReproduction()
    {
        List<Agent> living = getLiving();

        // Collect emotionally-ready candidates
        List<Agent> fertile = new ArrayList<>();
        for (Agent a : living)
        {
            if (a.getTokens() >= a.reproductionTokenThreshold())
                fertile.add(a);
        }
        if (fertile.size() < 2) return;

        // Sort best predictors first (lowest MSE)
        fertile.sort((x, y) -> Double.compare(x.getMSE(), y.getMSE()));

        // Mate top pairs
        for (int i = 0; i + 1 < fertile.size(); i += 2)
        {
            Agent parentA = fertile.get(i);
            Agent parentB = fertile.get(i + 1);
            int   costA   = 15;
            int   costB   = 15;

            if (parentA.deductTokens(costA) && parentB.deductTokens(costB))
            {
                // Genetic crossover: each gene inherited randomly from one parent
                double childA = ThreadLocalRandom.current().nextBoolean() ? parentA.a : parentB.a;
                double childB = ThreadLocalRandom.current().nextBoolean() ? parentA.b : parentB.b;
                double childC = ThreadLocalRandom.current().nextBoolean() ? parentA.c : parentB.c;

                // Small mutation
                childA += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.15;
                childB += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.15;
                childC += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.15;

                // Sandbox education: blend with historical institutional knowledge
                double[] educated = sandbox.educateChild(new double[]{childA, childB, childC});

                int nextGen = Math.max(parentA.generation, parentB.generation) + 1;
                Agent child = new Agent(
                    "Agent_" + agentCounter.getAndIncrement(),
                    nextGen,
                    parentA.id, parentB.id,
                    educated[0], educated[1], educated[2],
                    20,
                    evaluator, sandbox
                );

                registerAgent(child);

                System.out.println("  [BIRTH] " + parentA.id
                    + " (" + parentA.emotionLabel().trim() + ")"
                    + " x " + parentB.id
                    + " (" + parentB.emotionLabel().trim() + ")"
                    + " -> " + child.id
                    + " (Gen " + nextGen + ") [Sandbox Lv." + sandbox.getLevel() + "]");
            }
        }
    }

    // -------------------------------------------------------
    // Run one full epoch
    // -------------------------------------------------------
    public void runEpoch(int epochNum, ExecutorService pool) throws Exception
    {
        System.out.println("\n================================================================");
        System.out.println("  EPOCH " + epochNum
            + " | Sandbox Level: " + sandbox.getLevel()
            + " | Active Models: " + getLiving().size());
        System.out.println("================================================================");

        List<Agent> living = getLiving();
        if (living.isEmpty()) { System.out.println("  All models extinct."); return; }

        // 1. Find the social benchmark: best MSE in the population right now
        double populationBestMSE = Double.MAX_VALUE;
        for (Agent a : living)
        {
            double mse = a.getMSE();
            if (mse < populationBestMSE) populationBestMSE = mse;
        }

        // 2. Update every agent's emotion before it acts
        for (Agent a : living)
            a.updateEmotion(populationBestMSE);

        // 3. Parallel improvement + sandbox contribution (each agent acts by its emotion)
        List<Future<?>> futures = new ArrayList<>();
        for (Agent a : living) futures.add(pool.submit(a));
        for (Future<?> f : futures) f.get();

        // 4. Cross-genome reproduction (emotional readiness filters who mates)
        crossGenomeReproduction();
        living = getLiving();

        // 5. Compute bounty weights (inverse-MSE proportional)
        double totalWeight = 0.0;
        for (Agent a : living) totalWeight += 1.0 / (0.01 + a.getMSE());

        // 6. Print status table
        System.out.printf("%-11s | %-4s | %-13s | %-26s | %-9s | %-6s%n",
            "Agent", "Gen", "Emotion", "Equation (a + bX + cX^2)", "MSE", "Tokens");
        System.out.println("--------------------------------------------------------------------------------");

        double bestMSE   = Double.MAX_VALUE;
        Agent  bestAgent = null;

        for (Agent a : living)
        {
            // Distribute bounty
            double w      = (1.0 / (0.01 + a.getMSE())) / totalWeight;
            int    reward = (int) Math.round(w * epochBounty);
            a.addTokens(reward);

            // Charge living cost
            if (!a.deductTokens(livingCost)) a.alive = false;

            if (a.getMSE() < bestMSE) { bestMSE = a.getMSE(); bestAgent = a; }

            String status = a.alive ? String.valueOf(a.getTokens()) : "DEAD";
            System.out.printf("%-11s | Gen%-2d| %-13s | Y=%5.2f + %4.2fX + %5.2fX^2 | %9.4f | %-6s%n",
                a.id, a.generation, a.emotionLabel(), a.a, a.b, a.c, a.getMSE(), status);
        }

        System.out.println("--------------------------------------------------------------------------------");
        if (bestAgent != null)
        {
            System.out.printf("  EPOCH BEST: %s (Gen %d, feeling %s)  MSE = %.5f%n",
                bestAgent.id, bestAgent.generation,
                bestAgent.emotion.toString(), bestAgent.getMSE());
        }
    }
}
