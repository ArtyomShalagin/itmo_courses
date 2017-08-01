package predicate_calculus.expression;

import java.util.Arrays;
import java.util.List;

public enum Operator {
    IMPL, OR, AND, NOT, PLUS, MULT, EQ, FORALL, EXISTS, FUNCTION, PREDICATE, INC;

    static List<Operator> increasePriorOrder = Arrays.asList(INC, MULT, PLUS, EQ, NOT, AND, OR, IMPL, EXISTS, FORALL);

    public Assoc getAssoc() {
        switch (this) {
            case IMPL: return Assoc.RIGHT;
            default: return Assoc.LEFT;
        }
    }

    public Arity getArity() {
        switch (this) {
            case NOT:
            case FORALL:
            case EXISTS:
            case INC:
                return Arity.UN;
            case IMPL:
            case OR:
            case AND:
            case PLUS:
            case MULT:
            case EQ:
                return Arity.BIN;
            case FUNCTION:
            case PREDICATE:
                return Arity.MULTI;
            default:
                return null;
        }
    }

    public String toString() {
        switch (this) {
            case FORALL: return "@";
            case EXISTS: return "?";
            case PLUS: return "+";
            case MULT: return "*";
            case IMPL: return "->";
            case OR: return "|";
            case AND: return "&";
            case NOT: return "!";
            case EQ: return "=";
            case INC: return "'";
            case FUNCTION: return "function";
            case PREDICATE: return "predicate";
            default: return "error";
        }
    }

    public int getPriority() {

        switch (this) {
            case FORALL:
            case EXISTS: return 9;
            case IMPL: return 8;
            case OR: return 7;
            case AND: return 6;
            case NOT: return 5;
            case EQ: return 4;
            case PLUS: return 3;
            case MULT: return 2;
            case INC: return 1;
            default: return -1;
        }
    }

    public enum Assoc {
        LEFT, RIGHT
    }

    public enum Arity {
        UN, BIN, MULTI
    }
}
