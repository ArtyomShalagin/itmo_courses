package predicate_calculus;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CheckResult {
    protected boolean passed;
    protected ResultType resultType;

    public boolean isPassed() {
        return passed;
    }

    @Override
    public String toString() {
        return resultType + " " + passed;
    }

    enum ResultType {
        ASSUMPTION, AXIOM, RULE
    }

    public static class CheckAssumptionResult extends CheckResult {
        private final int assumptionIndex;

        public static final CheckAssumptionResult FAILED = new CheckAssumptionResult(false, -1);

        public CheckAssumptionResult(boolean passed, int assumptionIndex) {
            resultType = ResultType.ASSUMPTION;
            this.passed = passed;
            this.assumptionIndex = assumptionIndex;
        }

        public int getAssumptionIndex() {
            return assumptionIndex;
        }

        @Override
        public String toString() {
            return "assumption " + assumptionIndex;
        }
    }

    public static class CheckAxiomResult extends CheckResult implements ResultWithErrorMessage {
        private final int standardAxiomIndex;
        private final String errorMessage;
        public final AxiomType axiomType;

        public static final CheckAxiomResult FAILED_STANDARD =
                new CheckAxiomResult(false, AxiomType.STANDARD, -1, null);
        public static final CheckAxiomResult FAILED_FORALL =
                new CheckAxiomResult(false, AxiomType.FORALL, -1, null);
        public static final CheckAxiomResult FAILED_EXISTS =
                new CheckAxiomResult(false, AxiomType.EXISTS, -1, null);
        public static final CheckAxiomResult FAILED_FORMAL =
                new CheckAxiomResult(false, AxiomType.FORMAL, -1, null);

        public static final CheckAxiomResult PASSED_FORALL =
                new CheckAxiomResult(true, AxiomType.FORALL, -1);
        public static final CheckAxiomResult PASSED_EXISTS =
                new CheckAxiomResult(true, AxiomType.EXISTS, -1);
        public static final CheckAxiomResult PASSED_FORMAL =
                new CheckAxiomResult(true, AxiomType.FORMAL, -1);

        private CheckAxiomResult(boolean passed, AxiomType axiomType, int standardAxiomIndex, String errorMessage) {
            resultType = ResultType.AXIOM;
            this.passed = passed;
            this.axiomType = axiomType;
            this.standardAxiomIndex = standardAxiomIndex;
            this.errorMessage = errorMessage;
        }

        public CheckAxiomResult(boolean passed, AxiomType axiomType, int standardAxiomIndex) {
            this(passed, axiomType, standardAxiomIndex, null);
        }

        public CheckAxiomResult(boolean passed, AxiomType axiomType, String errorMessage) {
            this(passed, axiomType, -1, errorMessage);
        }

        public int getStandardAxiomIndex() {
            return standardAxiomIndex;
        }

        @Override
        public boolean hasErrorMessage() {
            return errorMessage != null;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public CheckResult asCheckResult() {
            return this;
        }

        @Override
        public String toString() {
            return "axiom " + (axiomType == AxiomType.STANDARD ?
                    standardAxiomIndex : axiomType.toString().toLowerCase());
        }

        public enum AxiomType {
            STANDARD, FORALL, EXISTS, FORMAL
        }
    }

    public static class CheckRuleResult extends CheckResult implements ResultWithErrorMessage {
        private final String errorMessage;
        private final List<Integer> sourcePosition;
        public final RuleType ruleType;

        public static final CheckRuleResult FAILED_FORALL =
                new CheckRuleResult(false, RuleType.FORALL, null);
        public static final CheckRuleResult FAILED_EXISTS =
                new CheckRuleResult(false, RuleType.EXISTS, null);
        public static final CheckRuleResult FAILED_MP =
                new CheckRuleResult(false, RuleType.MP, null);

        public CheckRuleResult(boolean passed, RuleType ruleType, String errorMessage, Integer... sourcePosition) {
            resultType = ResultType.RULE;
            this.passed = passed;
            this.ruleType = ruleType;
            this.errorMessage = errorMessage;
            this.sourcePosition = Arrays.asList(sourcePosition);

            if (passed) {
                if (ruleType == RuleType.MP) {
                    if (sourcePosition.length != 2) {
                        throw new IllegalArgumentException(
                                "CheckRuleResult with Modus Ponens rule must have 2 source positions");
                    }
                } else {
                    if (sourcePosition.length != 1) {
                        throw new IllegalArgumentException(
                                "CheckRuleResult with " + ruleType + " rule must have 1 source position");
                    }
                }
            }
        }

        @Override
        public boolean hasErrorMessage() {
            return errorMessage != null;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public CheckResult asCheckResult() {
            return this;
        }

        public List<Integer> getSourcePosition() {
            return sourcePosition;
        }

        @Override
        public String toString() {
            return "rule " + ruleType + " (" +
                    sourcePosition.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", ")) + ")";
        }

        public enum RuleType {
            FORALL, EXISTS, MP
        }
    }

    public interface ResultWithErrorMessage {
        boolean hasErrorMessage();
        String getErrorMessage();
        CheckResult asCheckResult();
    }
}
