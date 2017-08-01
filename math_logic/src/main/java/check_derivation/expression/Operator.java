package check_derivation.expression;

public enum Operator {
    IMPL, OR, AND, NOT;

    public Assoc getAssoc() {
        switch (this) {
            case IMPL: return Assoc.RIGHT;
            default: return Assoc.LEFT;
        }
    }

    public String toString() {
        switch (this) {
            case IMPL: return "->";
            case OR: return "|";
            case AND: return "&";
            case NOT: return "!";
            default: return "error";
        }
    }

    public int priority() {
        switch (this) {
            case IMPL: return 3;
            case OR: return 2;
            case AND: return 1;
            case NOT: return 0;
            default: return -1;
        }
    }

    public enum Assoc {
        LEFT, RIGHT
    }
}
