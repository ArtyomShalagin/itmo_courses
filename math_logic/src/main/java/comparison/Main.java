package comparison;

import common.Util;
import predicate_calculus.ParsingException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static List<String> common;
    private static List<String> lemmaParenthesis;
    private static List<String> lemmaForallNotToNotExists;
    private static List<String> proofLess;
    private static List<String> proofGreater;

    private static final String PROOFS_DIR = "proofs/comparison/";

    static {
        try {
            common = Util.readFully(Paths.get(PROOFS_DIR, "common.txt").toFile());
            lemmaParenthesis = Util.readFully(Paths.get(PROOFS_DIR, "lemma_parenthesis.txt").toFile());
            lemmaForallNotToNotExists = Util.readFully(Paths.get(PROOFS_DIR, "lemma_forall_not_to_not_exists.txt").toFile());
            proofLess = Util.readFully(Paths.get(PROOFS_DIR, "proof_less.txt").toFile());
            proofGreater = Util.readFully(Paths.get(PROOFS_DIR, "proof_greater.txt").toFile());
        } catch (IOException e) {
            System.err.println("Unable to initialize lemmas in comparison: " + e.getMessage());
            common = lemmaParenthesis = lemmaForallNotToNotExists = proofLess = proofGreater = Collections.emptyList();
        }
    }

    public static void main(String[] args) throws IOException, ParsingException {
        String inputFilename = "io/comparison/input.txt";
        String outputFilename = "io/comparison/output.txt";
        int a, b;
        try (Scanner in = new Scanner(new File(inputFilename))) {
            a = in.nextInt();
            b = in.nextInt();
        }
        List<String> data = a <= b ? proofLessOrEq(a, b) : proofGreater(a, b);
        try (PrintWriter out = new PrintWriter(outputFilename)) {
            data.forEach(out::println);
        }
    }

    static List<String> proofLessOrEq(int a, int b) {
        List<String> result = new ArrayList<>();
        int c = b - a;
        final String A = nStroke("0", a);
        final String B = nStroke("0", b);
        final String C = nStroke("0", c);
        result.add(String.format("|-?p(%s+p)=%s", A, B));
        result.addAll(common);
        result.add(String.format("@a(a+0=a)->%s+0=%s", A, A));
        result.add(String.format("%s+0=%s", A, A));
        List<String> proof = new ArrayList<>();
        proofLess.forEach(s -> proof.add(s.replace("A", A)));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < c; i++) {
            String curr = builder.toString();
            proof.forEach(s -> result.add(s.replace("$", curr)));
            builder.append("'");
        }
        result.add(String.format("(%s+%s)=%s->?p(%s+p)=%s", A, C, B, A, B));
        result.add(String.format("?p(%s+p)=%s", A, B));
        return result;
    }

    static List<String> proofGreater(int a, int b) {
        List<String> result = new ArrayList<>();
        int c = a - b;
        String C = nStroke("", c - 1);
        String B = nStroke("", b);
        result.add(String.format("|-!(?p(0%s'%s+p=0%s))", C, B, B));
        result.addAll(common);
        result.addAll(lemmaParenthesis);
        proofGreater.forEach(s -> result.add(s.replace("C", C).replace("B", B)));
        lemmaForallNotToNotExists.forEach(s -> result.add(s.replace("C", C).replace("B", B)));
        return result;
    }

    private static String nStroke(String name, int n) {
        return name + String.join("", Collections.nCopies(n, "'"));
    }
}
