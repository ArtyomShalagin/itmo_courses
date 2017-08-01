package predicate_calculus;

import common.Lists;
import common.Pair;
import common.Util;
import predicate_calculus.CheckResult.CheckAssumptionResult;
import predicate_calculus.CheckResult.CheckAxiomResult;
import predicate_calculus.CheckResult.CheckRuleResult;
import predicate_calculus.expression.AbstractExpression;
import predicate_calculus.expression.BinExpression;
import predicate_calculus.expression.Quantifier;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static common.Util.cleanLine;
import static common.Util.logNoNewline;

public class Deductor {
    private final List<String> result = new ArrayList<>();
    private final ProofInfo proofInfo;
    private final AbstractExpression assumption;
    private final List<Pair<AbstractExpression, CheckResult>> checkResultList;

    private static List<String> aToALemma;
    private static List<String> forallLemma;
    private static List<String> existsLemma;

    private static final String PROOFS_DIR = "proofs/predicate_calculus/";

    static {
        try {
            aToALemma = Util.readFully(Paths.get(PROOFS_DIR, "a_to_a_lemma.txt").toFile());
            forallLemma = Util.readFully(Paths.get(PROOFS_DIR, "forall_lemma.txt").toFile());
            existsLemma = Util.readFully(Paths.get(PROOFS_DIR, "exists_lemma.txt").toFile());
        } catch (IOException e) {
            System.err.println("Unable to initialize lemmas in deductor: " + e.getMessage());
            aToALemma = forallLemma = existsLemma = Collections.emptyList();
        }
        replaceReadable(forallLemma);
        replaceReadable(existsLemma);
    }

    private Deductor(ProofInfo proofInfo, List<Pair<AbstractExpression, CheckResult>> checkResultList) {
        this.proofInfo = proofInfo;
        this.assumption = Lists.get(proofInfo.assumptions, -1);
        this.checkResultList = checkResultList;
    }

    public static List<String> deduct(ProofInfo proofInfo, List<Pair<AbstractExpression, CheckResult>> checkResult) {
        if (proofInfo.assumptions.size() == 0) {
            throw new IllegalArgumentException("Cannot apply deduction theorem when proof has no assumptions");
        }
        return new Deductor(proofInfo, checkResult).deduct();
    }

    private List<String> deduct() {
        result.clear();
        String newAssumptions = proofInfo.assumptions.stream()
                .limit(proofInfo.assumptions.size() - 1)
                .map(Object::toString)
                .collect(Collectors.joining(","));
        result.add(newAssumptions + "|-" + assumption + "->" + proofInfo.result);
        for (int i = 0; i < checkResultList.size(); i++) {
            Pair<AbstractExpression, CheckResult> pair = checkResultList.get(i);
            handleExpression(pair.first, pair.second);
            logNoNewline("\rDeducted " + (i + 1) + "/" + checkResultList.size());
        }
        cleanLine(("Deducted /" + checkResultList.size() * 2).length());

        return result;
    }

    private void handleExpression(AbstractExpression expr, CheckResult checkResult) {
        if (expr.equals(assumption)) {
            handleLastAssumption();
        } else if (checkResult instanceof CheckAxiomResult) {
            handleAxiomOrAssumption(expr);
        } else if (checkResult instanceof CheckAssumptionResult) {
            handleAxiomOrAssumption(expr);
        } else if (checkResult instanceof CheckRuleResult) {
            handleRule(expr, (CheckRuleResult) checkResult);
        }
    }

    private void handleLastAssumption() {
        aToALemma.forEach(s -> result.add(s.replace("A", assumption.toString())));
    }

    private void handleAxiomOrAssumption(AbstractExpression expr) {
        result.add(expr.toString());
        result.add(String.format("%s->(%s->%s)", expr, assumption, expr));
        result.add(String.format("%s->%s", assumption, expr));
    }

    private void handleRule(AbstractExpression expr, CheckRuleResult checkResult) {
        switch (checkResult.ruleType) {
            case FORALL:
                handleRuleForall(expr, checkResult);
                break;
            case EXISTS:
                handleRuleExists(expr, checkResult);
                break;
            case MP:
                handleRuleMp(expr, checkResult);
        }
    }

    private static String replaceReadable(String s) {
        s = s.replace("A", "$0");
        s = s.replace("B", "$1");
        s = s.replace("C", "$2");
        s = s.replace("x", "$3");
        return s;
    }

    private static void replaceReadable(List<String> data) {
        for (int i = 0; i < data.size(); i++) {
            data.set(i, replaceReadable(data.get(i)));
        }
    }

    private void handleRuleForall(AbstractExpression expr, CheckRuleResult checkResult) {
        forallLemma.forEach(s -> {
            BinExpression binExpr = (BinExpression) expr;
            Quantifier quant = (Quantifier) binExpr.getRight();
//                assumption, left from forall, right from forall, variable
            s = s.replace("$0", assumption.toString());
            s = s.replace("$1", binExpr.getLeft().toString());
            s = s.replace("$2", quant.getArg().toString());
            s = s.replace("$3", quant.getBinding().toString());
            result.add(s);
        });
    }

    private void handleRuleExists(AbstractExpression expr, CheckRuleResult checkResult) {
        existsLemma.forEach(s -> {
            BinExpression binExpr = (BinExpression) expr;
            Quantifier quant = (Quantifier) binExpr.getLeft();
//                assumption, left from forall, right from forall, variable
            s = s.replace("$0", assumption.toString());
            s = s.replace("$1", quant.getArg().toString());
            s = s.replace("$2", binExpr.getRight().toString());
            s = s.replace("$3", quant.getBinding().toString());
            result.add(s);
        });
    }

    private void handleRuleMp(AbstractExpression expr, CheckRuleResult checkResult) {
        AbstractExpression source = checkResultList.get(checkResult.getSourcePosition().get(0)).first;
        result.add(String.format("(%s->%s)->((%s->(%s->%s))->(%s->%s))",
                assumption, source, assumption, source, expr, assumption, expr));
        result.add(String.format("(%s->(%s->%s))->(%s->%s)",
                assumption, source, expr, assumption, expr));
        result.add(String.format("%s->%s", assumption, expr));
    }
}
