import java.io.*;
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
    private static int nodeCount;
    private static float[][] points;
    private static float minValue;
    private static int[] solution;

    private static int[] next;
    private static int[] prev;
    private static float[] dist;

    private static Random random = new Random();
    private static Tabu tabu;

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

    /**
     * Randomly generates a tour sequence.
     * @return the total distance of the tour
     */
    private static float shuffle() {
        float result = 0;
        for (int i = 0; i < next.length; i++) {
            next[i] = i;
        }
        for (int i = next.length - 1; i > 0; i--) {
            int j = random.nextInt(i);
            int temp = next[i];
            next[i] = next[j];
            next[j] = temp;
            prev[next[i]] = i;
            dist[i] = length(points[i], points[next[i]]);
            result += dist[i];
        }
        prev[next[0]] = 0;
        dist[0] = length(points[0], points[next[0]]);
        result += dist[0];
        return result;
    }

    /**
     * Reconstruct dist[] and prev[] using next[].
     */
    private static void reconstructTour() {
        for (int i = 0; i < nodeCount; i++) {
            prev[next[i]] = i;
            dist[i] = length(points[i], points[next[i]]);
        }
    }

    private static float kOpt(int k) {
        k--; // 2-Opt means we are swapping 2 edges once, and we swap edges incrementally
        float minDiff = 0;
        float diff = 0;
        float maxDist = 0;
        int selected = -1;
        for (int i = 0; i < nodeCount; i++) {
            if (dist[i] > maxDist && !tabu.contains(i)) {
                maxDist = dist[i];
                selected = i;
            }
        }

        assert selected != -1; // We are promised to get a node

        int[] bestNext = next.clone(); // The best solution we get in this k-Opt operation
        while (k-- > 0 && !tabu.contains(selected)) {
            tabu.push(selected);
            float minDist = maxDist;
            int candidate = -1;
            for (int i = 0; i < nodeCount; i++) {
                if (i != selected && i != next[selected] && i != prev[selected]
                        && !tabu.contains(candidate)
                        && minDist > length(points[i], points[selected])) {
                    candidate = i;
                    minDist = length(points[i], points[selected]);
                }
            }
            if (candidate == -1) {
                break;
            }
            int candidateNext = next[candidate];
            int selectedNext = next[selected];
            float newEdge2 = length(points[selectedNext], points[candidateNext]);
            diff += minDist + newEdge2 - maxDist - dist[candidate];
            next[selected] = candidate;
            dist[selected] = minDist;
            for (int j = candidate; j != selectedNext; j = prev[j]) {
                dist[j] = dist[prev[j]];
                next[j] = prev[j];
            }
            for (int j = selected; j != selectedNext; j = next[j]) {
                prev[next[j]] = j;
            }
            next[selectedNext] = candidateNext;
            prev[candidateNext] = selectedNext;
            dist[selectedNext] = newEdge2;
            selected = selectedNext;
            maxDist = newEdge2;
            if (diff < minDiff) {
                minDiff = diff;
                bestNext = next.clone();
            }
        }

        next = bestNext.clone();
        reconstructTour();
        return minDiff;
    }

    private static boolean check() {
        int i = next[0];
        int ct = 1;
        while (i != 0) {
            i = next[i];
            ct++;
        }
        return ct == nodeCount;
    }

    private static float calcValue() {
        float v = 0;
        for (int i = 0; i < nodeCount; i++) {
            v += length(points[i], points[next[i]]);
        }
        return v;
    }

    private static void search() {
        int tryCount = 0;
        int tryLimit = 2000;
        minValue = shuffle();
        Tabu bestTabu = new Tabu(10); // Todo: check if tabu is stopping us from finding better solution
        int[] bestNext = next.clone();
        while (tryCount < tryLimit) {
            int tabuSize = nodeCount / 10;
            tabu = new Tabu(tabuSize);
            float value = shuffle();
            int threshold = 50;
            int pressure = 0;
            while (pressure < threshold) {
                float diff = kOpt(3);
                value += diff;
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
            if (minValue > value) {
                minValue = value;
                bestNext = next.clone();
                bestTabu = tabu;
            }
            tryCount++;
        }

        for (int i = 0, j = 0; i < nodeCount; i++, j = bestNext[j]) {
            solution[i] = j;
        }

        // Todo: delete this output
        System.out.print("tabu: ");
        for (int node : bestTabu.tabuQueue) {
            System.out.print(node + " ");
        }
        System.out.println("");
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
        fileName = "./data/tsp_100_2";
        fileName = "./data/tmp.data";
        fileName = "./data/tsp_200_2";
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

        points = new float[nodeCount][2];
        for (int i = 0; i < nodeCount; i++) {
            String line = lines.get(i + 1);
            String[] parts = line.split("\\s+");
            points[i][0] = Float.parseFloat(parts[0]);
            points[i][1] = Float.parseFloat(parts[1]);
        }

        solution = new int[nodeCount];
        next = new int[nodeCount];
        prev = new int[nodeCount];
        dist = new float[nodeCount];

        search();

        // prepare the solution in the specified output format
        System.out.println(minValue + " 0");
        for(int i = 0; i < nodeCount; i++) {
            System.out.print(solution[i] + " ");
        }
        System.out.println("");

        BufferedWriter writer = new BufferedWriter(new FileWriter("solution"));
        try {
            writer.write(minValue + " 0");
            writer.newLine();
            for (int i = 0; i < nodeCount; i++) {
                writer.write(solution[i] + " ");
            }
            writer.newLine();
        }
        finally {
            writer.close();
        }
    }
}