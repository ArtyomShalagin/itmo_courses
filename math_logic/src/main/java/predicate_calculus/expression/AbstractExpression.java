package predicate_calculus.expression;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractExpression {
    private AbstractExpression parent;

    public final Operator op;
    protected int hash;
    private final List<AbstractExpression> args;

    public AbstractExpression(Operator op, List<AbstractExpression> args) {
        this.op = op;
        this.args = args;
        args.forEach(expr -> expr.setParent(this));
    }

    public List<AbstractExpression> getArgs() {
        return args;
    }

    public Set<Variable> getFreeVariables() {
        return args.stream()
                .map(AbstractExpression::getFreeVariables)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public Set<String> getFreeVariablesNames() {
        return getFreeVariables().stream()
                .map(Variable::getName)
                .collect(Collectors.toSet());
    }

    public Set<Variable> getBoundInScopeVariables() {
        return parent == null ? new HashSet<>() : parent.getBoundInScopeVariables();
    }

    private void setParent(AbstractExpression parent) {
        this.parent = parent;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public String toPlainString() {
        return toString();
    }

    @Override
    public String toString() {
        return op + "(" + args.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
    }
}
