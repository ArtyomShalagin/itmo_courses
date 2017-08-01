package predicate_calculus.expression;

import java.util.Arrays;
import java.util.Objects;

public class BinExpression extends AbstractExpression {

    public BinExpression(Operator op, AbstractExpression left, AbstractExpression right) {
        super(op, Arrays.asList(left, right));
        if (!Arrays.asList(
                new Operator[]{Operator.AND, Operator.OR, Operator.PLUS, Operator.MULT, Operator.IMPL, Operator.EQ})
                .contains(op)) {
            throw new IllegalArgumentException("Operator " + op + " is not binary but isPassed to BinExpression ctor");
        }
        hash = Objects.hash(left, op, right);
    }

    public AbstractExpression getLeft() {
        return getArgs().get(0);
    }

    public AbstractExpression getRight() {
        return getArgs().get(1);
    }

    @Override
    public String toString() {
        return "(" + getLeft() + op + getRight() + ")";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BinExpression && equals((BinExpression) o);
    }

    private boolean equals(BinExpression other) {
        return hash == other.hash && op == other.op
                && Objects.equals(getLeft(), other.getLeft()) && Objects.equals(getRight(), other.getRight());
    }

}