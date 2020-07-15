import java.io.*;
import java.util.*;
import java.lang.Math;

public class Solver {
    static int items;
    static int capacity;
    static int[] values;
    static int[] weights;

    static int[] taken;
    static int[] curTaken;
    static int[] maxTaken;

    static int maxValue = 0;
    static Stack<Float> boundingStack = new Stack<>();
    static Stack<Integer> branchStack = new Stack<>();

    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Dynamic Programming, which is likely to deplete the memory
     */
    public static void solveDP() {
        int[][] dp = new int[items][capacity + 1];

        for (int i = 0; i < items; i++) {
            for (int j = 0; j <= capacity; j++) {
                if (i == 0) {
                    dp[i][j] = weights[i] <= j ? values[i] : 0;
                }
                else if (weights[i] <= j) {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i - 1][j - weights[i]] + values[i]);
                }
                else {
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        for (int i = items - 1, k = capacity; i >= 0; i--) {
            if (i == 0) {
                taken[i] = dp[i][k] == 0 ? 0 : 1;
            }
            else if (dp[i][k] == dp[i - 1][k]) {
                taken[i] = 0;
            }
            else {
                k -= weights[i];
                taken[i] = 1;
            }
        }

        maxValue = dp[items - 1][capacity];
    }

    /**
     *  Solve the problem with large input size
     */
    public static void solveLarge() {
        // Sort the array in descending order of value per weight
        int[][] temp = new int[items][3];
        for (int i = 0; i < items; i++) {
            temp[i][0] = values[i];
            temp[i][1] = weights[i];
            temp[i][2] = i; // The original index of this item
        }
        Arrays.sort(temp, new Comparator<int[]>() {
            @Override
            public int compare(int[] o1, int[] o2) {
                float t = (float)o2[0] / (float)o2[1] - (float)o1[0] / (float)o1[1];
                return t == 0 ? 0 : (t > 0 ? 1 : -1);
            }
        });
        for (int i = 0; i < items; i++) {
            values[i] = temp[i][0];
            weights[i] = temp[i][1];
        }

        curTaken = new int[items];
        maxTaken = new int[items];

        helper();

        // these 2 lines use recursive version of searching
//        float bounding = suffixBounding(0, capacity);
//        helper(0, 0, capacity, bounding);

        for (int i = 0; i < items; i++) {
            taken[temp[i][2]] = maxTaken[i];
        }
    }

    /**
     * Compute the maximum value that we can get using room space, and only items with index >= i
     */
    public static float suffixBounding(int i, int room) {
        float bounding = 0;
        while (i < items) {
            if (weights[i] <= room) {
                room -= weights[i];
                bounding += values[i];
            }
            else {
                bounding += room / (float)weights[i] * (float)values[i];
                break;
            }
            i++;
        }
        return bounding;
    }

    /**
     * Iterative version of the helper method, which better saves space.
     * This method does an exhaustive search and uses relaxation on the bounding.
     * The bounding is the maximum value we can get using the current selection and the "value per weight" paradigm.
     * We prune the search tree when current bounding <= max value we already got.
     */
    public static void helper() {
        int value = 0;
        int room = capacity;
        float bounding = suffixBounding(0, capacity);
        int state = 0; // 0 means we are going down, 1 means going up

        for (int i = 0; i >= 0; ) {
            if (state == 0 && (room < 0 || bounding <= maxValue)) { // prune invalid or useless search tree
                i--;
                state = 1;
            }
            else if (i == items) { // candidate of the result
                if (maxValue < value) {
                    maxValue = value;
                    maxTaken = curTaken.clone();
                }
                i--;
                state = 1;
            }
            else if (state == 0){ // come to a node from its parent
                branchStack.push(0); // 0 means we have considered the case where item i is chosen (left child)
                curTaken[i] = 1;
                value += values[i];
                room -= weights[i];
                i++;
            }
            else { // come to a node from its children
                // pop stack and decide whether go to right child or back track
                if (branchStack.pop() == 0) {
                    curTaken[i] = 0;
                    value -= values[i];
                    room += weights[i];
                    boundingStack.push(bounding);
                    branchStack.push(1); // Go to right child node
                    bounding = value + suffixBounding(i + 1, room);
                    state = 0;
                    i++;
                }
                else {
                    bounding = boundingStack.pop();
                    i--;
                }
            }
        }
    }

    /**
     * Recursive version of the helper method, which will cause stack overflow when input size is large
     */
    @Deprecated
    public static void helper(int i, int value, int room, float bounding) {
        if (bounding <= maxValue) {
            return;
        }
        if (room < 0) {
            return;
        }
        if (i == items) {
            if (maxValue < value) {
                maxValue = value;
                maxTaken = curTaken.clone();
            }
            return;
        }
        curTaken[i] = 1;
        helper(i + 1, value + values[i], room - weights[i], bounding);
        curTaken[i] = 0;
        bounding = value + suffixBounding(i + 1, room);
        helper(i + 1, value, room, bounding);
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
        items = Integer.parseInt(firstLine[0]);
        capacity = Integer.parseInt(firstLine[1]);

        values = new int[items];
        weights = new int[items];
        taken = new int[items];

        for(int i=1; i < items+1; i++){
            String line = lines.get(i);
            String[] parts = line.split("\\s+");

            values[i-1] = Integer.parseInt(parts[0]);
            weights[i-1] = Integer.parseInt(parts[1]);
        }

        solveLarge();

        // the line below use dynamic programming to find the solution
//        solveDP();

        // prepare the solution in the specified output format
        System.out.println(maxValue + " 1");
        for(int i = 0; i < items; i++) {
            System.out.print(taken[i] + " ");
        }
        System.out.println("");
    }
}