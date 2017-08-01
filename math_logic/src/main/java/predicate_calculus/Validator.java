package predicate_calculus;

import predicate_calculus.expression.*;
import predicate_calculus.CheckResult.CheckAxiomResult;
import predicate_calculus.CheckResult.CheckAxiomResult.AxiomType;
import predicate_calculus.CheckResult.CheckRuleResult;
import predicate_calculus.CheckResult.CheckRuleResult.RuleType;
import common.Pair;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static predicate_calculus.CheckResult.*;
import static common.Util.cleanLine;
import static common.Util.logNoNewline;

@SuppressWarnings("Duplicates")
public class Validator {
    private Map<AbstractExpression, List<Pair<AbstractExpression, Integer>>> mp = new HashMap<>();
    private List<AbstractExpression> proof;
    private Map<AbstractExpression, Integer> assumptions = new HashMap<>();
    private Map<AbstractExpression, Integer> proved = new HashMap<>();
    private Map<String, AbstractExpression> freeVariablesInAssumptions = new HashMap<>();

    private Validator(List<AbstractExpression> assumptions, List<AbstractExpression> proof) {
        this.proof = proof;
        IntStream.range(0, assumptions.size()).forEach(i -> {
            AbstractExpression assumption = assumptions.get(i);
            this.assumptions.putIfAbsent(assumption, i);
        });
        if (assumptions.size() != 0) {
            AbstractExpression lastAssumption = assumptions.get(assumptions.size() - 1);
                    lastAssumption.getFreeVariables().stream()
                            .map(Variable::getName)
                            .forEach(name -> freeVariablesInAssumptions.put(name, lastAssumption));
        }
    }

    public static ValidationResult validate(ProofInfo proofInfo) {
        return new Validator(proofInfo.assumptions, proofInfo.proof).doValidate();
    }

    private ValidationResult doValidate() {
        for (AbstractExpression expr : assumptions.keySet()) {
            proved.put(expr, 0);
        }
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        for (int i = 0; i < proof.size(); i++) {
            AbstractExpression expr = proof.get(i);

            CheckAssumptionResult assumptionResult = checkAssumption(expr);
            CheckAxiomResult axiomResult = checkAxiom(expr);
            CheckRuleResult rulesResult = checkRule(expr);
            if (assumptionResult.isPassed()) {
                result.addProved(expr, assumptionResult);
            } else if (axiomResult.isPassed() || rulesResult.isPassed()) {
                CheckResult passed = axiomResult.isPassed() ? axiomResult : rulesResult;
                result.addProved(expr, passed);
            } else {
                ResultWithErrorMessage failed = axiomResult;
                if (axiomResult.hasErrorMessage() || rulesResult.hasErrorMessage()) {
                    failed = axiomResult.hasErrorMessage() ? axiomResult : rulesResult;
                }
                result.addProved(expr, failed.asCheckResult());
                result.setValid(false);
                if (failed.hasErrorMessage()) {
                    result.setErrorMsg("Вывод некорректен начиная с формулы №" + (i + 1) + ": " + failed.getErrorMessage());
                } else {
                    result.setErrorMsg("Вывод некорректен начиная с формулы №" + (i + 1));
                }
                break;
            }
            acceptFormula(i);
            logNoNewline("\rValidated " + (i + 1) + "/" + proof.size());
        }
        cleanLine(("Validated /" + String.valueOf(proof.size()).length() * 2).length());
        return result;
    }

    private void putInMp(BinExpression expr, int index) {
        mp.putIfAbsent(expr.getRight(), new ArrayList<>());
        mp.get(expr.getRight()).add(new Pair<>(expr.getLeft(), index));
    }

    private void acceptFormula(int index) {
        AbstractExpression expr = proof.get(index);
        proved.put(expr, index);
        if (expr.op == Operator.IMPL) {
            putInMp((BinExpression) expr, index);
        }
    }

    private ResultWithErrorMessage findPassedOrFailed(List<ResultWithErrorMessage> results) {
        Optional<CheckResult> passed = results.stream()
                .map(ResultWithErrorMessage::asCheckResult)
                .filter(CheckResult::isPassed)
                .findAny();
        if (passed.isPresent()) {
            return (ResultWithErrorMessage) passed.get();
        } else {
            Optional<ResultWithErrorMessage> withSpecificError = results.stream()
                    .filter(ResultWithErrorMessage::hasErrorMessage)
                    .findAny();
            return withSpecificError.orElse(null);
        }
    }

