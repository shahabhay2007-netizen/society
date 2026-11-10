import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Agent
 *
 * A single living model in the ecosystem. Each agent:
 *   - Has its own regression equation: Y = a + b*X + c*X^2
 *   - Has an Emotion that shapes its behaviour each epoch
 *   - Earns tokens by making better predictions (black-box reward only)
 *   - Improves itself differently depending on how it feels
 *   - Contributes to the shared Sandbox (more when Content, less when Anxious)
 *
 * HOW EMOTION WORKS
 * -----------------
 * Before each epoch the Ecosystem calls updateEmotion(), passing in:
 *   - The agent's current token count
 *   - Its last MSE (was it improving?)
 *   - The best MSE anyone else has right now (social comparison)
 *
 * The emotion then adjusts:
 *   - Number of improvement trials  (effort level)
 *   - Search step size              (risk tolerance)
 *   - Sandbox donation rate         (social generosity)
 *   - Reproduction eagerness        (willingness to invest in children)
 */
public class Agent implements Runnable
{
    // --- Identity ---
    public String id;
    public int    generation;
    public String parentAId;
    public String parentBId;

    // --- The model's "genome": regression coefficients ---
    public volatile double a;
    public volatile double b;
    public volatile double c;

    // --- Resources ---
    private AtomicInteger tokens;
    public  volatile boolean alive = true;

    // --- Emotional state ---
    public volatile Emotion emotion   = Emotion.CURIOUS;
    public volatile double  lastMSE   = Double.MAX_VALUE; // MSE from previous epoch
    public volatile int     failStreak = 0;               // consecutive epochs of no improvement

    // --- Dependencies ---
    private HiddenEvaluator evaluator;
    private SharedSandbox   sandbox;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------
    public Agent(String id, int generation,
                 String parentAId, String parentBId,
                 double a, double b, double c,
                 int initialTokens,
                 HiddenEvaluator evaluator,
                 SharedSandbox sandbox)
    {
        this.id         = id;
        this.generation = generation;
        this.parentAId  = parentAId;
        this.parentBId  = parentBId;
        this.a          = a;
        this.b          = b;
        this.c          = c;
        this.tokens     = new AtomicInteger(initialTokens);
        this.evaluator  = evaluator;
        this.sandbox    = sandbox;
        this.lastMSE    = evaluator.computeMSE(a, b, c);
    }

    // -------------------------------------------------------
    // Token management (thread-safe)
    // -------------------------------------------------------
    public void addTokens(int amount) { tokens.addAndGet(amount); }

    public boolean deductTokens(int amount)
    {
        while (true)
        {
            int cur = tokens.get();
            if (cur < amount) return false;
            if (tokens.compareAndSet(cur, cur - amount)) return true;
        }
    }

    public int getTokens() { return tokens.get(); }

    // -------------------------------------------------------
    // Prediction quality
    // -------------------------------------------------------
    public double getMSE()
    {
        return evaluator.computeMSE(a, b, c);
    }

    // -------------------------------------------------------
    // Emotion update (called by Ecosystem each epoch)
    //
    // The agent sees:
    //   - Its own token balance    -> survival pressure
    //   - Whether it improved      -> self-assessment
    //   - Best MSE in population   -> social comparison
    // -------------------------------------------------------
    public void updateEmotion(double populationBestMSE)
    {
        double currentMSE = getMSE();
        int    balance    = getTokens();
        boolean improved  = currentMSE < lastMSE * 0.95; // meaningful improvement threshold

        if (improved) failStreak = 0;
        else          failStreak++;

        // --- Priority rules (checked in order of urgency) ---

        // 1. Survival fear overrides everything
        if (balance < 15)
        {
            emotion = Emotion.ANXIOUS;
        }
        // 2. Doing great on both money and accuracy -> feel content
        else if (balance > 60 && currentMSE < 1.0)
        {
            emotion = Emotion.CONTENT;
        }
        // 3. Someone is significantly better -> feel competitive
        else if (currentMSE > populationBestMSE * 3.0 && balance >= 15)
        {
            emotion = Emotion.COMPETITIVE;
        }
        // 4. Repeated failure -> feel dejected
        else if (failStreak >= 3)
        {
            emotion = Emotion.DEJECTED;
        }
        // 5. Actively improving and stable -> feel confident
        else if (improved && balance > 25)
        {
            emotion = Emotion.CONFIDENT;
        }
        // 6. Default: curious
        else
        {
            emotion = Emotion.CURIOUS;
        }

        lastMSE = currentMSE;
    }

