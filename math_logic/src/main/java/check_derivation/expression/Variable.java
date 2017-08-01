package check_derivation.expression;

public class Variable extends Expression {
    public final int number;
    public final String name;

    public Variable(int number, String name) {
        super(null);
        this.number = number;
        this.name = name;
        hash = 762367 * name.hashCode();
    }

    public boolean equals(Object o) {
        return o instanceof Variable && equals((Variable) o);
    }

    public boolean equals(Variable e) {
        return name.equals(e.name);
    }

    public String toString() {
//        return "" + number + "-" + name;
        return name;
    }

    public String toPlainString() {
        return toString();
    }
}
