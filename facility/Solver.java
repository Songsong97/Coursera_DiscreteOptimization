import java.io.*;
import java.util.*;
import java.lang.Math;


public class Solver {
    private static int facilityCount;
    private static int customerCount;
    private static float minValue;
    private static int[] solution;

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
        facilityCount = Integer.parseInt(firstLine[0]);
        customerCount = Integer.parseInt(firstLine[1]);

        for (int i = 0; i < facilityCount; i++) {
            String line = lines.get(i + 1);
            String[] parts = line.split("\\s+");
            float setupCost = Float.parseFloat(parts[0]);
            int capacity = Integer.parseInt(parts[1]);
            float posX = Float.parseFloat(parts[2]);
            float posY = Float.parseFloat(parts[3]);
        }
        for (int i = 0; i < customerCount; i++) {
            String line = lines.get(i + 1 + facilityCount);
            String[] parts = line.split("\\s+");
            int demand = Integer.parseInt(parts[0]);
            float posX = Float.parseFloat(parts[1]);
            float posY = Float.parseFloat(parts[2]);
        }

        solution = new int[customerCount];


        // prepare the solution in the specified output format
        System.out.println(minValue + " 0");
        for(int i = 0; i < customerCount; i++) {
            System.out.print(solution[i] + " ");
        }
        System.out.println("");
    }
}