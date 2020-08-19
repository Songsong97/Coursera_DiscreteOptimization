import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.lang.Math;

class Tabu {
    private int size;
    LinkedList<Integer> tabuQueue;
    private HashSet<Integer> tabuSet;

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

    void pop() {
        int node = tabuQueue.removeFirst();
        tabuSet.remove(node);
    }
}

public class Solver {
    // constants with respect to a given problem
    private static int nodeCount;
    private static int vehicleCount;
    private static int capacity;
    private static float distantLength;
    private static float averageDemand;

    // demands and locations of each customer, with 0 representing the warehouse
    private static int[] demand;
    private static float[][] points;

    // distance matrix
    private static float[][] distance;

    // capacity violation of each vehicle
    private static int[] violation;

    // the best objective and solution found so far
    private static float minValue;
    private static int[][] solution;

    // random generator
    private static Random random = new Random(0);

    // other variables
    private static float[][] centroids;
    private static ArrayList<ArrayList<Integer>> visits;

    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static float length(float[] a, float[] b) {
        float v1 = a[0] - b[0];
        float v2 = a[1] - b[1];
        return (float)Math.sqrt(v1 * v1 + v2 * v2);
    }

    private static void prepareSolution() {
        minValue = 0;
        solution = new int[vehicleCount][];
        for (int i = 0; i < vehicleCount; i++) {
            solution[i] = new int[visits.get(i).size()];
            for (int j = 0; j < solution[i].length; j++) {
                solution[i][j] = visits.get(i).get(j);
            }
            minValue += getCost(i);
        }
    }

    private static float getCost(int vehicleIndex) {
        if (visits.get(vehicleIndex).size() == 0) {
            return 0;
        }
        visits.get(vehicleIndex).add(0);
        final int sz = visits.get(vehicleIndex).size();
        float result = 0;
        for (int i = 0; i < sz; i++) {
            int node = visits.get(vehicleIndex).get(i);
            int next = visits.get(vehicleIndex).get((i + 1) % sz);
            result += distance[node][next];
        }
        visits.get(vehicleIndex).remove(sz - 1);
        return result;
    }

    private static int getDemand(int vehicleIndex) {
        int result = 0;
        for (int node : visits.get(vehicleIndex)) {
            result += demand[node];
        }
        return result;
    }

    // Use greedy algorithm to generate an initial path for a given vehicle
    private static float greedy(int vehicleIndex) {
        float result = 0;
        visits.get(vehicleIndex).add(0);
        final int sz = visits.get(vehicleIndex).size();
        HashSet<Integer> set = new HashSet<>();
        for (int i = 0; i < sz; i++) {
            set.add(visits.get(vehicleIndex).get(i));
        }
        int start = visits.get(vehicleIndex).get(random.nextInt(sz));
        visits.get(vehicleIndex).clear();
        int p = start;
        set.remove(p);
        int zero = 0;
        ArrayList<Integer> tempList = new ArrayList<>();
        tempList.add(p);
        while (!set.isEmpty()) {
            float minD = Float.MAX_VALUE;
            int n = -1;
            for (int node : set) {
                float d = distance[node][p];
                if (d < minD) {
                    minD = d;
                    n = node;
                }
            }
            set.remove(n);
            result += minD;
            p = n;
            tempList.add(p);
            if (p == 0) {
                zero = tempList.size() - 1;
            }
        }
        result += distance[p][start];
        for (int i = (1 + zero) % sz, ct = 1; ct < sz; ct++, i = (i + 1) % sz) {
            visits.get(vehicleIndex).add(tempList.get(i));
        }
        return result;
    }

