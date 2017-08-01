package predicate_calculus.expression;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static predicate_calculus.expression.Operator.INC;

public class Inc extends AbstractExpression {

    public Inc(AbstractExpression arg) {
        super(INC, Collections.singletonList(arg));
    }

    public AbstractExpression getArg() {
        return getArgs().get(0);
    }

    @Override
    public String toPlainString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean withBracket = getArg().op != INC && getArg().op != null && getArg().op.getArity() != Operator.Arity.BIN;
        if (withBracket) {
            sb.append('(');
        }
        sb.append(getArg());
        if (withBracket) {
            sb.append(')');
        }
        sb.append('\'');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Inc && equals((Inc) o);
    }

    private boolean equals(Inc o) {
        return Objects.equals(getArg(), o.getArg());
    }
}
