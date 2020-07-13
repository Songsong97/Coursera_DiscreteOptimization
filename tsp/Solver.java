import java.io.*;
import java.util.*;

public class Solver {
    static int nodeCount;
    static float[][] points;
    static float minValue;
    static int[] solution;

    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static float length(float[] a, float[] b) {
        float v1 = a[0] - b[0];
        float v2 = a[1] - b[1];
        return (float)Math.sqrt(v1 * v1 + v2 * v2);
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

        points = new float[nodeCount][2];
        for (int i = 0; i < nodeCount; i++) {
            String line = lines.get(i + 1);
            String[] parts = line.split("\\s+");
            points[i][0] = Float.parseFloat(parts[0]);
            points[i][1] = Float.parseFloat(parts[1]);
        }

        solution = new int[nodeCount];
        minValue = 0;
        for (int i = 0; i < nodeCount; i++) {
            solution[i] = i;
            minValue += length(points[i], points[(i + 1) % nodeCount]);
        }

        // prepare the solution in the specified output format
        System.out.println(minValue + " 0");
        for(int i = 0; i < nodeCount; i++) {
            System.out.print(solution[i] + " ");
        }
        System.out.println("");
    }
}