    private CheckAssumptionResult checkAssumption(AbstractExpression expr) {
        return assumptions.containsKey(expr) ?
                new CheckAssumptionResult(true, assumptions.get(expr)) : CheckAssumptionResult.FAILED;
    }

    private CheckAxiomResult checkAxiom(AbstractExpression expr) {
        OptionalInt standardAxiomSchemaIndex = IntStream.range(0, Axioms.axiomSchemas.size())
                .filter(i -> recComp(Axioms.axiomSchemas.get(i), expr))
                .findAny();
        OptionalInt standardAxiomIndex = IntStream.range(0, Axioms.axioms.size())
                .filter(i -> recComp(Axioms.axioms.get(i), expr, Collections.emptySet(), true))
                .findAny();
        if (standardAxiomSchemaIndex.isPresent()) {
            return new CheckAxiomResult(true, AxiomType.STANDARD, standardAxiomSchemaIndex.getAsInt());
        } else if (standardAxiomIndex.isPresent()) {
            return new CheckAxiomResult(true, AxiomType.STANDARD, standardAxiomIndex.getAsInt());
        } else {
            List<ResultWithErrorMessage> results =
                    Arrays.asList(checkAxiomForall(expr), checkAxiomExists(expr), checkAxiomFormal(expr));
            ResultWithErrorMessage result = findPassedOrFailed(results);
            if (result == null) {
                return CheckAxiomResult.FAILED_STANDARD;
            }
            return (CheckAxiomResult) result;
        }
    }

    // checks @x(X)->X[x:=Y] where Y is free for substitution
    private CheckAxiomResult checkAxiomForall(AbstractExpression expr) {
        if (expr.op != Operator.IMPL) {
            return CheckAxiomResult.FAILED_FORALL;
        }
        BinExpression binExpr = (BinExpression) expr;
        if (binExpr.getLeft().op != Operator.FORALL) {
            return CheckAxiomResult.FAILED_FORALL;
        }
        Quantifier quantLeft = ((Quantifier) binExpr.getLeft());
        AbstractExpression left = quantLeft.getArg();
        AbstractExpression right = binExpr.getRight();
        Variable binding = quantLeft.getBinding();
        boolean freeForSubst = freeForSubst(left, right, binding);
        Pair<Boolean, Map<Integer, AbstractExpression>> comparisonResult =
                recCompWithMap(left, right, Collections.singleton(binding.getName()), true);
        boolean matches = comparisonResult.first;
        if (matches) {
            if (freeForSubst) {
                return CheckAxiomResult.PASSED_FORALL;
            } else {
                AbstractExpression replacement = comparisonResult.second.get(binding.getId());
                return new CheckAxiomResult(false, AxiomType.FORALL,
                        String.format("терм %s не свободен для подстановки в формулу %s вместо переменной %s",
                                replacement, left, binding.getName())
                );
            }
        } else {
            return CheckAxiomResult.FAILED_FORALL;
        }
    }

    // checks X[x:=Y]->?x(X) where Y is free for substitution
    private CheckAxiomResult checkAxiomExists(AbstractExpression expr) {
        if (expr.op != Operator.IMPL) {
            return CheckAxiomResult.FAILED_EXISTS;
        }
        BinExpression binExpr = (BinExpression) expr;
        if (binExpr.getRight().op != Operator.EXISTS) {
            return CheckAxiomResult.FAILED_EXISTS;
        }
        Quantifier quantRight = ((Quantifier) binExpr.getRight());
        AbstractExpression left = binExpr.getLeft();
        AbstractExpression right = quantRight.getArg();
        Variable binding = quantRight.getBinding();
        boolean freeForSubst = freeForSubst(right, left, binding);
        Pair<Boolean, Map<Integer, AbstractExpression>> comparisonResult =
                recCompWithMap(right, left, Collections.singleton(quantRight.getBinding().getName()), true);
        boolean matches = comparisonResult.first;
        if (matches) {
            if (freeForSubst) {
                return CheckAxiomResult.PASSED_EXISTS;
            } else {
                AbstractExpression replacement = comparisonResult.second.get(binding.getId());
                return new CheckAxiomResult(false, AxiomType.EXISTS,
                        String.format("терм %s не свободен для подстановки в формулу %s вместо переменной %s",
                                replacement, right, binding.getName())
                );
            }
        } else {
            return CheckAxiomResult.FAILED_EXISTS;
        }
    }

