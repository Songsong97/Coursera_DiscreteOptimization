import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

public class Solver {
    static int maxValue = 0;
    static int items;
    static int capacity;
    static int[] values;
    static int[] weights;
    static int[] taken;
    static int[] curTaken;

    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Dynamic Programming
     */
    public static void solveDP() {
        int[][] dp = new int[items][capacity + 1];
        taken = new int[items];

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

        // prepare the solution in the specified output format
        System.out.println(dp[items - 1][capacity]+" 1");
        for(int i=0; i < items; i++){
            System.out.print(taken[i]+" ");
        }
        System.out.println("");
    }

    public static void solve2() {
        int[][] temp = new int[items][3];
        for (int i = 0; i < items; i++) {
            temp[i][0] = values[i];
            temp[i][1] = weights[i];
            temp[i][2] = i;
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
        int greedySpace = capacity;
        float bounding = 0;
        for (int i = 0; i < items; i++) {
            if (weights[i] <= greedySpace) {
                greedySpace -= weights[i];
                bounding += values[i];
            }
            else {
                bounding += greedySpace / (float)weights[i] * (float)values[i];
                break;
            }
        }
        helper(0, 0, capacity, bounding);
        int[] initTaken = new int[items];
        for (int i = 0; i < items; i++) {
            initTaken[temp[i][2]] = taken[i];
        }

        // prepare the solution in the specified output format
        System.out.println(maxValue+" 1");
        for(int i=0; i < items; i++){
            System.out.print(initTaken[i]+" ");
        }
        System.out.println("");
    }

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
                taken = curTaken.clone();
            }
            return;
        }
        curTaken[i] = 1;
        helper(i + 1, value + values[i], room - weights[i], bounding);
        curTaken[i] = 0;
        bounding = value;
        int greedySpace = room;
        for (int j = i + 1; j < items; j++) {
            if (weights[j] <= greedySpace) {
                greedySpace -= weights[j];
                bounding += values[j];
            }
            else {
                bounding += greedySpace / (float)weights[j] * (float)values[j];
                break;
            }
        }
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

        for(int i=1; i < items+1; i++){
            String line = lines.get(i);
            String[] parts = line.split("\\s+");

            values[i-1] = Integer.parseInt(parts[0]);
            weights[i-1] = Integer.parseInt(parts[1]);
        }

        solve2();
    }
}