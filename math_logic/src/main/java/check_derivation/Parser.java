package check_derivation;

import check_derivation.expression.*;

import java.util.HashMap;
import java.util.Stack;

import static check_derivation.expression.Operator.*;

public class Parser {

    private final int LOWEST_PRIORITY = 3;
    private final char[] expr;
    private int p;
    private int factorNumber;
    HashMap<String, Integer> variables;
    Stack<Integer> virtualBrackets;

    public Parser(String expr) {
        this.expr = expr.toCharArray();
        p = 0;
        variables = new HashMap<>();
        factorNumber = 0;
        virtualBrackets = new Stack<>();
    }

    public Expression parse() {
        Expression result = null;
        try {
            result = parseRec(0, LOWEST_PRIORITY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private Expression parseRec(int level, int prior) {
        skipSpaces();
        if (prior == 0) {
            return readFactor(level, prior);
        } else {
            Expression val = parseRec(level, prior - 1);
            skipSpaces();
            while (!end() && onOperator() && peekOperator().priority() == prior) {
                skipSpaces();
                Operator op = getOperator();
                skipSpaces();
                Expression next;
                if (op.getAssoc() == Assoc.RIGHT) {
                    virtualBrackets.add(level);
                    next = parseRec(level + 1, LOWEST_PRIORITY);
                } else {
                    next = parseRec(level, prior - 1);
                }
                val = new BinExpression(val, next, op);
                if (!end() && !virtualBrackets.empty() && virtualBrackets.peek() == level) {
                    virtualBrackets.pop();
                    return val;
                }
            }
            return val;
        }
    }

    private Expression readFactor(int level, int prior) {
        if (Character.isDigit(peekChar()) || Character.isLetter(peekChar())) {
            return readVariable();
        } else if (peekChar() == '(') {
            getChar();
            Expression e = parseRec(level + 1, LOWEST_PRIORITY);
            if (getChar() != ')') {
                System.err.println("Error: no )");
            }
            return e;
        } else if (peekOperator() == NOT) {
            getChar();
            return new UnExpression(NOT, parseRec(level + 1, prior));
        }
        System.err.println("Error: can't parse");
        return null;
    }
    
    private Expression readVariable() {
        StringBuilder builder = new StringBuilder();
        while (!end() && (Character.isDigit(peekChar()) || Character.isLetter(peekChar()))) {
            builder.append(getChar());
        }
        String var = builder.toString();
        int number;
        if (variables.containsKey(var)) {
            number = variables.get(var);
        } else {
            number = factorNumber++;
            variables.put(var, number);
        }
        return new Variable(number, var);
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
        } else if (op == '-' && expr[p + 1] == '>') {
            result = IMPL;
            len = 2;
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
        return peekOperator() != null;
    } 

    private boolean end() {
        return p >= expr.length;
    }

    void skipSpaces() {
        while (!end() && (peekChar() == ' ' || peekChar() == '\t'))
            getChar();
    }

    private boolean isOperation(char c) {
        return c == '|' || c == '&' || c == '>' || c == '!';
    }
}