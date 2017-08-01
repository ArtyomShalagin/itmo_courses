package check_derivation.expression;

public class UnExpression extends Expression {
    public final Expression arg;

    public UnExpression(Operator op, Expression arg) {
        super(op);
        this.arg = arg;
        hash = op.hashCode() * 67432 + arg.hashCode() * 76424;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UnExpression && equals((UnExpression) o);
    }

    private boolean equals(UnExpression e) {
        return e.hashCode() == hashCode() && op == e.op && arg.equals(e.arg);
    }

    @Override
    public String toString() {
        return op + "" + arg;
    }

    public String toPlainString() {
        return toString();
    }
}
