package predicate_calculus.expression;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Predicate extends AbstractExpression implements Named {
    public final String name;

    public Predicate(String name, List<AbstractExpression> expressions) {
        super(Operator.PREDICATE, expressions);
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
        builder.append(name);
        if (!getArgs().isEmpty()) {
            builder.append("(");
            getArgs().forEach(e -> builder.append(e).append(","));
            builder.setLength(builder.length() - 1);
            builder.append(")");
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Predicate && equals((Predicate) o);
    }

    private boolean equals(Predicate other) {
        return hash == other.hash && Objects.equals(getArgs(), other.getArgs());
    }
}
