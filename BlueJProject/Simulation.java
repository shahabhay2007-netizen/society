import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulation
 * 
 * The entry point. In BlueJ you right-click this class and call runSimulation().
 * 
 * This sets up the full Civilizational Model Ecosystem:
 *   - Two founding agents with different starting "beliefs" (prior parameters)
 *   - A shared Sandbox they collectively build over time
 *   - A hidden evaluator that only returns token rewards (no formula revealed)
 *   - 6 epochs of competition, improvement, mating, and civilizational construction
 * 
 * At the end, the isolated solo benchmark shows how much better the ecosystem
 * performs versus a single model learning entirely on its own.
 */
public class Simulation
{
    public static void runSimulation() throws Exception
    {
        System.out.println("=================================================================");
        System.out.println("   HIERARCHICAL SELF-IMPROVING MODEL ECOSYSTEM");
        System.out.println("   Hidden Target: Y = 2.50 + 1.80*X - 0.35*X^2");
        System.out.println("   Goal: Discover this curve using only token rewards.");
        System.out.println("=================================================================");

        HiddenEvaluator evaluator = new HiddenEvaluator();
        SharedSandbox   sandbox   = new SharedSandbox();

        // epochBounty=150 (fixed pot per epoch), livingCost=4 (upkeep per agent)
        Ecosystem world = new Ecosystem(evaluator, sandbox, 150, 4);

        // -------------------------------------------------------
        // Founding Agents -- different starting priors:
        //   Founder_A: believes intercept matters (1.0, 0.0, 0.0)
        //   Founder_B: believes slope matters    (0.0, 1.0, 0.0)
        // Neither knows the true curve.
        // -------------------------------------------------------
        Agent founderA = new Agent(
            "Founder_A", 0, null, null,
            1.0, 0.0, 0.0,
            35, evaluator, sandbox
        );
        Agent founderB = new Agent(
            "Founder_B", 0, null, null,
            0.0, 1.0, 0.0,
            35, evaluator, sandbox
        );

        world.registerAgent(founderA);
        world.registerAgent(founderB);

        // Thread pool for parallel agent execution
        ExecutorService pool = Executors.newFixedThreadPool(8);

        // Run 6 civilizational epochs
        int totalEpochs = 6;
        for (int e = 1; e <= totalEpochs; e++)
        {
            world.runEpoch(e, pool);
            Thread.sleep(300);
        }

        pool.shutdown();

        // -------------------------------------------------------
        // Benchmark: an isolated solo model, no ecosystem,
        // no sandbox, no mating -- just random walk alone.
        // -------------------------------------------------------
        runIsolatedBenchmark(evaluator);

        System.out.println("\nSimulation complete.");
    }

    private static void runIsolatedBenchmark(HiddenEvaluator evaluator)
    {
        System.out.println("\n=================================================================");
        System.out.println("   BENCHMARK: Isolated Solo Model");
        System.out.println("   (No Ecosystem, No Sandbox, No Mating)");
        System.out.println("=================================================================");

        double a = 0.0, b = 0.0, c = 0.0;

        // The solo model performs 500 random-walk trials on its own
        for (int step = 0; step < 500; step++)
        {
            double da = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
            double db = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
            double dc = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;

            int curReward  = evaluator.evaluateTokens(a, b, c);
            int nextReward = evaluator.evaluateTokens(a + da, b + db, c + dc);

            if (nextReward >= curReward)
            {
                a += da;
                b += db;
                c += dc;
            }
        }

        System.out.printf("Solo Final Equation: Y = %.4f + %.4fX + %.4fX^2%n", a, b, c);
        System.out.printf("Solo Final MSE:      %.5f%n", evaluator.computeMSE(a, b, c));
        System.out.println("(Compare against ecosystem's best MSE above)");
    }

    // BlueJ also allows running via main()
    public static void main(String[] args) throws Exception
    {
        runSimulation();
    }
}
