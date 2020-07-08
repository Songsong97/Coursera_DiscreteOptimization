import java.io.*;
import java.util.*;
import java.lang.Math;

public class Solver {

    static int nodeCount;
    static int edgeCount;
    static int[] solution;
    static int minValue;

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

        ArrayList<ArrayList<Integer>> edges = new ArrayList<>();
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

        solution = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            solution[i] = i;
        }

        // prepare the solution in the specified output format
        System.out.println(minValue + " 1");
        for(int i = 0; i < nodeCount; i++) {
            System.out.print(solution[i] + " ");
        }
        System.out.println("");
    }
}