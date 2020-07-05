import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

/**
 * The class <code>Solver</code> is an implementation of a greedy algorithm to solve the knapsack problem.
 *
 */
public class Solver {
    
    /**
     * The main class
     */
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
        int items = Integer.parseInt(firstLine[0]);
        int capacity = Integer.parseInt(firstLine[1]);

        int[] values = new int[items];
        int[] weights = new int[items];

        for(int i=1; i < items+1; i++){
            String line = lines.get(i);
            String[] parts = line.split("\\s+");

            values[i-1] = Integer.parseInt(parts[0]);
            weights[i-1] = Integer.parseInt(parts[1]);
        }

        // Dynamic Programming
        int[][] dp = new int[items][capacity + 1];
        int[] taken = new int[items];
        int value = 0;

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

        value = dp[items - 1][capacity];

        // prepare the solution in the specified output format
        System.out.println(value+" 1");
        for(int i=0; i < items; i++){
            System.out.print(taken[i]+" ");
        }
        System.out.println("");
    }
}