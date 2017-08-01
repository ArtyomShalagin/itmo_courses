package check_derivation;

import check_derivation.expression.*;
import javafx.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class Tester {
    private Random rnd = new Random(134);

    public void test() {
        int cnt = 0;
        while (true) {
            Expression e = randomExpression(10000);
            boolean ok = e.equals(new Parser(e.toString()).parse());
            if (!ok) {
                System.out.println(e);
                System.out.println(e.toPlainString());
                break;
            }
            cnt++;
            if (cnt % 10 == 0)
                System.out.print("\r" + cnt);
        }
    }

    private void testValidate() {
        ArrayList<String> proof = randomProof(2, 10, 3);
        PrintWriter out;
        try {
            out = new PrintWriter(new File("input.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        proof.forEach(out::println);
        out.close();
        try {
            Main.main(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<String> randomProof(int exprLen, int size, int assCount) {
        ArrayList<Expression> assumptions = new ArrayList<>();
        int assumptionCnt = rnd.nextInt(assCount - 1) + 1;
        for (int i = 0; i < assumptionCnt; i++) {
            assumptions.add(randomExpression(exprLen));
        }
        ArrayList<Expression> proof = new ArrayList<>();
        ArrayList<Expression> proved = new ArrayList<>();
        proof.add(assumptions.get(0));
        proved.add(assumptions.get(0));
        ArrayList<Pair<Expression, Expression>> mp = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int t = rnd.nextInt(3);
            if (t == 0) { //to axiom
                Expression e = insertRandom(Axioms.axioms.get(rnd.nextInt(Axioms.axioms.size())), new HashMap<>());
                proof.add(e);
                if (e instanceof BinExpression) {
                    if (e.op == Operator.IMPL) {
                        mp.add(new Pair<>(((BinExpression) e).left, ((BinExpression) e).right));
                    }
                }
            } else if (t == 1) {
                if (!mp.isEmpty()) {
                }
            } else {
                if (proved.size() != 0) {
                    int ind = rnd.nextInt(proved.size());
                    proof.add(proved.get(ind));
                    proved.remove(ind);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(assumptions.get(0).toString());
        System.err.println(assumptions.get(0));
        for (int i = 1; i < assumptionCnt; i++) {
            sb.append(",");
            sb.append(assumptions.get(i).toString());
            System.err.println(assumptions.get(i));
        }
        sb.append("|-");
        sb.append(proof.get(proof.size() - 1));
        ArrayList<String> res = new ArrayList<>();
        res.add(sb.toString());
        for (Expression e : proof) {
            res.add(e.toString());
        }
        return res;
    }

    private Expression insertRandom(Expression ax, HashMap<String, Expression> map) {
        if (ax instanceof Variable) {
            Variable var = (Variable) ax;
            if (map.containsKey(var.name)) {
                return map.get(var.name);
            } else {
                Expression e = randomExpression(2);
                map.put(var.name, e);
                return e;
            }
        } else if (ax instanceof UnExpression) {
            return new UnExpression(Operator.NOT, insertRandom(((UnExpression) ax).arg, map));
        } else {
            BinExpression bin = (BinExpression) ax;
            return new BinExpression(insertRandom(bin.left, map), insertRandom(bin.right, map), bin.op);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
//        new Tester().test();
//        new Tester().testValidate();

        Scanner in1 = new Scanner(new File("output.txt"));
        Scanner in2 = new Scanner(new File("a.out"));
        while (in1.hasNextLine() || in2.hasNextLine()) {
            String s1 = in1.nextLine();
            String s2 = in2.nextLine();
            if (!s1.equals(s2) && (!s1.contains("M.P.") || !s2.contains("M.P."))) {
                System.err.println("ERROR");
                System.out.println(s1);
                System.out.println(s2);
                System.out.println();
            }
        }
    }

    private Expression randomExpression(int len) {
        if (len < 2) {
            int n = rnd.nextInt(3);
            return new Variable(n, "" + (char) ('a' + n));
        }
        int divider = rnd.nextInt(len);
        Operator op = randomOperator();
        if (op == Operator.NOT) {
            return new UnExpression(op, randomExpression(len - 1));
        } else {
            Expression res = new BinExpression(randomExpression(divider), randomExpression(len - divider), op);
            if (Math.random() > .5) {
                res = new Parser(res.toPlainString()).parse();
            }
            return res;
        }
    }

    private Operator randomOperator() {
        int type = rnd.nextInt(4);
        switch (type) {
            case 0:
                return Operator.NOT;
            case 1:
                return Operator.AND;
            case 2:
                return Operator.OR;
            case 3:
                return Operator.IMPL;
            default:
                return null;
        }
    }
}
