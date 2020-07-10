import java.io.*;
import java.util.*;
import java.lang.Math;

class Tabu {
    private int size;
    private HashSet<Integer> tabuSet;
    private LinkedList<Integer> tabuQueue;

    Tabu(int size) {
        this.size = size;
        tabuSet = new HashSet<>();
        tabuQueue = new LinkedList<>();
    }

    boolean contains(int node) {
        return tabuSet.contains(node);
    }

    void push(int node) {
        if (tabuSet.contains(node)) {
            return;
        }
        tabuSet.add(node);
        tabuQueue.addLast(node);
        if (tabuSet.size() > size) {
            pop();
        }
    }

    int pop() {
        int node = tabuQueue.removeFirst();
        tabuSet.remove(node);
        return node;
    }
}

public class AdvancedSolver {
    private static int nodeCount;
    private static int edgeCount;
    private static ArrayList<ArrayList<Integer>> edges;
    private static int[] solution;
    private static int minValue;

    private static int[] color; // current coloring for the graph
    private static int[] violation; // number of neighbors which have the same color as the node
    private static int numViolation;
    private static Tabu tabu;
    private static Random random = new Random();
    private static Random removeRandom = new Random();

    private static int sample(int start, int end) {
        assert end >= start : "Invalid uniform distribution";
        return start + random.nextInt(end - start + 1);
    }

    /**
     * @return the next node to change color.
     */
    private static int selectNextNode() {
        int maxViolation = 0;
        ArrayList<Integer> candidates = new ArrayList<>();

        // Select node with maximum violation with neighbors
        for (int i = 0; i < nodeCount; i++) {
            if (violation[i] == 0) {
                continue;
             }
            if (tabu.contains(i)) { // Do not consider nodes in the tabu
                continue;
            }
            if (maxViolation == violation[i]) {
                candidates.add(i);
            }
            else if (maxViolation < violation[i]) {
                maxViolation = violation[i];
                candidates.clear();
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return -1;
        }

        // Uniformly sample a node from the candidates
        return candidates.get(sample(0, candidates.size() - 1));
    }

    /**
     * Change the color of node.
     * @param node: the node to change color.
     * @param availableColors: the number of colors we can use for now.
     */
    private static void changeColor(int node, int availableColors) {
        int[] colorCount = new int[availableColors];
        ArrayList<Integer> neighbors = edges.get(node);

        for (int neighbor : neighbors) {
            colorCount[color[neighbor]]++;
        }
        int minViolation = nodeCount;
        ArrayList<Integer> candidates = new ArrayList<>();

        // Select color with least violation with neighbors
        for (int c = 0; c < availableColors; c++) {
            if (c == color[node]) {
                continue;
            }
            if (minViolation > colorCount[c]) {
                minViolation = colorCount[c];
                candidates.clear();
                candidates.add(c);
            }
            else if (minViolation == colorCount[c]) {
                candidates.add(c);
            }
        }

        // Uniformly sample a color from the candidates
        int newColor = candidates.get(sample(0, candidates.size() - 1));
        for (int neighbor : neighbors) {
            if (color[neighbor] == color[node]) {
                violation[neighbor]--;
                violation[node]--;
                numViolation -= 2;
            }
            else if (color[neighbor] == newColor) {
                violation[neighbor]++;
                violation[node]++;
                numViolation += 2;
            }
        }
        color[node] = newColor;
    }

    /**
     * Compute how many violations this coloring produces.
     */
    private static void calcViolation() {
        violation = new int[nodeCount];
        numViolation = 0;
        for (int i = 0; i < nodeCount; i++) {
            int ct = 0;
            for (int neighbor : edges.get(i)) {
                ct += color[i] == color[neighbor] ? 1 : 0;
            }
            violation[i] = ct;
            numViolation += ct;
        }
    }

    /**
     * Check if it is possible to find a solution with availableColors.
     * @param tabuSize: size of the tabu.
     * @param availableColors: we can only choose from [0, availableColors) to color a node.
     * @return whether it is feasible in limited steps.
     */
    private static boolean isFeasible(int tabuSize, int availableColors) {
        // We only allow the program to change the color for limited steps.
        int limit = 50000;
        int ct = 0;

        calcViolation();

        tabu = new Tabu(tabuSize);
        while (ct < limit && numViolation > 0) {
            int node = selectNextNode();

            // If we cannot find a node, the tabu list may be too long, pop one node
            while (node == -1) {
                tabu.pop();
                node = selectNextNode();
            }

            tabu.push(node);
            changeColor(node, availableColors);
            ct++;
        }

        return numViolation == 0;
    }

    /**
     * Randomize colors for all nodes. This is used to initialize the solver.
     * @param availableColors: we can only choose from [0, availableColors) to color a node.
     */
    private static void randomizeColor(int availableColors) {
        color = new int[nodeCount];
        for (int i = 0; i < color.length; i++) {
            color[i] = sample(0, availableColors - 1);
        }
    }

    /**
     * Remove a color from current color choice.
     * It works like this, suppose there are total 10 colors from 0 ~ 9, the current color choice is:
     * 7 1 4 2 5 9 0 3 6 8 5 9 0 4
     * if we want to remove color 5, for each color that is bigger than 5, we minus it by 1:
     * 6 1 4 2 5 8 0 3 5 7 5 8 0 4
     * then for each color equals to 5 we set it to a random color from 0 ~ 8:
     * 6 1 4 2 3 8 0 3 1 7 4 8 0 4
     * @param availableColors: we can only choose from [0, availableColors) to color a node.
     */
    private static void removeColor(int availableColors) {
        int toRemove = removeRandom.nextInt(availableColors);
        for (int i = 0; i < color.length; i++) {
            if (color[i] == toRemove) {
                color[i] = sample(0, availableColors - 2);
            }
            else if (color[i] > toRemove) {
                color[i]--;
            }
        }
    }

    /**
     * Using local search to find a solution.
     */
    private static void search() {
        randomizeColor(nodeCount);
        int tabuSize = nodeCount / 10;
        solution = new int[nodeCount];
        minValue = nodeCount;
        for (int availableColors = nodeCount; availableColors > 0; availableColors--) {
            // Retry if no feasible solution is found in limited steps
            int retryLimit = 100 + nodeCount / 20;
            int retryCount = 0;
            while (retryCount < retryLimit) {
                if (isFeasible(tabuSize, availableColors)) {
                    solution = color.clone();
                    minValue = availableColors;
                    removeColor(availableColors);
                    calcViolation();
                    break;
                }
                color = solution.clone();
                removeColor(minValue);
                calcViolation();
                retryCount++;
            }
            if (retryCount >= retryLimit) {
                return;
            }
        }
    }

    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
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

        // parse the data in the file
        String[] firstLine = lines.get(0).split("\\s+");
        nodeCount = Integer.parseInt(firstLine[0]);
        edgeCount = Integer.parseInt(firstLine[1]);

        edges = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            edges.add(new ArrayList<>());
        }

        for (int i = 1; i <= edgeCount; i++) {
            String line = lines.get(i);
            String[] parts = line.split("\\s+");
            int u = Integer.parseInt(parts[0]);
            int v = Integer.parseInt(parts[1]);
            edges.get(u).add(v);
            edges.get(v).add(u);
        }

        search();

        // prepare the solution in the specified output format
        System.out.println(minValue + " 0");
        for(int i = 0; i < nodeCount; i++) {
            System.out.print(solution[i] + " ");
        }
        System.out.println("");
    }
}