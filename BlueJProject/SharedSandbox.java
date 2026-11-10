import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SharedSandbox
 * 
 * The civilizational infrastructure -- the "Academy" / "Library" / "Data Lab"
 * that all models collectively build by investing surplus tokens.
 * 
 * What it does:
 *   - Stores a Knowledge Archive of the best parameter sets ever discovered.
 *   - Educates newborn children: instead of random guesses, children get a
 *     warm-start blended from historical elite solutions (like a school curriculum).
 *   - Levels up as the community invests more tokens, granting finer-grained
 *     search precision to every agent (like improving scientific instruments).
 * 
 * This is "Niche Construction": agents actively improve their own environment
 * to give future generations better starting conditions.
 */
public class SharedSandbox
{
    private AtomicInteger publicTreasury = new AtomicInteger(0);
    private int infrastructureLevel = 1;

    // Shared archive of the best discovered parameter sets in history
    private List<double[]> knowledgeArchive = new CopyOnWriteArrayList<>();

    public SharedSandbox()
    {
        // The civilization starts from zero knowledge
        knowledgeArchive.add(new double[]{0.0, 0.0, 0.0});
    }

    /**
     * A model donates tokens to build public infrastructure.
     * Every 80 tokens triggers a Sandbox level-up.
     */
    public synchronized void investTokens(Agent contributor, int amount)
    {
        if (contributor.deductTokens(amount))
        {
            int total = publicTreasury.addAndGet(amount);
            if (total >= infrastructureLevel * 80)
            {
                infrastructureLevel++;
                System.out.println("  [SANDBOX UPGRADED] Community built Sandbox Level "
                    + infrastructureLevel + "! (Treasury: " + total + " tokens)");
            }
        }
    }

    /**
     * A model deposits a successful breakthrough into the shared library.
     * Only high-quality solutions (MSE < 0.5) are archived.
     */
    public synchronized void archiveBreakthrough(double a, double b, double c, double mse)
    {
        if (mse < 0.5)
        {
            knowledgeArchive.add(new double[]{a, b, c});
            // Keep the archive lean -- retain the most recent elite solutions
            if (knowledgeArchive.size() > 8)
            {
                knowledgeArchive.remove(0);
            }
        }
    }

    /**
     * Education: a newborn child enters the Sandbox before facing reality.
     * Their inherited genes are blended with the collective historical knowledge.
     * Higher Sandbox level = more institutional knowledge transferred.
     */
    public double[] educateChild(double[] inheritedGenome)
    {
        if (knowledgeArchive.isEmpty())
        {
            return inheritedGenome.clone();
        }

        // Always blend with the latest (best) archived solution
        double[] elite = knowledgeArchive.get(knowledgeArchive.size() - 1);
        double[] educated = new double[3];

        // Higher level transfers more institutional knowledge
        double institutionalWeight = Math.min(0.85, 0.35 * infrastructureLevel);
        for (int i = 0; i < 3; i++)
        {
            educated[i] = (1.0 - institutionalWeight) * inheritedGenome[i]
                        + institutionalWeight * elite[i];
        }
        return educated;
    }

    /**
     * As the Sandbox levels up, models get finer search resolution.
     * Level 1 = coarse random walk (0.40 step).
     * Level 4+ = precise gradient-like search (0.06 step).
     */
    public double getExplorationPrecision()
    {
        return Math.max(0.06, 0.40 / infrastructureLevel);
    }

    public int getLevel()
    {
        return infrastructureLevel;
    }

    public int getTreasury()
    {
        return publicTreasury.get();
    }
}
