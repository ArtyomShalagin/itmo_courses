package check_derivation;

import check_derivation.expression.Expression;

import java.util.ArrayList;
import java.util.Arrays;

public class Axioms {
    static final ArrayList<Expression> axioms;

    static {
        String[] axiomsString = {
                "a->b->a",
                "(a->b)->(a->b->c)->(a->c)",
                "a->b->a&b",
                "a&b->a",
                "a&b->b",
                "a->a|b",
                "b->a|b",
                "(a->c)->(b->c)->(a|b->c)",
                "(a->b)->(a->!b)->!a",
                "!!a->a"
        };
        axioms = new ArrayList<>(axiomsString.length);
        Arrays.stream(axiomsString).forEach(s -> axioms.add(new Parser(s).parse()));
    }
}