    // X[x=0]&@x(X->X[x=x'])->X
    private CheckAxiomResult checkAxiomFormal(AbstractExpression expr) {
        if (expr.op != Operator.IMPL) {
            return CheckAxiomResult.FAILED_FORMAL;
        }
        BinExpression binExpr = (BinExpression) expr;
        AbstractExpression template = binExpr.getRight();
        AbstractExpression mainPart = binExpr.getLeft();
        if (mainPart.op != Operator.AND) {
            return CheckAxiomResult.FAILED_FORMAL;
        }
        BinExpression binMainPart = (BinExpression) mainPart;
        AbstractExpression leftMain = binMainPart.getLeft();
        if (binMainPart.getRight().op != Operator.FORALL) {
            return CheckAxiomResult.FAILED_FORMAL;
        }
        Quantifier rightMain = (Quantifier) binMainPart.getRight();
        Variable inductionVariable = rightMain.getBinding();
        boolean mainLeftMatchesTemplate = recComp(template, leftMain,
                (var, e) -> {
                    if (Objects.equals(var.getName(), inductionVariable.getName())) {
                        return e instanceof Const && ((Const) e).getValue() == Const.ConstValue.ZERO;
                    }
                    return Objects.equals(var, e);
                });
        if (!mainLeftMatchesTemplate) {
            return CheckAxiomResult.FAILED_FORMAL;
        }
        AbstractExpression induction = rightMain.getArg();
        if (induction.op != Operator.IMPL) {
            return CheckAxiomResult.FAILED_FORMAL;
        }
        BinExpression binInduction = (BinExpression) induction;
        boolean inductionLeftMatches = recComp(template, binInduction.getLeft(),
                (var, e) -> {
                    if (Objects.equals(var.getName(), inductionVariable.getName())) {
                        return e instanceof Variable && Objects.equals(var.getName(), ((Variable) e).getName());
                    }
                    return Objects.equals(var, e);
                });
        boolean inductionRightMatches = recComp(template, binInduction.getRight(),
                (var, e) -> {
                    if (Objects.equals(var.getName(), inductionVariable.getName())) {
                        if (!(e instanceof Inc)) {
                            return false;
                        }
                        Inc inc = (Inc) e;
                        return inc.getArg() instanceof Variable
                                && Objects.equals(var.getName(), ((Variable) inc.getArg()).getName());
                    }
                    return Objects.equals(var, e);
                });
        boolean matches = inductionLeftMatches && inductionRightMatches;
        if (matches) {
            return CheckAxiomResult.PASSED_FORMAL;
        } else {
            return CheckAxiomResult.FAILED_FORMAL;
        }
    }

    private AbstractExpression copyExprUnbindingVars(AbstractExpression expr) {
        Map<String, Integer> newlyBound = new HashMap<>();
        return doCopyExprUnbindingVars(expr, newlyBound);
    }

    private AbstractExpression doCopyExprUnbindingVars(AbstractExpression expr, Map<String, Integer> newlyBound) {
        if (expr instanceof Variable) {
            Variable var = (Variable) expr;
            newlyBound.putIfAbsent(var.getName(), 0);
            return new Variable(var.getName(), var.getId(), newlyBound.get(var.getName()) != 0);
        }
        if (expr instanceof Quantifier) {
            Quantifier quant = (Quantifier) expr;
            Variable oldBinding = quant.getBinding();
            newlyBound.putIfAbsent(oldBinding.getName(), 0);
            newlyBound.compute(oldBinding.getName(), (ignored, value) -> value + 1);
            Variable newBinding = new Variable(oldBinding.getName(), oldBinding.getId(), oldBinding.isBound());
            AbstractExpression newArg = doCopyExprUnbindingVars(quant.getArg(), newlyBound);
            AbstractExpression result = new Quantifier(quant.op, newBinding, newArg);
            newlyBound.compute(oldBinding.getName(), (ignored, value) -> value - 1);
            return result;
        }
        if (expr instanceof Const) {
            return new Const(((Const) expr).getValue());
        }
        if (expr instanceof BinExpression) {
            BinExpression binExpr = (BinExpression) expr;
            return new BinExpression(binExpr.op,
                    doCopyExprUnbindingVars(binExpr.getLeft(), newlyBound),
                    doCopyExprUnbindingVars(binExpr.getRight(), newlyBound));
        }
        if (expr instanceof BinPredicate) {
            BinPredicate binPred = (BinPredicate) expr;
            return new BinPredicate(binPred.op,
                    doCopyExprUnbindingVars(binPred.getLeft(), newlyBound),
                    doCopyExprUnbindingVars(binPred.getRight(), newlyBound));
        }
        if (expr instanceof Function) {
            Function func = (Function) expr;
            return new Function(func.getName(),
                    func.getArgs().stream()
                            .map(e -> doCopyExprUnbindingVars(e, newlyBound))
                            .collect(Collectors.toList()));
        }
        if (expr instanceof Inc) {
            Inc inc = (Inc) expr;
            return new Inc(doCopyExprUnbindingVars(inc.getArg(), newlyBound));
        }
        if (expr instanceof Predicate) {
            Predicate pred = (Predicate) expr;
            return new Function(pred.getName(),
                    pred.getArgs().stream()
                            .map(e -> doCopyExprUnbindingVars(e, newlyBound))
                            .collect(Collectors.toList()));
        }
        if (expr instanceof UnExpression) {
            UnExpression unExpr = (UnExpression) expr;
            return new UnExpression(unExpr.op, doCopyExprUnbindingVars(unExpr.getArg(), newlyBound));
        }
        return null;
    }

