package check_derivation.expression;

public class BinExpression extends Expression {
    public final Expression left, right;

    public BinExpression(Expression left, Expression right, Operator op) {
        super(op);
        this.left = left;
        this.right = right;
        hash = left.hashCode() * 2772 + right.hashCode() * 27643 + op.hashCode() * 3462;
    }

    public String toString() {
        return "(" + left.toString() + op + right.toString() + ")";
    }

    public String toPlainString() {
        String l = left.toPlainString(), r = right.toPlainString();
        if (right.op != Operator.NOT && r.length() > 2 && (op == Operator.IMPL || right.op.priority() <= op.priority())) {
            r = r.substring(1, r.length() - 1);
        }
        if (left.op != Operator.NOT && l.length() > 2 && op != Operator.IMPL && left.op.priority() <= op.priority()) {
            l = l.substring(1, l.length() - 1);
        }
        return "(" + l + op + r + ")";
    }

    public boolean equals(Object o) {
        return o instanceof BinExpression && equals((BinExpression) o);
    }

    private boolean equals(BinExpression e) {
        return e.hashCode() == hashCode() && op == e.op && left.equals(e.left) && right.equals(e.right);
    }
}