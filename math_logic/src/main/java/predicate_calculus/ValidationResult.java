package predicate_calculus;

import predicate_calculus.expression.AbstractExpression;
import common.Pair;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private String errorMsg;
    private final List<Pair<AbstractExpression, CheckResult>> result = new ArrayList<>();

    public void addProved(AbstractExpression expr, CheckResult checkResult) {
        result.add(new Pair<>(expr, checkResult));
    }

    public List<Pair<AbstractExpression, CheckResult>> getResultList() {
        return result;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
