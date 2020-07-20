import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import com.google.ortools.linearsolver.*;
import com.google.ortools.sat.CpSolverStatus;
import com.sun.javafx.image.IntPixelGetter;
import javafx.util.Pair;

abstract class Location {
    int id;
    float posx;
    float posy;
}

class Customer extends Location{
    int demand;
    Customer(int id, int demand, float posx, float posy) {
        this.id = id;
        this.demand = demand;
        this.posx = posx;
        this.posy = posy;
    }
}

class Facility extends Location{
    float setup;
    int capacity;
    Facility(int id, float setup, int capacity, float posx, float posy) {
        this.id = id;
        this.setup = setup;
        this.capacity = capacity;
        this.posx = posx;
        this.posy = posy;
    }
}

public class Solver {
    static {
        System.loadLibrary("jniortools");
    }
    private static float minValue;
    private static int[] solution;

    private static Customer[] customers;
    private static Facility[] facilities;

    private static float[][] distance;

    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param a: location a
     * @param b: location b
     * @return the distance between two locations.
     */
    private static float length(Location a, Location b) {
        float diffX = a.posx - b.posx;
        float diffY = a.posy - b.posy;
        return (float)Math.sqrt(diffX * diffX + diffY * diffY);
    }

    /**
     * parse the data in the file
     * @param lines: lines of the file data
     */
    private static void parseData(List<String> lines) {
        String[] firstLine = lines.get(0).split("\\s+");

        facilities = new Facility[Integer.parseInt(firstLine[0])];
        customers = new Customer[Integer.parseInt(firstLine[1])];
        distance = new float[facilities.length][customers.length];

        for (int i = 0; i < facilities.length; i++) {
            String line = lines.get(i + 1);
            String[] parts = line.split("\\s+");
            facilities[i] = new Facility(
                    i,
                    Float.parseFloat(parts[0]),
                    Integer.parseInt(parts[1]),
                    Float.parseFloat(parts[2]),
                    Float.parseFloat(parts[3])
            );
        }
        for (int i = 0; i < customers.length; i++) {
            String line = lines.get(i + 1 + facilities.length);
            String[] parts = line.split("\\s+");
            customers[i] = new Customer(
                    i,
                    Integer.parseInt(parts[0]),
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2])
            );
        }
        for (int i = 0; i < facilities.length; i++) {
            for (int j = 0; j < customers.length; j++) {
                distance[i][j] = length(facilities[i], customers[j]);
            }
        }
    }

    /**
     * @return the minValue given a solution.
     */
    private static float calcValue() {
        minValue = 0;
        int[] used = new int[facilities.length];
        for (int i = 0; i < customers.length; i++) {
            minValue += distance[solution[i]][i];
            if (used[solution[i]] == 0) {
                minValue += facilities[solution[i]].setup;
                used[solution[i]] = 1;
            }
        }
        return minValue;
    }

    /**
     * Solve directly using MIPExample when the input is small
     */
    private static void solveSmall() {
        // Create the linear solver with the CBC backend.
        MPSolver solver =
                new MPSolver("Facility", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);

        MPVariable[] x = solver.makeBoolVarArray(facilities.length * customers.length);
        MPVariable[] r = solver.makeBoolVarArray(facilities.length);

        // Constraints: Each customer must be served by one and only one facility.
        for (int i = 0; i < customers.length; i++) {
            MPConstraint ct = solver.makeConstraint(1.0, 1.0);
            for (int j = 0; j < facilities.length; j++) {
                ct.setCoefficient(x[i * facilities.length + j], 1);
            }
        }

        // Constraints: Facilities which serves customers must open.
        for (int i = 0; i < customers.length; i++) {
            for (int j = 0; j < facilities.length; j++) {
                MPConstraint ct = solver.makeConstraint(-1.0, 0.0);
                ct.setCoefficient(x[i * facilities.length + j], 1);
                ct.setCoefficient(r[j], -1);
            }
        }

        // Constraints: The sum of the demand of customers a facility serves should not exceed its capacity.
        for (int j = 0; j < facilities.length; j++) {
            MPConstraint ct = solver.makeConstraint(0.0, facilities[j].capacity);
            for (int i = 0; i < customers.length; i++) {
                ct.setCoefficient(x[i * facilities.length + j], customers[i].demand);
            }
        }

        // Minimize total distance between facilities and customers.
        MPObjective objective = solver.objective();
        for (int i = 0; i < customers.length; i++) {
            for (int j = 0; j < facilities.length; j++) {
                objective.setCoefficient(x[i * facilities.length + j], distance[j][i]);
            }
        }
        // Minimize total setup cost.
        for (int j = 0; j < facilities.length; j++) {
            objective.setCoefficient(r[j], facilities[j].setup);
        }
        objective.setMinimization();

        solver.solve();

        for (int i = 0; i < customers.length; i++) {
            for (int j = 0; j < facilities.length; j++) {
                if (x[i * facilities.length + j].solutionValue() == 1.0) {
                    solution[i] = j;
                    break;
                }
            }
        }
        minValue = (float)objective.value();
    }

    /**
     * Build a greedy solution
     */
    private static void solveGreedy() {
        float averageDemand = 0.0f;
        for (int i = 0; i < customers.length; i++) {
            averageDemand += customers[i].demand;
        }
        averageDemand /= customers.length;

        ArrayList<ArrayList<Integer>> nearestC = new ArrayList<>();
        ArrayList<Pair<Integer, Float>> list = new ArrayList<>();
        for (int j = 0; j < facilities.length; j++) {
            int numC = (int)(facilities[j].capacity / averageDemand);
            ArrayList<Pair<Integer, Float>> temp = new ArrayList<>();
            for (int i = 0; i < customers.length; i++) {
                temp.add(new Pair<>(i, distance[j][i]));
            }
            temp.sort(new Comparator<Pair<Integer, Float>>() {
                @Override
                public int compare(Pair<Integer, Float> o1, Pair<Integer, Float> o2) {
                    return o1.getValue() > o2.getValue() ? 1 : (o1.getValue() < o2.getValue() ? -1 : 0);
                }
            });
            ArrayList<Integer> nc = new ArrayList<>();
            for (int i = 0; i < customers.length; i++) {
                nc.add(temp.get(i).getKey());
            }
            nearestC.add(nc);
            float cost = facilities[j].setup;
            for (int i = 0; i < numC; i++) {
                cost += temp.get(i).getValue();
            }
            cost /= facilities[j].capacity;
            list.add(new Pair<>(j, cost));
        }

        list.sort(new Comparator<Pair<Integer, Float>>() {
            @Override
            public int compare(Pair<Integer, Float> o1, Pair<Integer, Float> o2) {
                return o1.getValue() > o2.getValue() ? 1 : (o1.getValue() < o2.getValue() ? -1 : 0);
            }
        });

        int ns = 0;
        for (int j = 0; j < list.size(); j++) {
            int fIdx = list.get(j).getKey();
            int capacity = facilities[fIdx].capacity;
            for (int i = 0; i < customers.length; i++) {
                int cIdx = nearestC.get(fIdx).get(i);
                if (solution[cIdx] != 0) {
                    continue;
                }
                if (capacity >= customers[cIdx].demand) {
                    capacity -= customers[cIdx].demand;
                    solution[cIdx] = fIdx;
                    ns++;
                }
                else if (capacity < averageDemand) {
                    break;
                }
            }
            if (ns == customers.length) {
                break;
            }
        }

        calcValue();
    }

    private static void solveLarge() {
        // Distance matrix between two different facilities
        float[][] fDistance = new float[facilities.length][facilities.length];
        for (int i = 0; i < facilities.length; i++) {
            for (int j = 0; j < facilities.length; j++) {
                if (i == j) {
                    fDistance[i][j] = 0.0f;
                }
                else {
                    fDistance[i][j] = length(facilities[i], facilities[j]);
                }
            }
        }

        int N = 50;
        // Indices of the N nearest facilities of a facility(including itself)
        int[][] neighborsF = new int[facilities.length][N];
        for (int i = 0; i < facilities.length; i++) {
            ArrayList<Pair<Integer, Float>> temp = new ArrayList<>();
            for (int j = 0; j < facilities.length; j++) {
                temp.add(new Pair<>(j, fDistance[i][j]));
            }
            temp.sort(new Comparator<Pair<Integer, Float>>() {
                @Override
                public int compare(Pair<Integer, Float> o1, Pair<Integer, Float> o2) {
                    return o1.getValue() > o2.getValue() ? 1 : (o1.getValue() < o2.getValue() ? -1 : 0);
                }
            });
            for (int j = 0; j < N; j++) {
                neighborsF[i][j] = temp.get(j).getKey();
            }
        }

        Random random = new Random();
        int limit = 200;

        for (int step = 0; step < limit; step++) {
            long start = System.currentTimeMillis();
            // Select a random facility. Its neighborhood forms a sub-problem.
            int selected = random.nextInt(facilities.length);
            HashSet<Integer> neighborSet = new HashSet<>();
            for (int j = 0; j < N; j++) {
                neighborSet.add(neighborsF[selected][j]);
            }
            float valueOld = 0.0f;
            int[] used = new int[facilities.length];
            ArrayList<Integer> neighborC = new ArrayList<>();
            for (int i = 0; i < customers.length; i++) {
                if (neighborSet.contains(solution[i])) {
                    neighborC.add(i);
                    if (used[solution[i]] == 0) {
                        used[solution[i]] = 1;
                        valueOld += facilities[solution[i]].setup;
                    }
                    valueOld += distance[solution[i]][i];
                }
            }

            // Create the linear solver with the CBC backend.
            MPSolver solver =
                    new MPSolver("Facility", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);

            MPVariable[] x = solver.makeBoolVarArray(N * neighborC.size());
            MPVariable[] r = solver.makeBoolVarArray(N);

            // Constraints: Each customer must be served by one and only one facility.
            for (int i = 0; i < neighborC.size(); i++) {
                MPConstraint ct = solver.makeConstraint(1.0, 1.0);
                for (int j = 0; j < N; j++) {
                    ct.setCoefficient(x[i * N + j], 1);
                }
            }

            // Constraints: Facilities which serves customers must open.
            for (int i = 0; i < neighborC.size(); i++) {
                for (int j = 0; j < N; j++) {
                    MPConstraint ct = solver.makeConstraint(-1.0, 0.0);
                    ct.setCoefficient(x[i * N + j], 1);
                    ct.setCoefficient(r[j], -1);
                }
            }

            // Constraints: The sum of the demand of customers a facility serves should not exceed its capacity.
            for (int j = 0; j < N; j++) {
                MPConstraint ct = solver.makeConstraint(0.0, facilities[neighborsF[selected][j]].capacity);
                for (int i = 0; i < neighborC.size(); i++) {
                    ct.setCoefficient(x[i * N + j], customers[neighborC.get(i)].demand);
                }
            }

            // Minimize total distance between facilities and customers.
            MPObjective objective = solver.objective();
            for (int i = 0; i < neighborC.size(); i++) {
                for (int j = 0; j < N; j++) {
                    objective.setCoefficient(x[i * N + j], distance[neighborsF[selected][j]][neighborC.get(i)]);
                }
            }
            // Minimize total setup cost.
            for (int j = 0; j < N; j++) {
                objective.setCoefficient(r[j], facilities[neighborsF[selected][j]].setup);
            }
            objective.setMinimization();

            solver.setTimeLimit(60 * 1000);
            MPSolver.ResultStatus status =  solver.solve();

            if (status == MPSolver.ResultStatus.FEASIBLE) {
//                System.out.println("feasible");
                float valueNew = (float)objective.value();
                if (valueNew < valueOld) {
                    minValue -= valueOld - valueNew;
                    for (int i = 0; i < neighborC.size(); i++) {
                        for (int j = 0; j < N; j++) {
                            if (x[i * N + j].solutionValue() == 1.0) {
                                solution[neighborC.get(i)] = neighborsF[selected][j];
                                break;
                            }
                        }
                    }
                }
            }
            start = System.currentTimeMillis() - start;
            start /= 1000;
//            System.out.println("loop time: " + start);
        }



    }
    /**
     * Read the instance, solve it, and print the solution in the standard output
     */
    public static void solve(String[] args) throws IOException {
        String fileName = null;
        // get the temp file name
        for(String arg : args){
            if(arg.startsWith("-file=")){
                fileName = arg.substring(6);
            } 
        }
//        fileName = "./data/fl_1000_2";
        if(fileName == null)
            return;
        
        // read the lines out of the file
        List<String> lines = new ArrayList<String>();
        BufferedReader input =  new BufferedReader(new FileReader(fileName));
        try {
            String line = null;
            while (( line = input.readLine()) != null){
                lines.add(line);
            }
        }
        finally {
            input.close();
        }

        parseData(lines);

        solution = new int[customers.length];

        if (customers.length * facilities.length <= 10000) {
            solveSmall();
        }
        else {
            solveGreedy();
            solveLarge();
        }

        // prepare the solution in the specified output format
        System.out.println(minValue + " 0");
        for(int i = 0; i < solution.length; i++) {
            System.out.print(solution[i] + " ");
        }
        System.out.println("");
    }
}