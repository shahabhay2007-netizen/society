import java.util.*;

class Evaluator {

    public int evaluate(double actual, double predicted) {

        double error = Math.abs(actual - predicted);

        if (error <= 0.5) return 10;
        else if (error <= 1) return 7;
        else if (error <= 2) return 4;
        else if (error <= 4) return 1;
        else return 0;
    }
}

class ModelRegistry {

    static ArrayList<ModelAgent> models = new ArrayList<>();

    static synchronized void register(ModelAgent m) {
        models.add(m);
    }

    static synchronized void display() {
        System.out.println("\nALL MODEL INSTANCES");

        for (ModelAgent m : models) {
            System.out.println(m.name +
                " | Generation: " + m.generation +
                " | Tokens: " + m.getTokens());
        }
    }
}

class ResourceManager {

    static synchronized void fund(ModelAgent parent,
                                  ModelAgent child, int amount) {

        int given = parent.withdraw(amount);
        child.addTokens(given);

        System.out.println(parent.name +
            " funded " + child.name + " with " + given + " tokens");
    }

    static synchronized void transfer(ModelAgent child,
                                       ModelAgent parent, int amount) {

        int given = child.withdraw(amount);
        parent.addTokens(given);

        System.out.println(child.name +
            " returned " + given + " tokens to " + parent.name);
    }
}

class ModelAgent extends Thread {

    String name;

    double a;
    double b;

    int tokens = 10;
    int generation;

    ModelAgent parent;

    ArrayList<ModelAgent> children = new ArrayList<>();

    static final int MAX_GENERATION = 2;

    Evaluator evaluator;

    double x[] = {1, 2, 3, 4, 5};
    double y[] = {2, 4, 5, 4, 6};

    ModelAgent(String name, double a, double b,
               int generation, ModelAgent parent,
               Evaluator evaluator) {

        this.name = name;
        this.a = a;
        this.b = b;
        this.generation = generation;
        this.parent = parent;
        this.evaluator = evaluator;
    }

    synchronized void addTokens(int amount) {
        tokens += amount;
    }

    synchronized int withdraw(int amount) {

        if (tokens >= amount) {
            tokens -= amount;
            return amount;
        }

        return 0;
    }

    synchronized int getTokens() {
        return tokens;
    }

    int score(double ca, double cb) {

        int total = 0;

        for (int i = 0; i < x.length; i++) {

            double predicted = ca + cb * x[i];

            total += evaluator.evaluate(y[i], predicted);
        }

        return total;
    }

    void improve() {

        int bestScore = score(a, b);

        for (int i = 0; i < 100; i++) {

            double newA = a + (Math.random() - 0.5) * 0.4;
            double newB = b + (Math.random() - 0.5) * 0.4;

            int newScore = score(newA, newB);

            if (newScore > bestScore) {

                a = newA;
                b = newB;

                bestScore = newScore;
            }
        }
    }

    void createChild() {

        if (generation >= MAX_GENERATION) {
            return;
        }

        if (getTokens() < 20) {
            return;
        }

        String childName = name + "_child";

        ModelAgent child = new ModelAgent(
            childName,
            a,
            b,
            generation + 1,
            this,
            evaluator
        );

        ResourceManager.fund(this, child, 5);

        children.add(child);
        ModelRegistry.register(child);

        child.start();

        try {
            child.join();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        int support = child.getTokens() / 5;

        ResourceManager.transfer(child, this, support);
    }

    public void run() {

        System.out.println("\n" + name + " started working");

        improve();

        int earned = score(a, b);

        addTokens(earned);

        System.out.println(name + " improved itself");
        System.out.println("Equation: Y = " + a + " + " + b + "X");
        System.out.println("Tokens earned: " + earned);

        createChild();

        System.out.println(name + " finished working");
    }
}

public class Main {

    public static void main(String[] args) {

        Evaluator evaluator = new Evaluator();

        ModelAgent A = new ModelAgent(
            "Model_A", 0, 0, 0, null, evaluator
        );

        ModelAgent B = new ModelAgent(
            "Model_B", 1, 0.5, 0, null, evaluator
        );

        ModelRegistry.register(A);
        ModelRegistry.register(B);

        A.start();
        B.start();

        try {
            A.join();
            B.join();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ModelRegistry.display();
    }
}
