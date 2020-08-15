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

    // demands and locations of each customer, with 0 representing the warehouse
    private static int[] demand;
    private static float[][] points;

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

//    private static float kOpt(int k) {
//        k--; // 2-Opt means we are swapping 2 edges once, and we swap edges incrementally
//        float minDiff = 0;
//        float diff = 0;
//        float maxDist = 0;
//        int selected = -1;
//        for (int i = 0; i < nodeCount; i++) {
//            if (dist[i] > maxDist && !tabu.contains(i)) {
//                maxDist = dist[i];
//                selected = i;
//            }
//        }
//
//        assert selected != -1; // We are promised to get a node
//
//        int[] bestNext = next.clone(); // The best solution we get in this k-Opt operation
//        while (k-- > 0 && !tabu.contains(selected)) {
//            tabu.push(selected);
//            float foundEdge = 0;
//            float foundEdge2 = 0;
//            float improvement = 0;
//            int candidate = -1;
//            for (int i = 0; i < nodeCount; i++) {
//                if (i != selected && i != next[selected] && i != prev[selected]
//                        && !tabu.contains(candidate)) {
//                    float newEdge = length(points[i], points[selected]);
//                    float newEdge2 = length(points[next[i]], points[next[selected]]);
//                    float temp = newEdge + newEdge2 - maxDist - dist[i];
//                    if (temp <improvement) {
//                        improvement = temp;
//                        foundEdge = newEdge;
//                        foundEdge2 = newEdge2;
//                        candidate = i;
//                    }
//                }
//            }
//            if (candidate == -1) {
//                break;
//            }
//            int candidateNext = next[candidate];
//            int selectedNext = next[selected];
//            diff += improvement;
//            next[selected] = candidate;
//            dist[selected] = foundEdge;
//            for (int j = candidate; j != selectedNext; j = prev[j]) {
//                dist[j] = dist[prev[j]];
//                next[j] = prev[j];
//            }
//            for (int j = selected; j != selectedNext; j = next[j]) {
//                prev[next[j]] = j;
//            }
//            next[selectedNext] = candidateNext;
//            prev[candidateNext] = selectedNext;
//            dist[selectedNext] = foundEdge2;
//            selected = selectedNext;
//            maxDist = foundEdge2;
//            if (diff < minDiff) {
//                minDiff = diff;
//                bestNext = next.clone();
//            }
//        }
//
//        next = bestNext.clone();
//        reconstructTour();
//        return minDiff;
//    }


    private static void kMeans() {
        visits = new ArrayList<>();
        for (int i = 0; i < vehicleCount; i++) {
            visits.add(new ArrayList<>());
        }
        centroids = new float[vehicleCount][2];
        // todo: may need reinitialization
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
            boolean bias = true;
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
            System.out.println("diff = " + diff);
            if (diff < 0.01) {
                break;
            }
        }
        solution = new int[vehicleCount][];
        for (int i = 0; i < vehicleCount; i++) {
            solution[i] = new int[visits.get(i).size()];
            for (int j = 0; j < solution[i].length; j++) {
                solution[i][j] = visits.get(i).get(j);
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
        fileName = "./data/vrp_421_41_1";
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
        for (int i = 0; i < nodeCount; i++) {
            String line = lines.get(i + 1);
            String[] parts = line.split("\\s+");
            demand[i] = Integer.parseInt(parts[0]);
            points[i][0] = Float.parseFloat(parts[1]);
            points[i][1] = Float.parseFloat(parts[2]);
        }

        kMeans();





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