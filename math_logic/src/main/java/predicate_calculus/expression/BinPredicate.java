package predicate_calculus.expression;

import java.util.Arrays;
import java.util.Objects;

public class BinPredicate extends AbstractExpression {

    public BinPredicate(Operator op, AbstractExpression left, AbstractExpression right) {
        super(op, Arrays.asList(left, right));
    }

    public AbstractExpression getLeft() {
        return getArgs().get(0);
    }

    public AbstractExpression getRight() {
        return getArgs().get(1);
    }

    @Override
    public String toString() {
        return "" + getLeft() + "=" + getRight() + "";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BinPredicate && equals((BinPredicate)o);
    }

    private boolean equals(BinPredicate o) {
        return Objects.equals(getLeft(), o.getLeft()) && Objects.equals(getRight(), o.getRight());
    }
}