    // Break the crossing in the path of a given vehicle
    private static float kOpt(int vehicleIndex, int k, Tabu tabu) {
        k--; // 2-Opt means we are swapping 2 edges once, and we swap edges incrementally
        int selected = -1;
        int selectedNext = 0;
        visits.get(vehicleIndex).add(0);
        final int sz = visits.get(vehicleIndex).size();
        float maxDist = 0;
        for (int i = 0; i < sz; i++) {
            int node = visits.get(vehicleIndex).get(i);
            int next = visits.get(vehicleIndex).get((i + 1) % sz);
            if (distance[node][next] > maxDist && !tabu.contains(node)) {
                maxDist = distance[node][next];
                selected = node;
                selectedNext = next;
            }
        }
        assert selected != -1; // We are promised to get a node

        // The best solution we get in this k-Opt operation
        Integer[] best = visits.get(vehicleIndex).toArray(new Integer[sz]);
        float diff = 0;
        float minDiff = 0;
        while (k-- > 0 && !tabu.contains(selected)) {
            tabu.push(selected);
            float improvement = 0; // improvement is negative since we want to reduce the cost
            float foundEdge = 0;
            int candidate = -1;
            for (int i = 0; i < sz; i++) {
                int node = visits.get(vehicleIndex).get(i);
                int next = visits.get(vehicleIndex).get((i + 1) % sz);
                if (node != selected && next != selected && node != selectedNext
                        && !tabu.contains(candidate)) {
                    float newEdge = length(points[node], points[selected]);
                    float newEdge2 = length(points[next], points[selectedNext]);
                    float temp = newEdge + newEdge2 - maxDist - distance[node][next];
                    if (temp < improvement) {
                        improvement = temp;
                        candidate = node;
                        foundEdge = newEdge2;
                    }
                }
            }
            if (candidate == -1) {
                break;
            }

            // reorder the path
            int start = visits.get(vehicleIndex).indexOf(selectedNext);
            int end = visits.get(vehicleIndex).indexOf(candidate);
            int ct = start < end ? (end - start + 1) : (sz - start + end + 1);
            while (ct >= 2) {
                int node1 = visits.get(vehicleIndex).get(start);
                int node2 = visits.get(vehicleIndex).get(end);
                visits.get(vehicleIndex).set(start, node2);
                visits.get(vehicleIndex).set(end, node1);
                start = (start + 1) % sz;
                end = (end - 1 + sz) % sz;
                ct -= 2;
            }

            selected = selectedNext;
            maxDist = foundEdge;
            diff += improvement;
            if (diff < minDiff) {
                minDiff = diff;
                best = visits.get(vehicleIndex).toArray(new Integer[sz]);
            }
        }

        // reconstruct the solution
        visits.get(vehicleIndex).clear();
        int zero = 0;
        while (zero < sz && best[zero] != 0) {
            zero++;
        }
        for (int i = (zero + 1) % sz, ct = 1; ct < sz; ct++, i = (i + 1) % sz) {
            visits.get(vehicleIndex).add(best[i]);
        }
        return minDiff;
    }

    // Use k-Opt to optimize a single vehicle's path, this is the same as a traveling salesman problem
    private static void optimizeVehicle(int vehicleIndex) {
        if (visits.get(vehicleIndex).size() == 0) {
            return;
        }
        final int sz = visits.get(vehicleIndex).size();
        int tryCount = 0;
        int tryLimit = 1000; // todo: try a smaller value?
        float minCost = greedy(vehicleIndex);
        Integer[] best = visits.get(vehicleIndex).toArray(new Integer[sz]);
        while (tryCount++ < tryLimit) {
//            System.out.println("vehicle: " + vehicleIndex + ", " + tryCount);
            int tabuSize = Math.min(sz / 2 + 2, sz - 1);
            Tabu t = new Tabu(tabuSize);
            float cost = greedy(vehicleIndex);
            int threshold = 20;
            int pressure = 0;
            while (pressure < threshold) {
                float diff = kOpt(vehicleIndex, 3, t);
                cost += diff;
                if (diff == 0) {
                    pressure++;
                }
                else {
                    if (pressure != 0) {
                        threshold--;
                    }
                    pressure = 0;
                }
            }
            if (minCost > cost) {
                minCost = cost;
                best = visits.get(vehicleIndex).toArray(new Integer[sz]);
            }
        }

        visits.get(vehicleIndex).clear();
        for (int node : best) {
            visits.get(vehicleIndex).add(node);
        }
    }

    // Relocate a node from a vehicle to another vehicle's path
    private static void relocate(int from, int to, int fromPos, int toPos) {
        int node = visits.get(from).get(fromPos);
        visits.get(from).remove(fromPos);
        visits.get(to).add(toPos + 1, node);
        violation[from] -= demand[node];
        violation[to] += demand[node];
    }

    // Exchange two nodes from two different vehicles' paths
    private static void exchange(int from, int to, int fromPos, int toPos) {
        int node = visits.get(from).get(fromPos);
        int other = visits.get(to).get(toPos);
        visits.get(from).set(fromPos, other);
        visits.get(to).set(toPos, node);
        violation[from] = violation[from] - demand[node] + demand[other];
        violation[to] = violation[to] - demand[other] + demand[node];
    }

