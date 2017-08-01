package predicate_calculus;

import predicate_calculus.expression.*;
import common.Util;

import java.util.*;

import static predicate_calculus.expression.Operator.*;

public class Parser {
    private final int LOWEST_PRIORITY = 9;
    private final char[] expr;
    private int p;
    private Map<String, Integer> boundVariables = new HashMap<>();
    private Map<String, Stack<Integer>> variables = new HashMap<>();
    private int currId = 0;

    public static AbstractExpression parse(String source) throws ParsingException {
        return new Parser(source).parse();
    }

    private Parser(String expr) {
        this.expr = Util.removeSpaces(expr).toCharArray();
    }

    private AbstractExpression parse() throws ParsingException {
        try {
            Stack<Integer> virtualBrackets = new Stack<>();
            return parseRec(0, LOWEST_PRIORITY, virtualBrackets);
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private AbstractExpression parseRec(int level, int prior, Stack<Integer> virtualBrackets)
            throws ParsingException {
        if (prior == 0) {
            return readFactor(level, prior, virtualBrackets);
        } else {
            AbstractExpression val = parseRec(level, prior - 1, virtualBrackets);
            while (!end() && onOperator() && peekOperator().getPriority() == prior) {
                Operator op = getOperator();
                if (op == INC) {
                    val = new Inc(val);
                    while (onOperator() && peekOperator() == INC) {
                        getOperator();
                        val = new Inc(val);
                    }
                } else {
                    AbstractExpression next;
                    if (op.getAssoc() == Assoc.RIGHT) {
                        virtualBrackets.add(level);
                        next = parseRec(level + 1, LOWEST_PRIORITY, virtualBrackets);
                    } else {
                        next = parseRec(level, prior - 1, virtualBrackets);
                    }
                    if (op == EQ) {
                        val = new BinPredicate(op, val, next);
                    } else {
                        val = new BinExpression(op, val, next);
                    }
                    if (!end() && !virtualBrackets.empty() && virtualBrackets.peek() == level) {
                        virtualBrackets.pop();
                        return val;
                    }
                }
            }
            return val;
        }
    }

    private AbstractExpression readFactor(int level, int prior, Stack<Integer> virtualBrackets)
            throws ParsingException {
        if (Character.isDigit(peekChar()) || Character.isLetter(peekChar())) {
            String name = readName();
            if (!end() && peekChar() == '(' || Character.isUpperCase(name.charAt(0))) { // predicate or function
                List<AbstractExpression> args = Collections.emptyList();
                if (!end() && peekChar() == '(') {
                    args = readBracket();
                }
                if (Character.isLowerCase(name.charAt(0))) {    // function
                    return new Function(name, args);
                } else {                                        // predicate
                    return new Predicate(name, args);
                }
            } else if ("0".equals(name)) {
                return new Const(Const.ConstValue.ZERO);
            } else {                                            // variable
                variables.putIfAbsent(name, new Stack<>());
                if (variables.get(name).isEmpty()) {
                    variables.get(name).push(currId++);
                }
                boundVariables.putIfAbsent(name, 0);
                return new Variable(name, variables.get(name).peek(), boundVariables.get(name) > 0);
            }
        } else if (peekChar() == '(') {
            getChar();
            AbstractExpression e = parseRec(level + 1, LOWEST_PRIORITY, virtualBrackets);
            if (getChar() != ')') {
                throw new ParsingException("no closing bracket", expr, p);
            }
            return e;
        } else if (peekOperator().getArity() == Arity.UN) {
            Operator op = getOperator();
            if (op == Operator.NOT) {
                // or getPriority() - 1?
                return new UnExpression(op, parseRec(level + 1, op.getPriority(), virtualBrackets));
            } else {
                String name = readName();
                boundVariables.putIfAbsent(name, 0);
                boundVariables.compute(name, (ignored, value) -> value + 1);
                variables.putIfAbsent(name, new Stack<>());
                variables.get(name).push(currId++);
                Variable binding = new Variable(name, variables.get(name).peek(), true); // ?? lowest
                AbstractExpression result = new Quantifier(op, binding, parseRec(level + 1, EQ.getPriority(), virtualBrackets));
                boundVariables.compute(name, (ignored, value) -> value - 1);
                variables.get(name).pop();
                return result;
            }
        }
        throw new ParsingException("trying to read factor, but not pointing to a variable, " +
                "opening bracket, or unary operation", expr, p);
    }

    /**
     * Read all terms in brackets separated by comma.
     *
     * @return List of Expressions in brackets
     */
    private List<AbstractExpression> readBracket() throws ParsingException {
        List<AbstractExpression> result = new ArrayList<>();
        getChar();
        Stack<Integer> virtualBrackets = new Stack<>();
        result.add(parseRec(0, LOWEST_PRIORITY, virtualBrackets));
        while (peekChar() == ',') {
            getChar();
            result.add(parseRec(0, LOWEST_PRIORITY, virtualBrackets));
        }
        getChar();
        return result;
    }

    private String readName() {
        StringBuilder builder = new StringBuilder();
        builder.append(getChar());
        while (!end() && Character.isDigit(peekChar())) {
            builder.append(getChar());
        }
        return builder.toString();
    }

    private char peekChar() {
        return expr[p];
    }

    private char getChar() {
        return expr[p++];
    }

    private Operator readOperator(boolean get) {
        char op = peekChar();
        int len = 1;
        Operator result = null;
        if (op == '|') {
            result = OR;
        } else if (op == '&') {
            result = AND;
        } else if (op == '!') {
            result = NOT;
        } else if (op == '+') {
            result = PLUS;
        } else if (op == '*') {
            result = MULT;
        } else if (op == '@') {
            result = FORALL;
        } else if (op == '?') {
            result = EXISTS;
        } else if (op == '-' && expr[p + 1] == '>') {
            result = IMPL;
            len = 2;
        } else if (op == '=') {
            result = EQ;
        } else if (op == '\'') {
            result = INC;
        }
        if (result != null && get) {
            p += len;
        }
        return result;
    }

    private Operator peekOperator() {
        return readOperator(false);
    }

    private Operator getOperator() {
        return readOperator(true);
    }

    private boolean onOperator() {
        return !end() && peekOperator() != null;
    }

    private boolean end() {
        return p >= expr.length;
    }

}