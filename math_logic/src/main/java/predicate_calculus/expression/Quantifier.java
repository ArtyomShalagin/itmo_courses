package predicate_calculus.expression;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class Quantifier extends AbstractExpression {
    private final Variable binding;

    public Quantifier(Operator op, Variable binding, AbstractExpression expr) {
        super(op, Collections.singletonList(expr));
        if (op != Operator.FORALL && op != Operator.EXISTS) {
            throw new IllegalArgumentException("Operator " + op + " cannot be isPassed to Quantifier ctor");
        }
        this.binding = binding;
        hash = Objects.hash(op, binding, expr);
    }

    public AbstractExpression getArg() {
        return getArgs().get(0);
    }

    public Variable getBinding() {
        return binding;
    }

    @Override
    public Set<Variable> getBoundInScopeVariables() {
        Set<Variable> bound = super.getBoundInScopeVariables();
        bound.add(binding);
        return bound;
    }

    @Override
    public String toString() {
        return "" + op + binding + "(" + getArg() + ")";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Quantifier && equals((Quantifier) o);
    }

    private boolean equals(Quantifier other) {
        return hash == other.hash && op == other.op
                && Objects.equals(binding, other.binding) && Objects.equals(getArg(), other.getArg());
    }
}
