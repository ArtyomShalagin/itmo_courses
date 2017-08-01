package predicate_calculus;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import common.Util;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeductorTest {
    @Test
    public void test1_deduction() {
        String filenameTemplate = "test_data/predicate_calculus/correct%d.in";
        // tests 8 and 15 are not valid because of problems with free variables in assumptions
        int[] testNumbers = {1, 2, 5, 6, 7, 9, 10, 11, 12, 13, 14};
        try {
            for (int testNumber : testNumbers) {
                System.out.println("testing deduction, testNumber = " + testNumber);

                String filename = String.format(filenameTemplate, testNumber);
                List<String> fileData = Util.readFully(Paths.get(filename).toFile());
                ProofInfo proofInfo = ProofInfo.parseProofInfo(fileData);
                ValidationResult validationResult = Validator.validate(proofInfo);

                assert validationResult != null && validationResult.isValid();

                List<String> deducted = Deductor.deduct(proofInfo, validationResult.getResultList());
                ProofInfo deductedProofInfo = ProofInfo.parseProofInfo(deducted);
                ValidationResult deductedValidationResult = Validator.validate(deductedProofInfo);

                assert deductedValidationResult != null && deductedValidationResult.isValid();
            }
        } catch (IOException | ParsingException e) {
            fail(e.getMessage());
        }
    }
}
