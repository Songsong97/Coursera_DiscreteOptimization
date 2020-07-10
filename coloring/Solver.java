import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.lang.Math;

class Node {
    boolean[] colorSet;
    int id;
    int color;
    int degree;
    int occupy = 0;
    Node(int size, int id, int degree) {
        colorSet = new boolean[size];
        color = -1;
        this.id = id;
        this.degree = degree;
    }
}

class MyPriorityQueue {
    Node[] data;
    int[] indexInData;
    int size;
    private static MyPriorityQueue singleton = null;
    private MyPriorityQueue(ArrayList<ArrayList<Integer>> edges) {
        size = edges.size();
        data = new Node[size];
        indexInData = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = new Node(data.length, i, edges.get(i).size());
            indexInData[i] = i;
        }
        for (int i = data.length / 2 - 1; i >= 0; i--) {
            maxHeapify(i);
        }
    }
    public static MyPriorityQueue createMyQueue(ArrayList<ArrayList<Integer>> edges) {
        if (singleton == null) {
            singleton = new MyPriorityQueue(edges);
        }
        return singleton;
    }
    private static int parent(int i) {
        return (i - 1) / 2;
    }
    private static int left(int i) {
        return 2 * i + 1;
    }
    private static int right(int i) {
        return 2 * i + 2;
    }
    private static int comp(Node n1, Node n2) {
        if (n1.degree != n2.degree) {
            return n1.degree - n2.degree;
        }
        if (n1.occupy != n2.occupy) {
            return n1.occupy - n2.occupy;
        }
        for (int i = 0; i < n1.colorSet.length; i++) {
            if (n1.colorSet[i] != n2.colorSet[i]) {
                return n1.colorSet[i] ? 1 : -1;
            }
        }
        return 0;
    }
    private void maxHeapify(int i) {
        int l = left(i);
        int r = right(i);
        int largest;
        if (l < size && comp(data[l], data[i]) > 0) {
            largest = l;
        }
        else {
            largest = i;
        }
        if (r < size && comp(data[r], data[largest]) > 0) {
            largest = r;
        }
        if (largest != i) {
            indexInData[data[i].id] = largest;
            indexInData[data[largest].id] = i;
            Node temp = data[i];
            data[i] = data[largest];
            data[largest] = temp;
            maxHeapify(largest);
        }
    }
    Node extractMax() {
        if (size < 1) {
            System.out.println("heap underflow");
        }
        Node max = data[0];
        indexInData[data[size - 1].id] = 0;
        indexInData[data[0].id] = -1;
        data[0] = data[size - 1];
        data[size - 1] = null;
        size--;
        maxHeapify(0);
        return max;
    }
    void disableColor(int nodeID, int c) {
        int i = indexInData[nodeID];
        data[i].colorSet[c] = true;
        data[i].occupy++;
        while (i > 0 && comp(data[parent(i)], data[i]) < 0) {
            int p = parent(i);
            indexInData[data[i].id] = p;
            indexInData[data[p].id] = i;
            Node temp = data[i];
            data[i] = data[p];
            data[p] = temp;
            i = p;
        }
    }
    void enableColor(int nodeID, int c) {
        int i = indexInData[nodeID];
        data[i].colorSet[c] = false;
        data[i].occupy--;
        maxHeapify(i);
    }
    void insert(Node node) {
        data[size] = node;
        indexInData[node.id] = size;
        int i = size;
        size++;
        while (i > 0 && comp(data[parent(i)], data[i]) < 0) {
            int p = parent(i);
            indexInData[data[i].id] = p;
            indexInData[data[p].id] = i;
            Node temp = data[i];
            data[i] = data[p];
            data[p] = temp;
            i = p;
        }
    }
}

public class Solver {
    static int nodeCount;
    static int edgeCount;
    static int[] solution;
    static int minValue;

    static MyPriorityQueue queue;
    static Stack<Node> stack;

    static ArrayList<ArrayList<Integer>> edges;

    public static void main(String[] args) {
        try {
            solve(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void solveNaive() {
        int state = 0; // 0 means we are searching down, 1 means we are back tracking
        Stack<Integer> bounding = new Stack<>();
        Stack<ArrayList<Integer>> changedStack = new Stack<>();
        bounding.push(-1);
        int limit = 0;
        int ct = 0;
        for (int i = 0; i >= 0; ) {
            ct++;
            if (limit > 0 && ct > limit) {
                return;
            }
            if (i == nodeCount) {
                if (bounding.peek() + 1 < minValue) {
                    minValue = bounding.peek() + 1;
                    for (Node n : stack) {
                        solution[n.id] = n.color;
                    }
                    limit = Math.max(8 * ct, 100000);
                    ct = 0;
                }
                i--;
                state = 1;
            }
            else if (state == 0) {
                Node node = queue.extractMax();
                int j;
                for (j = 0; j <= i; j++) {
                    if (!node.colorSet[j]) {
                        if (j + 1 >= minValue) { // no need to search this branch
                            j = i + 1;
                            break;
                        }
                        node.colorSet[j] = true;
                        node.color = j;
                        ArrayList<Integer> changed = new ArrayList<>();
                        for (int k = 0; k < edges.get(node.id).size(); k++) {
                            int neighbor = edges.get(node.id).get(k);
                            int indexNeighbor = queue.indexInData[neighbor];
                            if (indexNeighbor != -1 && !queue.data[indexNeighbor].colorSet[j]) {
                                queue.disableColor(neighbor, j);
                                changed.add(neighbor);
                            }
                        }
                        bounding.push(Math.max(bounding.peek(), j));
                        changedStack.push(changed);
                        stack.push(node);
                        break;
                    }
                }
                if (j == i + 1) {
                    state = 1;
                    i--;
                    queue.insert(node);
                }
                else {
                    i++;
                }
            }
            else {
                Node node = stack.pop();
                bounding.pop();
                ArrayList<Integer> restore = changedStack.pop();
                for (int restoreID : restore) {
                    queue.enableColor(restoreID, node.color);
                }
                node.colorSet[node.color] = false;
                int j = node.color + 1;
                node.color = -1;
                for (; j <= i; j++) {
                    if (!node.colorSet[j]) {
                        if (j + 1 >= minValue) { // no need to search this branch
                            j = i + 1;
                            break;
                        }
                        node.colorSet[j] = true;
                        node.color = j;
                        ArrayList<Integer> changed = new ArrayList<>();
                        for (int k = 0; k < edges.get(node.id).size(); k++) {
                            int neighbor = edges.get(node.id).get(k);
                            int indexNeighbor = queue.indexInData[neighbor];
                            if (indexNeighbor != -1 && !queue.data[indexNeighbor].colorSet[j]) {
                                queue.disableColor(neighbor, j);
                                changed.add(neighbor);
                            }
                        }
                        bounding.push(Math.max(bounding.peek(), j));
                        changedStack.push(changed);
                        stack.push(node);
                        break;
                    }
                }
                if (j == i + 1) { // back tracking
                    i--;
                    queue.insert(node);
                }
                else {
                    i++;
                    state = 0;
                }
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
//        fileName = "tmp.data"; // todo: to be deleted
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
        queue = MyPriorityQueue.createMyQueue(edges);
        stack = new Stack<>();
        minValue = nodeCount;
        solution = new int[nodeCount];

        solveNaive();

        // prepare the solution in the specified output format
        System.out.println(minValue + " 0");
        for(int i = 0; i < nodeCount; i++) {
            System.out.print(solution[i] + " ");
        }
        System.out.println("");
    }
}