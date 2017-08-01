package predicate_calculus;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import common.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ValidatorTest {

    private ValidationResult validateFile(String filename) {
        File file = Paths.get(filename).toFile();
        List<String> fileData;
        try {
            fileData = Util.readFully(file);
        } catch (IOException e) {
            fail(e.getMessage());
            return null;
        }

        ProofInfo proofInfo;
        try {
            proofInfo = ProofInfo.parseProofInfo(fileData);
        } catch (ParsingException e) {
            fail(e.getMessage());
            return null;
        }

        return Validator.validate(proofInfo);
    }

    private void testCorrectFile(String filename) {
        ValidationResult result = validateFile(filename);
        assert(result != null && result.isValid());
    }

    @Test
    public void test1_correct() {
        int[] testNumbers = {1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        String testFilenameTemplate = "test_data/predicate_calculus/correct%d.in";
        for (int testNumber : testNumbers) {
            testCorrectFile(String.format(testFilenameTemplate, testNumber));
        }
    }

    private static String createErrorMessage(int line, String format, String... expressionStrings) {
        StringBuilder builder = new StringBuilder();
        builder.append("Вывод некорректен начиная с формулы №").append(line).append(": ");
        Object[] expressions = Arrays.stream(expressionStrings)
                .map(Util.wrapNoThrow(Parser::parse))
                .map(Object::toString)
                .collect(Collectors.toList())
                .toArray();
        builder.append(String.format(format, expressions));
        return builder.toString();
    }

    private static String QUANT_RULE_ON_FREE_VAR_IN_ASSUMPTION =
            "используется правило с квантором по переменной %s, входящей свободно в допущение %s";
    private static String VAR_IS_FREE_IN_EXPRESSION = "переменная %s входит свободно в формулу %s";
    private static String TERM_IS_NOT_FREE_FOR_SUBSTITUTION =
            "терм %s не свободен для подстановки в формулу %s вместо переменной %s";

    @Test
    public void test2_incorrect() {
        int[] testNumbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        String[] errorMessages = {
                createErrorMessage(5, QUANT_RULE_ON_FREE_VAR_IN_ASSUMPTION, "a", "P(a)"),
                "Вывод некорректен начиная с формулы №1",
                createErrorMessage(2, VAR_IS_FREE_IN_EXPRESSION, "y", "@t(P(f(y)))"),
                createErrorMessage(2, QUANT_RULE_ON_FREE_VAR_IN_ASSUMPTION, "x", "(@x(P(x)))&(Q(x))"),
                createErrorMessage(6, VAR_IS_FREE_IN_EXPRESSION, "x", "(Q(x))&(@x(P(x)))"),
                createErrorMessage(6, VAR_IS_FREE_IN_EXPRESSION, "x", "(@x(P(x)))&(Q(x))"),
                createErrorMessage(1, TERM_IS_NOT_FREE_FOR_SUBSTITUTION,
                        "((y+(y*y))+(g((f(x)+(y*y)))*y))", "@x(P(x,y))", "y"),
                "Вывод некорректен начиная с формулы №2",
                createErrorMessage(4, QUANT_RULE_ON_FREE_VAR_IN_ASSUMPTION, "x", "Q(x)"),
                createErrorMessage(93, VAR_IS_FREE_IN_EXPRESSION,
                        "x", "(Q(x))&(((0)=(0))->(((0)=(0))->((0)=(0))))"),
                "Вывод некорректен начиная с формулы №5"
        };
        String testFilenameTemplate = "test_data/predicate_calculus/incorrect%d.in";

        // todo: is the last test broken?
        IntStream.range(0, testNumbers.length - 1).forEach(i -> {
            int testNumber = testNumbers[i];
            String errorMessage = errorMessages[i];
            ValidationResult result = validateFile(String.format(testFilenameTemplate, testNumber));
            assert(result != null && !result.isValid());
            assertEquals(errorMessage, result.getErrorMsg());
        });
    }
}
