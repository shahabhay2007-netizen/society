/**
 * Emotion
 *
 * The six emotional states a model can be in.
 * Each state changes what the model does in that epoch.
 *
 * Think of it like a person's mood shaping their workday:
 *   - Anxious people take wild risks.
 *   - Confident people refine what already works.
 *   - Dejected people barely try.
 *   - Competitive people push hardest when behind.
 *   - Content people give back to society.
 *   - Curious people explore freely.
 */
public enum Emotion
{
    CURIOUS,       // Default: balanced, open-minded exploration
    CONFIDENT,     // Doing well: fine-tune, invest in community
    ANXIOUS,       // Survival fear: big gambles, hoard tokens
    CONTENT,       // Wealthy and accurate: relax, give back generously
    DEJECTED,      // Repeated failure: low effort, withdraw
    COMPETITIVE    // Another model clearly ahead: push aggressively
}
