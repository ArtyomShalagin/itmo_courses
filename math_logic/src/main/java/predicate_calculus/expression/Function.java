package predicate_calculus.expression;

import java.util.List;
import java.util.Objects;

public class Function extends AbstractExpression implements Named {
    public final String name;

    public Function(String name, List<AbstractExpression> expressions) {
        super(Operator.FUNCTION, expressions);
        this.name = name;
        hash = Objects.hash(name, op, expressions);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append("(");
        getArgs().forEach(e -> builder.append(e).append(","));
        builder.setLength(builder.length() - 1);
        builder.append(")");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Function && equals((Function) o);
    }

    private boolean equals(Function other) {
        return hash == other.hash && Objects.equals(getArgs(), other.getArgs());
    }
}