    // -------------------------------------------------------
    // Self-improvement: behaviour shaped by emotion
    // -------------------------------------------------------
    public void improve()
    {
        if (!alive) return;

        // Base step size from sandbox technology level
        double baseStep = sandbox.getExplorationPrecision();

        // Emotion modifies effort and risk tolerance
        int    trials;
        double stepMultiplier;

        switch (emotion)
        {
            case ANXIOUS:
                // Desperate gambles: wide search, many attempts
                trials = 120;
                stepMultiplier = 2.5;
                break;
            case COMPETITIVE:
                // Aggressive push: frequent trials, moderately bold
                trials = 100;
                stepMultiplier = 1.8;
                break;
            case CURIOUS:
                // Balanced exploration (default)
                trials = 70;
                stepMultiplier = 1.0;
                break;
            case CONFIDENT:
                // Fine-tune what already works: precise, fewer trials
                trials = 55;
                stepMultiplier = 0.6;
                break;
            case CONTENT:
                // No rush: conservative, light touch
                trials = 35;
                stepMultiplier = 0.4;
                break;
            case DEJECTED:
                // Low energy: minimal effort
                trials = 30;
                stepMultiplier = 0.8;
                break;
            default:
                trials = 70;
                stepMultiplier = 1.0;
        }

        double step = baseStep * stepMultiplier;
        int bestReward = evaluator.evaluateTokens(a, b, c);

        for (int i = 0; i < trials; i++)
        {
            double da = (ThreadLocalRandom.current().nextDouble() - 0.5) * step;
            double db = (ThreadLocalRandom.current().nextDouble() - 0.5) * step;
            double dc = (ThreadLocalRandom.current().nextDouble() - 0.5) * step;

            int candidateReward = evaluator.evaluateTokens(a + da, b + db, c + dc);
            if (candidateReward >= bestReward)
            {
                this.a += da;
                this.b += db;
                this.c += dc;
                bestReward = candidateReward;
            }
        }

        sandbox.archiveBreakthrough(a, b, c, getMSE());
    }

    // -------------------------------------------------------
    // Civic contribution: emotion decides generosity
    // -------------------------------------------------------
    public void contributeToSandbox()
    {
        double donationRate;

        switch (emotion)
        {
            case CONTENT:      donationRate = 0.35; break; // most generous
            case CONFIDENT:    donationRate = 0.25; break;
            case CURIOUS:      donationRate = 0.18; break;
            case COMPETITIVE:  donationRate = 0.08; break; // saving for self
            case DEJECTED:     donationRate = 0.05; break; // withdrawn
            case ANXIOUS:      donationRate = 0.00; break; // hoarding
            default:           donationRate = 0.15;
        }

        int balance = getTokens();
        if (balance > 20 && donationRate > 0)
        {
            int donation = (int)(balance * donationRate);
            if (donation > 0) sandbox.investTokens(this, donation);
        }
    }

    // -------------------------------------------------------
    // Reproduction eagerness: emotion sets the threshold
    // (Used by Ecosystem when deciding who can mate)
    // -------------------------------------------------------
    public int reproductionTokenThreshold()
    {
        switch (emotion)
        {
            case CONTENT:     return 20; // very eager to reproduce
            case CONFIDENT:   return 28;
            case CURIOUS:     return 32;
            case COMPETITIVE: return 35; // focused on self-improvement
            case DEJECTED:    return 50; // too low energy to reproduce
            case ANXIOUS:     return 60; // survival first
            default:          return 30;
        }
    }

    // -------------------------------------------------------
    // Thread entry point
    // -------------------------------------------------------
    @Override
    public void run()
    {
        if (!alive) return;
        improve();
        contributeToSandbox();
    }

    // -------------------------------------------------------
    // Emoji label for display
    // -------------------------------------------------------
    public String emotionLabel()
    {
        switch (emotion)
        {
            case CURIOUS:     return "Curious    ";
            case CONFIDENT:   return "Confident  ";
            case ANXIOUS:     return "Anxious    ";
            case CONTENT:     return "Content    ";
            case DEJECTED:    return "Dejected   ";
            case COMPETITIVE: return "Competitive";
            default:          return "Unknown    ";
        }
    }
}
