
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Created by takirala on 11/6/2016.
 */
public class antifraud {

    public static void main(String[] args) throws Exception {

        if (args == null || args.length != 5)
            throw new Exception("INVALID_ARGS");

        final String batchPayment = args[0];
        final String streamPayment = args[1];

        if (!Files.exists(Paths.get(batchPayment)))
            throw new Exception("INVALID_BATCH_PAYMENT_FILE");
        if (!Files.exists(Paths.get(streamPayment)))
            throw new Exception("INVALID_STREAM_PAYMENT_FILE");

        final String output_1 = args[2];
        final String output_2 = args[3];
        final String output_3 = args[4];

        HashMap<Integer, HashSet<Integer>> initialState = new HashMap<>();
        buildInitialState(batchPayment, initialState);

        writeOutput(initialState,
                streamPayment,
                output_1,
                output_2,
                output_3);
    }

    private static void writeOutput(
            HashMap<Integer, HashSet<Integer>> initialState,
            String streamPayment,
            String output_1,
            String output_2,
            String output_3)
            throws Exception {

        final String UNVERIFIED = "unverified";
        final String TRUSTED = "trusted";

        BufferedWriter feature1 = null;
        BufferedWriter feature2 = null;
        BufferedWriter feature3 = null;
        try {
            feature1 = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(output_1), "utf-8"));
            feature2 = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(output_2), "utf-8"));
            feature3 = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(output_3), "utf-8"));

            try (BufferedReader br = new BufferedReader(
                    new FileReader(streamPayment))) {
                //Ignore the header line.
                br.readLine();

                for (String line; (line = br.readLine()) != null; ) {
                    String transaction[] = line.split(",");

                    Integer from = null;
                    Integer to = null;
                    try {
                        from = Integer.parseInt(transaction[1].trim());
                        to = Integer.parseInt(transaction[2].trim());
                    } catch (NumberFormatException ignore) {
                        continue;
                    }
                    long t1 = System.currentTimeMillis();
                    int distance = findRelation(initialState, from, to, 0);
                    if (from == to)
                        throw new Exception("INVALID_TRANSACTION");
                    if (distance == 1) {
                        feature1.write(TRUSTED);
                        feature2.write(TRUSTED);
                        feature3.write(TRUSTED);
                    } else if (distance == 2) {
                        feature1.write(UNVERIFIED);
                        feature2.write(TRUSTED);
                        feature3.write(TRUSTED);
                    } else if (distance <= 4) {
                        feature1.write(UNVERIFIED);
                        feature2.write(UNVERIFIED);
                        feature3.write(TRUSTED);
                    } else {
                        feature1.write(UNVERIFIED);
                        feature2.write(UNVERIFIED);
                        feature3.write(UNVERIFIED);
                    }
                    feature1.newLine();
                    feature2.newLine();
                    feature3.newLine();
                }
            }

        } finally {
            if (feature1 != null) feature1.close();
            if (feature2 != null) feature2.close();
            if (feature3 != null) feature3.close();
        }
    }

    private static void buildInitialState(
            String batchPayment,
            HashMap<Integer, HashSet<Integer>> initialState)
            throws IOException {

        try (BufferedReader br = new BufferedReader(
                new FileReader(batchPayment))) {
            //Ignore the header line.
            br.readLine();

            for (String line; (line = br.readLine()) != null; ) {
                String transaction[] = line.split(",");
                if (transaction.length < 5) continue;
                Integer from = null;
                Integer to = null;
                try {
                    from = Integer.parseInt(transaction[1].trim());
                    to = Integer.parseInt(transaction[2].trim());
                } catch (NumberFormatException ignore) {
                    continue;
                }

                addEdge(from, to, initialState);
                addEdge(to, from, initialState);
            }
        }
    }

    static int findRelation(HashMap<Integer, HashSet<Integer>> stateMap,
                            Integer from,
                            Integer to,
                            int depth) {
        depth++;
        if (!stateMap.containsKey(from)) return Integer.MAX_VALUE;
        // We can maintain a list of nodes visited so far in order to
        // prevent visiting them again. This is just an optimization
        // but will not change any business logic.


        // Optimistic bfs with dfs
        HashSet<Integer> neighbors = stateMap.get(from);
        if (neighbors.contains(to)) return depth;

        for (Integer neighbor : neighbors) {
            if (depth < 4) {
                int reach = findRelation(stateMap, neighbor, to, depth);
                if (reach <= 4) return reach;
            }
        }
        return Integer.MAX_VALUE;
    }

    static void addEdge(Integer from, Integer to,
                        HashMap<Integer, HashSet<Integer>> stateMap) {
        HashSet<Integer> prev = stateMap.get(from);
        if (prev == null) {
            prev = new HashSet<>();
            prev.add(to);
            stateMap.put(from, prev);
        } else prev.add(to);
    }
}