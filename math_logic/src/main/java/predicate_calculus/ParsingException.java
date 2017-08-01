package predicate_calculus;

import java.util.Collections;

public class ParsingException extends Exception {
    private final int index;
    private final String expr;

    public ParsingException(String msg, char[] expr, int index) {
        super(msg);
        this.index = index;
        this.expr = new String(expr);
    }

    @Override
    public String getMessage() {
        return "Parser error at index " + index + ", " + super.getMessage() + '\n' + expr + '\n' +
                String.join("", Collections.nCopies(index, " ")) + '^';
    }
}