    /**
     * The strategy of the local search is described below. The method here is not optimal and may easily get stuck
     * in local minima, but it can generate a "good" feasible solution quickly.
     *
     * First, we group nodes into different clusters using k-means algorithm. This helps the solution maintain a
     * locality structure in geography.
     *
     * Then, we repeat a process until we don't have violations. In each loop, we find the vehicle with the largest
     * capacity violation and try every other node with a "relocate" and "exchange" strategy.
     *
     * I combined the feasibility and cost of the problem into one score using a simple formula:
     *         score = violationImprovement / averageDemand - u * distancePenalty / distantLength
     * where "averageDemand" is the mean of all customers' demands and "distantLength" is the largest distance between
     * two points. "violationImprovement" is the reduction of violation after a "relocate" or "exchange" operation and
     * "distancePenalty" is the increase in total distance(cost) after that operation.
     *
     * In the formula, u is used to scale the contribution of the distance penalty to the score of that operation.
     * If we cannot find positive score, it means we have to compromise longer travel distance to make the solution
     * feasible. So, if that happens, the algorithm reduces u gradually.
     */
    private static void search() {
        kMeans();

        for (int i = 0; i < vehicleCount; i++) {
            optimizeVehicle(i);
            violation[i] = getDemand(i) - capacity;
        }

        float u = 5.0f;
        while (true) {
            int selectedVehicle = -1;
            int maxViolation = 0;
            for (int i = 0; i < vehicleCount; i++) {
                if (violation[i] > maxViolation) {
                    maxViolation = violation[i];
                    selectedVehicle = i;
                }
            }
            if (maxViolation == 0) { // we are done
                break;
            }

            float maxScore = 0;
            int operation = -1; // 0 means relocate, 1 means exchange
            int candidateVehicle = -1;
            int fromPos = -1;
            int toPos = -1;
            for (int i = 0, sz = visits.get(selectedVehicle).size(); i < sz; i++) {
                int node = visits.get(selectedVehicle).get(i);
                int prev = i > 0 ? visits.get(selectedVehicle).get(i - 1) : 0;
                int next = i < (sz - 1) ? visits.get(selectedVehicle).get(i + 1) : 0;
                for (int j = 0; j < vehicleCount; j++) {
                    if (j == selectedVehicle) {
                        continue;
                    }

                    // relocate
                    int violationImprovement = Math.max(maxViolation, 0) + Math.max(violation[j], 0)
                            - (Math.max(maxViolation - demand[node], 0) + Math.max(violation[j] + demand[node], 0));
                    for (int candidateIdx = -1, csz = visits.get(j).size(); candidateIdx < csz; candidateIdx++) {
                        int candidate = candidateIdx == -1 ? 0 : visits.get(j).get(candidateIdx);
                        int candidateNext = candidateIdx < (csz - 1) ? visits.get(j).get(candidateIdx + 1) : 0;
                        float distancePenalty = -distance[node][next] - distance[node][prev]
                                + distance[candidate][node] + distance[candidateNext][node]
                                + distance[next][prev] - distance[candidate][candidateNext];
                        float score = violationImprovement / averageDemand
                                - u * distancePenalty / distantLength;
                        if (score > maxScore) {
                            maxScore = score;
                            operation = 0;
                            candidateVehicle = j;
                            fromPos = i;
                            toPos = candidateIdx;
                        }
                    }

                    // exchange
                    for (int candidateIdx = 0, csz = visits.get(j).size(); candidateIdx < csz; candidateIdx++) {
                        int candidate = visits.get(j).get(candidateIdx);
                        int candidatePrev = candidateIdx > 0 ? visits.get(j).get(candidateIdx - 1) : 0;
                        int candidateNext = candidateIdx < (csz - 1) ? visits.get(j).get(candidateIdx + 1) : 0;
                        float distancePenalty = -distance[node][next] - distance[node][prev]
                                - distance[candidate][candidateNext] - distance[candidate][candidatePrev]
                                + distance[candidatePrev][node] + distance[candidateNext][node]
                                + distance[next][candidate] + distance[prev][candidate];
                        violationImprovement = Math.max(maxViolation, 0) + Math.max(violation[j], 0)
                                - Math.max(maxViolation - demand[node] + demand[candidate], 0)
                                - Math.max(violation[j] - demand[candidate] + demand[node], 0);
                        float score = violationImprovement / averageDemand
                                - u * distancePenalty / distantLength;
                        if (score > maxScore) {
                            maxScore = score;
                            operation = 1;
                            candidateVehicle = j;
                            fromPos = i;
                            toPos = candidateIdx;
                        }
                    }
                }
            }

//            System.out.println(maxScore);
            if (maxScore == 0) {
                u *= 0.9;
            }
            else {
                if (operation == 0) {
                    // call relocate method and optimize two vehicles
                    relocate(selectedVehicle, candidateVehicle, fromPos, toPos);
                    optimizeVehicle(selectedVehicle);
                    optimizeVehicle(candidateVehicle);
                }
                else if (operation == 1){
                    // call exchange method and optimize two vehicles
                    exchange(selectedVehicle, candidateVehicle, fromPos, toPos);
                    optimizeVehicle(selectedVehicle);
                    optimizeVehicle(candidateVehicle);
                }
            }
        }

        prepareSolution();
    }

