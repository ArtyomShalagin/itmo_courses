package predicate_calculus.expression;

import java.util.Collections;
import java.util.Set;

public class Const extends AbstractExpression {
    private final ConstValue value;

    public Const(ConstValue value) {
        super(null, Collections.emptyList());
        this.value = value;
    }

    public ConstValue getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Const aConst = (Const) o;

        return value == aConst.value;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        switch (value) {
            case ZERO:
                return "0";
            default:
                return "unknown const";
        }
    }

    public enum ConstValue {
        ZERO
    }
}
