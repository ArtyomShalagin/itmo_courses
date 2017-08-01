package comparison;

import common.Lists;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import predicate_calculus.ParsingException;
import predicate_calculus.ProofInfo;
import predicate_calculus.ValidationResult;
import predicate_calculus.Validator;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static comparison.Main.proofGreater;
import static comparison.Main.proofLessOrEq;
import static junit.framework.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ComparisonTest {

    private void test(int a, int b) {
        List<String> data = a <= b ? proofLessOrEq(a, b) : proofGreater(a, b);

        try {
            ProofInfo proofInfo = ProofInfo.parseProofInfo(data);
            ValidationResult validationResult = Validator.validate(proofInfo);
            if (validationResult.isValid()) {
                System.out.println(a + ", " + b + ": OK");
            } else {
                String badExpr = Lists.get(validationResult.getResultList(), -1).toString();
                fail(badExpr + "\n" + validationResult.getErrorMsg());
            }
        } catch (ParsingException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test1_random() {
        Random rnd = new Random("I'm writing unit tests instead of reading type theory books :(".hashCode());
        int upperBound = 100;
        int n = 5;
        List<Integer> as = IntStream.range(0, n)
                .boxed()
                .map(i -> rnd.nextInt(upperBound) + 1)
                .sorted()
                .collect(Collectors.toList());
        List<Integer> bs = IntStream.range(0, n)
                .boxed()
                .map(i -> rnd.nextInt(upperBound) + 1)
                .sorted()
                .collect(Collectors.toList());
        for (int a : as) {
            for (int b : bs) {
                test(a, b);
            }
        }
    }
}
