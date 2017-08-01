package predicate_calculus;

import common.Lists;
import common.Timer;
import common.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;

import static common.Util.log;

public class Main {
    private static Timer timer = new Timer();

    public static void main(String[] args) throws IOException, ParsingException {
        String inputFilename = "io/predicate_calculus/input.txt";
        String outputFilename = "io/predicate_calculus/output.txt";

        File file = Paths.get(inputFilename).toFile();
        List<String> fileData = Util.readFully(file);

        timer.start();
        ProofInfo proofInfo = ProofInfo.parseProofInfo(fileData);
        log("Parsed in " + timer.time() + "ms");

        timer.start();
        ValidationResult validationResult = Validator.validate(proofInfo);
        log("Validated in " + timer.time() + "ms");
        if (!validationResult.isValid()) {
            log("Proof is invalid");
            log(Lists.get(validationResult.getResultList(), -1).first);
            log(validationResult.getErrorMsg());
        } else {
            log("Proof is valid");
            if (proofInfo.assumptions.isEmpty()) {
                try (PrintWriter out = new PrintWriter(outputFilename)) {
                    fileData.forEach(out::println);
                }
            } else {
                timer.start();
                List<String> deducted = Deductor.deduct(proofInfo, validationResult.getResultList());
                log("Deducted in " + timer.time() + "ms");

                try (PrintWriter out = new PrintWriter(outputFilename)) {
                    deducted.forEach(out::println);
                }
            }
        }
    }
}
