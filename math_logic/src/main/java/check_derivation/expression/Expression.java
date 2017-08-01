package check_derivation.expression;

public abstract class Expression {
    public final Operator op;
    protected int hash;

    public Expression(Operator op) {
        this.op = op;
    }

    public abstract String toPlainString();

    public int hashCode() {
        return hash;
    }
}