    /**
     * Given two expressions assumes that arg is obtained from target
     * by replacing the replaced variable with expression X
     * (that must be checked somewhere else) and checks that X is free
     * for substitution in target instead of replaced.
     *
     * @param target
     * @param arg
     * @param replaced
     * @return
     */
    private boolean freeForSubst(AbstractExpression target, AbstractExpression arg, Variable replaced) {
        return recComp(copyExprUnbindingVars(target), copyExprUnbindingVars(arg),
                (var, expr) -> {
                    // todo: is it okay to check !isBound() here?
                    if (Objects.equals(var.getName(), replaced.getName()) && !var.isBound()) {
                        return Collections.disjoint(
                                var.getBoundInScopeVariables().stream()
                                        .map(Variable::getName)
                                        .collect(Collectors.toList()),
                                copyExprUnbindingVars(expr).getFreeVariablesNames()
                        );
                    }
                    return true;
                }
        );
    }

    private CheckRuleResult checkRule(AbstractExpression expr) {
        List<ResultWithErrorMessage> results =
                Arrays.asList(checkRuleForall(expr), checkRuleExists(expr), checkRuleMp(expr));
        ResultWithErrorMessage result = findPassedOrFailed(results);
        if (result == null) {
            return CheckRuleResult.FAILED_MP;
        }
        return (CheckRuleResult) result;
    }

    // X -> Y then X -> @x(Y)
    private CheckRuleResult checkRuleForall(AbstractExpression expr) {
        if (expr.op != Operator.IMPL) {
            return CheckRuleResult.FAILED_FORALL;
        }
        BinExpression binExpr = (BinExpression) expr;
        if (binExpr.getRight().op != Operator.FORALL) {
            return CheckRuleResult.FAILED_FORALL;
        }
        Quantifier quant = (Quantifier) binExpr.getRight();
        Variable binding = quant.getBinding();
        if (freeVariablesInAssumptions.containsKey(binding.getName())) {
            return new CheckRuleResult(false, RuleType.FORALL,
                    String.format("используется правило с квантором по переменной %s, входящей свободно в допущение %s",
                            binding.getName(), freeVariablesInAssumptions.get(binding.getName())));
        }
        AbstractExpression left = binExpr.getLeft();
        boolean boundsFreeVariables = left.getFreeVariablesNames().contains(binding.getName());
        BinExpression source = new BinExpression(Operator.IMPL, left, quant.getArg());
        boolean sourceIsProved = proved.containsKey(source);
        if (sourceIsProved) {
            if (boundsFreeVariables) {
                return new CheckRuleResult(false, RuleType.FORALL,
                        String.format("переменная %s входит свободно в формулу %s", binding.getName(), left));
            } else {
                return new CheckRuleResult(true, RuleType.FORALL, null, proved.get(source));
            }
        } else {
            return CheckRuleResult.FAILED_FORALL;
        }
    }

