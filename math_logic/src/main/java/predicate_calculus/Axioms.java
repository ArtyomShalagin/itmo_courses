package predicate_calculus;

import predicate_calculus.expression.AbstractExpression;
import common.Util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Axioms {
    public static final List<AbstractExpression> axiomSchemas;
    public static final List<AbstractExpression> axioms;

    static {
        String[] axiomSchemasStrings = {
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
        String[] axiomsStrings = {
                "a=b->a'=b'",
                "a=b->a=c->b=c",
                "a'=b'->a=b",
                "!a'=0",
                "a+b'=(a+b)'",
                "a+0=a",
                "a*0=0",
                "a*b'=a*b+a"
        };
        axiomSchemas = Arrays.stream(axiomSchemasStrings)
                .map(Util.wrapNoThrow(Parser::parse))
                .collect(Collectors.toList());
        axioms = Arrays.stream(axiomsStrings)
                .map(Util.wrapNoThrow(Parser::parse))
                .collect(Collectors.toList());
    }
}