    private static void kMeans() {
        visits = new ArrayList<>();
        for (int i = 0; i < vehicleCount; i++) {
            visits.add(new ArrayList<>());
        }
        centroids = new float[vehicleCount][2];
        for (int i = 0; i < vehicleCount; i++) {
            int randomIdx = random.nextInt(nodeCount);
            centroids[i][0] = points[randomIdx][0];
            centroids[i][1] = points[randomIdx][1];
        }

        while (true) {
            for (int i = 0; i < vehicleCount; i++) {
                visits.get(i).clear();
            }
            for (int i = 1; i < points.length; i++) {
                int assigned = 0;
                float minLen = length(points[i], centroids[0]);
                for (int j = 1; j < centroids.length; j++) {
                    float len = length(points[i], centroids[j]);
                    if (len < minLen) {
                        assigned = j;
                        minLen = len;
                    }
                }
                visits.get(assigned).add(i);
            }
            float diff = 0;
            boolean bias = true; // if bias is set, each group(vehicle) will at least have the depot as its member
            for (int i = 0; i < centroids.length; i++) {
                float[] pos = new float[2];
                if (bias) {
                    pos[0] = points[0][0];
                    pos[1] = points[0][1];
                }
                else if (visits.get(i).size() == 0) {
                    continue;
                }
                for (int j = 0; j < visits.get(i).size(); j++) {
                    pos[0] += points[visits.get(i).get(j)][0];
                    pos[1] += points[visits.get(i).get(j)][1];
                }
                pos[0] /= (visits.get(i).size() + (bias ? 1 : 0));
                pos[1] /= (visits.get(i).size() + (bias ? 1 : 0));
                diff += length(pos, centroids[i]);
                centroids[i] = pos;
            }
//            System.out.println("diff = " + diff);
            if (diff < 0.01) {
                break;
            }
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
//        fileName = "./data/vrp_421_41_1";
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
        vehicleCount = Integer.parseInt(firstLine[1]);
        capacity = Integer.parseInt(firstLine[2]);

        points = new float[nodeCount][2];
        demand = new int[nodeCount];
        violation = new int[vehicleCount];
        averageDemand = 0;
        for (int i = 0; i < nodeCount; i++) {
            String line = lines.get(i + 1);
            String[] parts = line.split("\\s+");
            demand[i] = Integer.parseInt(parts[0]);
            points[i][0] = Float.parseFloat(parts[1]);
            points[i][1] = Float.parseFloat(parts[2]);
            averageDemand += demand[i];
        }
        averageDemand /= nodeCount - 1;

        distance = new float[nodeCount][nodeCount];
        distantLength = 0;
        for (int i = 0; i < nodeCount; i++) {
            for (int j = 0; j < i; j++) {
                float dist = length(points[i], points[j]);
                distance[i][j] = dist;
                distance[j][i] = dist;
                distantLength = Math.max(distantLength, dist);
            }
        }

        search();

        // prepare the solution in the specified output format
        System.out.println(minValue + " 0");
        for (int i = 0; i < solution.length; i++) {
            System.out.print("0 ");
            for (int j = 0; j < solution[i].length; j++) {
                System.out.print(solution[i][j] + " ");
            }
            System.out.println("0");
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter("solution"));
        try {
            writer.write(minValue + " 0");
            writer.newLine();
            for (int i = 0; i < solution.length; i++) {
                writer.write("0 ");
                for (int j = 0; j < solution[i].length; j++) {
                    writer.write(solution[i][j] + " ");
                }
                writer.write("0");
                writer.newLine();
            }
        }
        finally {
            writer.close();
        }
    }
}