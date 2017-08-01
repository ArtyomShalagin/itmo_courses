package predicate_calculus.expression;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Variable extends AbstractExpression implements Named {
    private final String name;
    private final int id;
    private final boolean bound;

    public Variable(String name, int id, boolean bound) {
        super(null, Collections.emptyList());
        this.name = name;
        this.id = id;
        this.bound = bound;
        hash = Objects.hash(name);
    }

    @Override
    public Set<Variable> getFreeVariables() {
        return bound ? Collections.emptySet() : Collections.singleton(this);
    }

    @Override
    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public boolean isBound() {
        return bound;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Variable && equals((Variable) o);
    }

    private boolean equals(Variable other) {
        // todo: should I compare ids?
//        return Objects.equals(name, other.name) && bound == other.bound;
        return Objects.equals(name, other.name);
    }
}
