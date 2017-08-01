package check_derivation;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Main {
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        BufferedReader in = new BufferedReader(new FileReader(new File("input.txt")));
        String header = in.readLine();
        String axioms = header.substring(0, header.indexOf("|-")).replaceAll("\\s", "");
        ArrayList<String> assumptionList = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(axioms, ",");
        while (st.hasMoreTokens()) {
            assumptionList.add(st.nextToken());
        }
//        String target = header.substring(header.indexOf("|-") + 2);
        ArrayList<String> proof = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            proof.add(line.replaceAll("\\s", ""));
        }

        System.out.println((System.currentTimeMillis() - start) + "ms, input read");

        List<String> result = new Validator(assumptionList, proof).validate();
        PrintWriter out = new PrintWriter(new File("output.txt"));
        result.forEach(out::println);
        out.close();
        System.out.println((System.currentTimeMillis() - start) + "ms total, done");
    }
}