    // X -> Y then ?x(X) -> Y
    private CheckRuleResult checkRuleExists(AbstractExpression expr) {
        if (expr.op != Operator.IMPL) {
            return CheckRuleResult.FAILED_EXISTS;
        }
        BinExpression binExpr = (BinExpression) expr;
        if (binExpr.getLeft().op != Operator.EXISTS) {
            return CheckRuleResult.FAILED_EXISTS;
        }
        Quantifier quant = (Quantifier) binExpr.getLeft();
        Variable binding = quant.getBinding();
        if (freeVariablesInAssumptions.containsKey(binding.getName())) {
            return new CheckRuleResult(false, RuleType.EXISTS,
                    String.format("используется правило с квантором по переменной %s, входящей свободно в допущение %s",
                            binding.getName(), freeVariablesInAssumptions.get(binding.getName())));
        }
        AbstractExpression right = binExpr.getRight();
        boolean boundsFreeVariables = right.getFreeVariablesNames().contains(binding.getName());
        BinExpression source = new BinExpression(Operator.IMPL, quant.getArg(), right);
        boolean sourceIsProved = proved.containsKey(source);
        if (sourceIsProved) {
            if (boundsFreeVariables) {
                return new CheckRuleResult(true, RuleType.EXISTS,
                        String.format("переменная %s входит свободно в формулу %s", binding.getName(), right));
            } else {
                return new CheckRuleResult(true, RuleType.EXISTS, null, proved.get(source));
            }
        } else {
            return CheckRuleResult.FAILED_EXISTS;
        }
    }

    private CheckRuleResult checkRuleMp(AbstractExpression expr) {
        if (!mp.containsKey(expr)) {
            return CheckRuleResult.FAILED_MP;
        }
        Optional<Pair<AbstractExpression, Integer>> optionalSource = mp.get(expr).stream()
                .filter(src -> proved.containsKey(src.first))
                .findAny();
        if (!optionalSource.isPresent()) {
            return CheckRuleResult.FAILED_MP;
        }
        Pair<AbstractExpression, Integer> source = optionalSource.get();
        AbstractExpression left = source.first;
        int implIndex = source.second;
        return new CheckRuleResult(true, RuleType.MP, null, proved.get(left), implIndex);
    }

    private boolean recComp(AbstractExpression template, AbstractExpression arg) {
        return recComp(template, arg, Collections.emptySet(), false);
    }

    private boolean recComp(AbstractExpression template, AbstractExpression arg, Set<String> special) {
        return recComp(template, arg, special, false);
    }

    private boolean recComp(AbstractExpression template, AbstractExpression arg, boolean strict) {
        return recComp(template, arg, Collections.emptySet(), strict);
    }

    private boolean recComp(AbstractExpression template, AbstractExpression arg, Set<String> special, boolean mostlyStrict) {
        return recCompWithMap(template, arg, special, mostlyStrict).first;
    }

    private Pair<Boolean, Map<Integer, AbstractExpression>> recCompWithMap(AbstractExpression template, AbstractExpression arg, Set<String> special, boolean mostlyStrict) {
        final Map<Integer, AbstractExpression> map = new HashMap<>();
        boolean matches = recComp(template, arg,
                (var, expr) -> {
                    if (mostlyStrict && !special.contains(var.getName())
                            || !mostlyStrict && special.contains(var.getName())) {
                        return (expr instanceof Variable) && Objects.equals(var.getName(), ((Variable) expr).getName());
                    }
                    if (map.containsKey(var.getId())) {
                        return Objects.equals(map.get(var.getId()), expr);
                    } else {
                        map.put(var.getId(), expr);
                        return true;
                    }
                }
        );
        return new Pair<>(matches, map);
    }

    /**
     * Recursively check if two expressions are equal in all cases except
     * when template is a {@link Variable}. In that case a given predicate is used.
     *
     * @param template  An expression to match
     * @param arg       An expression being matched
     * @param predicate Predicate to test when template is a Variable
     * @return true if expressions are equals and predicate returned true for all Variables in template
     */
    private boolean recComp(AbstractExpression template, AbstractExpression arg, BiPredicate<Variable, AbstractExpression> predicate) {
        if (template instanceof Variable) {
            return predicate.test((Variable) template, arg);
        }
        boolean ok = template.getClass() == arg.getClass()
                && template.op == arg.op
                && template.getArgs().size() == arg.getArgs().size();
        if (template instanceof Named) {
            ok &= (arg instanceof Named)
                    && Objects.equals(((Named) arg).getName(), ((Named) template).getName());
        }
        return ok && IntStream.range(0, template.getArgs().size())
                .allMatch(i -> recComp(template.getArgs().get(i), arg.getArgs().get(i), predicate));
    }
}
