import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public class MIPExample {
    static {
        System.loadLibrary("jniortools");
    }

    public static void main(String[] args) throws Exception {
        // Create the linear solver with the CBC backend.
        MPSolver solver =
                new MPSolver("SimpleLpProgram", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);

        MPVariable[] vars = solver.makeBoolVarArray(2);
        System.out.println("Number of variables = " + solver.numVariables());

        MPConstraint ct = solver.makeConstraint(1, 1, "ct");
        ct.setCoefficient(vars[0], 1);
        System.out.println("Number of constraints = " + solver.numConstraints());

        // Create the objective function, 3 * x + y.
        MPObjective objective = solver.objective();
        objective.setCoefficient(vars[0], 1);
        objective.setCoefficient(vars[1], -1);
        objective.setMaximization();

        solver.solve();

        System.out.println("Solution:");
        System.out.println("Objective value = " + objective.value());
        System.out.println("x = " + vars[0].solutionValue());
        System.out.println("y = " + vars[1].solutionValue());
    }
}
