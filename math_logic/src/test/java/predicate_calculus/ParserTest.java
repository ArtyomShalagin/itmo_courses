package predicate_calculus;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import predicate_calculus.expression.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ParserTest {

    private AbstractExpression parseOfFail(String data) {
        try {
            return Parser.parse(data);
        } catch (ParsingException e) {
            System.err.println(e.getMessage());
            fail();
        } catch (Exception e) {
            System.err.println("unable to parse: " + data);
            System.err.println(e.getMessage());
            fail();
        }
        return new Variable("meh", 0, false);
    }

    private void test(String data) {
        AbstractExpression e1 = parseOfFail(data);
        AbstractExpression e2 = parseOfFail(e1.toString());
        assertEquals(e1, e2);
    }

    private void testWithAnswer(String data, AbstractExpression answer) {
        AbstractExpression e = parseOfFail(data);
        assertEquals(e, answer);
    }

    @Test
    public void test1_simple() {
        test("((Q(a)->?aQ(a))->(P(a)->(Q(a)->?aQ(a))))");
        test("((P(a)->(a))->((P(a)->(Q(a)->?aQ(a)))->(P(a)->?aQ(a))))");
        test("((P(a)->(Q(a)->?aQ(a          )))->(P(a)->?aQ(a)))");
        test("((A->A->A)-       >P(b))->P(a)->((A->        A->A)->P(b))");
        test("?aP(a)->(A->A->A)-    >   P(b)");
    }

    @Test
    public void test2_simple() {
        test("(A->A->A)->@xP(a)");
        test("@xP(x)->@xP(x)");
        test("@x(@xP(x)&Q(x))->@x(@xP(x)&Q(x))");
        test("@x(@yP(y)&Q(x))->@yP(y)&Q(x)");
        test("@x((@yP(y))&Q(x))->(@yP(y))&Q(x)");
    }

    @Test
    public void test3_equals() {
        test("t=t->((t=r->(t=t->r=t))->(t=r->r=t))");
        test("(0=0->0=0->0=0)->@c@b@a(a=b->a=c->b=c)");
        test("(r=t->(r=t->r=t))->(t=s->(r=t->(r=t->r=t)))");
    }

    @Test
    public void test4_inc() {
        test("@a(a=a)->(0+a)'=(0+a)'");
        test("(x'+0=(x+0)')&@y(((x)'+y=(x+y)')->((x)'+(y)'=(x+(y)')'))->((x)'+y=(x+y)')");
        test("x'+0=(x+0)')&@y(((x)'+0=(x+0)')->((x)'+(0)=(x+(0))'))->((x)'+0=(x+0)')");
    }

    @Test
    public void test5_axioms() {
        String[] axiomsStrings = {
                "a=b->a'=b'",
                "a'=b'->a=b",
                "!a'=0",
                "a+b'=(a+b)'",
                "a*b'=a*b+a"
        };
        AbstractExpression[] axioms = {
                new BinExpression(Operator.IMPL,
                        new BinPredicate(Operator.EQ,
                                new Variable("a", 0, false),
                                new Variable("b", 1, false)
                        ),
                        new BinPredicate(Operator.EQ,
                                new Inc(
                                        new Variable("a", 0, false)
                                ),
                                new Inc(
                                        new Variable("b", 1, false)
                                )
                        )
                ),
                new BinExpression(Operator.IMPL,
                        new BinPredicate(Operator.EQ,
                                new Inc(
                                        new Variable("a", 0, false)
                                ),
                                new Inc(
                                        new Variable("b", 1, false)
                                )
                        ),
                        new BinPredicate(Operator.EQ,
                                new Variable("a", 0, false),
                                new Variable("b", 1, false)
                        )
                ),
                new UnExpression(Operator.NOT,
                        new BinPredicate(Operator.EQ,
                                new Inc(
                                        new Variable("a", 0, false)
                                ),
                                new Const(Const.ConstValue.ZERO)
                        )
                ),
                new BinPredicate(Operator.EQ,
                        new BinExpression(Operator.PLUS,
                                new Variable("a", 0, false),
                                new Inc(
                                        new Variable("b", 1, false)
                                )
                        ),
                        new Inc(
                                new BinExpression(Operator.PLUS,
                                        new Variable("a", 0, false),
                                        new Variable("b", 1, false)
                                )
                        )
                ),
                new BinPredicate(Operator.EQ,
                        new BinExpression(Operator.MULT,
                                new Variable("a", 0, false),
                                new Inc(
                                        new Variable("b", 1, false)
                                )
                        ),
                        new BinExpression(Operator.PLUS,
                                new BinExpression(Operator.MULT,
                                        new Variable("a", 0, false),
                                        new Variable("b", 1, false)
                                ),
                                new Variable("a", 0, false)
                        )
                )
        };
        IntStream.range(0, axioms.length)
                .forEach(i -> testWithAnswer(axiomsStrings[i], axioms[i]));
    }

    @Test
    public void test6_predicate() {
        String[] expressionStrings = {
                "@x(x=y|?x(x=0))"
        };
        AbstractExpression[] expressions = {
                new Quantifier(Operator.FORALL, new Variable("x", 0, true),
                        new BinExpression(Operator.OR,
                                new BinPredicate(Operator.EQ,
                                        new Variable("x", 0, true),
                                        new Variable("y", 1, false)
                                ),
                                new Quantifier(Operator.EXISTS, new Variable("x", 2, true),
                                        new BinPredicate(Operator.EQ,
                                                new Variable("x", 2, true),
                                                new Const(Const.ConstValue.ZERO)
                                        )
                                )
                        )
                )
        };
        IntStream.range(0, expressions.length)
                .forEach(i -> testWithAnswer(expressionStrings[i], expressions[i]));
    }

    @Test
    public void test7_brute() {
        try {
            File dir = Paths.get("test_data/predicate_calculus").toFile();
            File[] files = dir.listFiles();
            if (files == null) {
                throw new IOException("Directory with tests is not a directory");
            }
            Arrays.stream(files)
                    .sorted(Comparator
                            .comparing((File f) -> f.getName().length())
                            .thenComparing(File::getName))
                    .forEach(file -> {
                        System.out.println("Parsing " + file.getName());
                        Scanner in;
                        try {
                            in = new Scanner(file);
                        } catch (FileNotFoundException e) {
                            System.err.println(e.getMessage());
                            return;
                        }
                        in.nextLine();
                        int cnt = 0;
                        while (in.hasNextLine()) {
                            cnt++;
                            test(in.nextLine());
//                            Thread t = new Thread(() -> test(in.nextLine()));
//                            t.start();
//                            try {
//                                t.join(0);
//                                if (t.isAlive()) {
//                                    fail("expression " + cnt + " took to long to parse");
//                                }
//                            } catch (InterruptedException e) {
//                                fail(e.getMessage());
//                            }
                        }
                    });
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
