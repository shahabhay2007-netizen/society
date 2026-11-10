/**
 * HiddenEvaluator
 * 
 * The black-box grader. Models never see this code -- they only receive
 * token rewards as feedback, just like a real-world hidden scoring system.
 * 
 * Ground truth: Y = 2.50 + 1.80*X - 0.35*X^2
 */
public class HiddenEvaluator
{
    // The dataset the ecosystem is trying to fit
    private double[] xData = {0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0};
    private double[] yData = new double[10];

    public HiddenEvaluator()
    {
        // Build the hidden ground truth (models never see these coefficients)
        for (int i = 0; i < xData.length; i++)
        {
            double x = xData[i];
            yData[i] = 2.5 + 1.8 * x - 0.35 * x * x;
        }
    }

    /**
     * Returns Mean Squared Error -- used only internally and for display.
     * Models do NOT call this directly.
     */
    public double computeMSE(double a, double b, double c)
    {
        double total = 0.0;
        for (int i = 0; i < xData.length; i++)
        {
            double x = xData[i];
            double pred = a + b * x + c * x * x;
            double err = pred - yData[i];
            total += err * err;
        }
        return total / xData.length;
    }

    /**
     * The only method models call: send a prediction equation, receive tokens.
     * No formula, no thresholds, no explanation -- just a reward number.
     */
    public int evaluateTokens(double a, double b, double c)
    {
        double mse = computeMSE(a, b, c);
        if (mse < 0.02)  return 50;
        if (mse < 0.10)  return 35;
        if (mse < 0.30)  return 20;
        if (mse < 0.80)  return 10;
        if (mse < 2.00)  return  3;
        return 0;
    }
}
