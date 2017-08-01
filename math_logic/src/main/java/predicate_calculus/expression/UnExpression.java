package predicate_calculus.expression;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class UnExpression extends AbstractExpression {

    public UnExpression(Operator op, AbstractExpression expr) {
        super(op, Collections.singletonList(expr));
        if (op != Operator.NOT) {
            throw new IllegalArgumentException("Operator " + op + " is not unary");
        }
        hash = Objects.hash(op, expr);
    }

    public AbstractExpression getArg() {
        return getArgs().get(0);
    }

    @Override
    public String toString() {
        return "" + op + getArg();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UnExpression && equals((UnExpression) o);
    }

    private boolean equals(UnExpression other) {
        return hash == other.hash && op == other.op && Objects.equals(getArg(), other.getArg());
    }
}
